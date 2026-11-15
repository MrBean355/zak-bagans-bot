# Specification: Reddit Bot Engine & Lifecycle

This specification defines the scheduled polling, API pagination, contribution ingestion, and filtering rules.

- **Primary Implementation**:
    - Engine: `src/main/kotlin/com/github/mrbean355/zakbot/ZakBagansBot.kt`
    - Reddit Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/RedditService.kt`
    - Contribution Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/ContributionService.kt`
    - Gemini Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/GeminiService.kt`
- **Unit Tests**:
    - `src/test/kotlin/com/github/mrbean355/zakbot/ZakBagansBotTest.kt`
    - `src/test/kotlin/com/github/mrbean355/zakbot/service/RedditServiceTest.kt`
    - `src/test/kotlin/com/github/mrbean355/zakbot/service/ContributionServiceTest.kt`
    - `src/test/kotlin/com/github/mrbean355/zakbot/service/GeminiServiceTest.kt`

---

## 1. Engine Invariants for AI Agents

1. **Initial Polling Delay**: `checkContributions()` must start with an initial delay (`initialDelay = 10_000L`) to
   allow Spring context, database connections, and caches to initialize.
2. **Fixed Polling Rate**: Polling occurs strictly every 15 minutes (`fixedRate = 15 * 60 * 1000L`).
3. **Cursor Ordering**: Submissions and comments returned by Reddit API must be sorted descending by creation time; the
   timestamp of the first (newest) item is persisted in `BotCache` as the cursor for the next poll only after all
   items in the batch have been processed.
4. **Safety Page Bounding**: Paging backwards via Reddit API `after` token must be capped at `10` pages
   (`PageHistoryLimit = 10`) with `5` items per page (`PageSizeLimit = 5`).
5. **No Self-Replies**: Comments authored by `BotUsername` must never be evaluated for quote matching.
6. **No Command Mixing**: Comments authored by `AuthorUsername` are routed exclusively to
   `commandService.processAuthorCommand()` and immediately returned.
7. **Parent Submission Caching**: `RedditService.getCommentSubmission()` MUST cache parent submissions in an in-memory
   LRU cache (capacity 100) to prevent redundant `GET /api/info` calls when multiple comments belong to the same post.
8. **Reddit API Resilience**: Write operations (`postCommentReply`, `setBotFlair`, `deleteComment`) and lookups (`findParentComment`,
   `getFlairOptions`, `getOwnRecentComments`) MUST catch exceptions and log errors/warnings to prevent transient API outages or network
   glitches from crashing scheduled jobs.
9. **Downvote Self-Deletion**: In each 15-minute polling cycle, `contributionService.checkDownvotedComments()` checks the bot's
   recent comments via `redditService.getOwnRecentComments()`. If a comment has not already been deleted and has a score
   at or below `zakbot.downvotes.delete-threshold` (default `-1`), it is deleted via Reddit `POST /api/del` (when replies are enabled),
   tracked in an in-memory LRU set to prevent duplicate deletions, and an alert is dispatched via Telegram.

---

## 2. Scheduling & Task Specifications

### 2.1 `checkContributions()`

```kotlin
@Scheduled(fixedRate = 15 * 60 * 1000L, initialDelay = 10_000L)
fun checkContributions()
```

- Calls `redditService.getSubmissionsSince(botCache.getLastSubmissionTime())`.
- For each submission: invokes `contributionService.processSubmission(it)`.
- If non-empty: sets `botCache.setLastSubmissionTime(first.created)`.
- Calls `redditService.getCommentsSince(botCache.getLastCommentTime())`.
- For each comment: invokes `contributionService.processComment(it)`.
- If non-empty: sets `botCache.setLastCommentTime(first.created)`.
- Calls `contributionService.checkDownvotedComments()`.

### 2.2 `updateBotFlair()`

```kotlin
@Scheduled(cron = "@midnight")
fun updateBotFlair()
```

- Fetches flairs via `redditService.getFlairOptions()`.
- If non-empty, selects `options.random()` and calls `redditService.setBotFlair(randomOption)`.

---

## 3. Contribution Processing Decision Trees

### 3.1 `processSubmission(submission: Submission)`

1. If `submission.author == AuthorUsername` -> `return` (ignore author's posts).
2. Call `checkForBotMention(submission)`.
3. If `botCache.isUserIgnored(submission.author)` or `botCache.isSubmissionIgnored(submission.fullName)` -> `return`.
4. Evaluate sensitivity via `geminiService.evaluateSubmission(submission)`. If `sensitivity >= zakbotProperties.moderation.sensitivityThreshold` (default `0.7f`):
    - `botCache.ignoreSubmission(submission.fullName, reason)`
    - Send `telegram.sensitive_submission_ignored` notification
    - `return`
5. Search phrase: `val response = phraseService.findPhrase(submission)?.substitute(submission) ?: return`.
6. Post Reddit reply: `redditService.replyToSubmission(submission, response)`.
7. Send Telegram alert:
   `telegramNotifier.sendMessage(getString("telegram.new_submission", submission.title, response, submission.url))`.

### 3.2 `processComment(comment: Comment)`

1. If `comment.author == AuthorUsername` -> delegate to `commandService.processAuthorCommand(comment)` and `return`.
2. Skip conditions (call `checkForBotMention(comment)` and return immediately if any is true):
    - `comment.author == BotUsername`
    - `botCache.isUserIgnored(comment.author)`
    - `botCache.isSubmissionIgnored(comment.submissionFullName)`
    - `redditService.getCommentSubmission(comment)?.let { botCache.isUserIgnored(it.author) } == true`
3. Reply-to-Bot Check:
    - If `redditService.findParentComment(comment)?.author == BotUsername`:
        - Determine `BotSentiment`:
            - `comment.body.isBadBot()` -> `BAD_BOT`
            - `comment.body.isGoodBot()` -> `GOOD_BOT`
            - Otherwise -> `geminiService.evaluateBotReply(comment)` (returns `NEUTRAL` if disabled or on error)
        - If `sentiment != GOOD_BOT`, call `checkForBotMention(comment)`.
        - If `sentiment == BAD_BOT`:
            - `botCache.ignoreUser(comment.author, comment.fullName)`
            - Reply with `reddit.new_user_ignored`
            - Send `telegram.new_ignored_user`
        - Else if `sentiment == GOOD_BOT && randomRoll() <= GOOD_BOT_REPLY_CHANCE`:
            - `val phrase = phraseService.getPhrase(PhraseType.GoodBot)?.substitute(comment)`
            - If non-null:
                - `val response = getString("reddit.good_bot_footer", phrase)`
                - Reply with `response`
                - Send `telegram.good_bot_praise`
        - `return`
4. Call `checkForBotMention(comment)`.
5. Search phrase: `val response = phraseService.findPhrase(comment)?.substitute(comment) ?: return`.
6. Post Reddit reply: `redditService.replyToComment(comment, response)`.
7. Send Telegram alert:
   `telegramNotifier.sendMessage(getString("telegram.new_comment", comment.body, response, comment.url))`.

---

## 4. Bot Mention Patterns & Rules

Evaluated inside `checkForBotMention()`:

- Text is normalized to lowercase and trimmed.
- Excluded if `isGoodBot()` evaluates to true.
- Triggers if:
    - Regex match: `\bbot\b`
    - Substrings: `"zakbot"`, `"zacbot"`, `"baganbot"`, `"bagansbot"`
- Emits `telegram.new_bot_mention_submission` or `telegram.new_bot_mention_comment`. Does not send a Reddit reply.

---

## 5. Verification Commands

```bash
# Run all bot engine and contribution tests
./gradlew test --tests "*ZakBagansBotTest*" --tests "*ContributionServiceTest*" --tests "*RedditServiceTest*"
```
