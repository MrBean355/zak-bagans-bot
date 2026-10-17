# Specification: Reddit Bot Engine & Lifecycle

This specification defines the scheduled polling, API pagination, contribution ingestion, and filtering rules.

- **Primary Implementation**:
    - Engine: `src/main/kotlin/com/github/mrbean355/zakbot/ZakBagansBot.kt`
    - Reddit Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/RedditService.kt`
    - Contribution Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/ContributionService.kt`
- **Unit Tests**:
    - `src/test/kotlin/com/github/mrbean355/zakbot/ZakBagansBotTest.kt`
    - `src/test/kotlin/com/github/mrbean355/zakbot/service/RedditServiceTest.kt`
    - `src/test/kotlin/com/github/mrbean355/zakbot/service/ContributionServiceTest.kt`

---

## 1. Engine Invariants for AI Agents

1. **Initial Polling Delay**: `checkContributions()` must start with an initial delay (`initialDelay = 10_000L`) to
   allow Spring context, database connections, and caches to initialize.
2. **Fixed Polling Rate**: Polling occurs strictly every 15 minutes (`fixedRate = 15 * 60 * 1000L`).
3. **Cursor Ordering**: Submissions and comments returned by Reddit API must be sorted descending by creation time; the
   timestamp of the first (newest) item is persisted in `BotCache` as the cursor for the next poll.
4. **Safety Page Bounding**: Paging backwards via Reddit API `after` token must be capped at `10` pages
   (`PageHistoryLimit = 10`) with `5` items per page (`PageSizeLimit = 5`).
5. **No Self-Replies**: Comments authored by `BotUsername` must never be evaluated for quote matching.
6. **No Command Mixing**: Comments authored by `AuthorUsername` are routed exclusively to
   `commandService.processAuthorCommand()` and immediately returned.

---

## 2. Scheduling & Task Specifications

### 2.1 `checkContributions()`

```kotlin
@Scheduled(fixedRate = 15 * 60 * 1000L, initialDelay = 10_000L)
fun checkContributions()
```

- Calls `redditService.getSubmissionsSince(botCache.getLastSubmissionTime())`.
- If non-empty: sets `botCache.setLastSubmissionTime(first.created)`.
- For each submission: invokes `contributionService.processSubmission(it)`.
- Calls `redditService.getCommentsSince(botCache.getLastCommentTime())`.
- If non-empty: sets `botCache.setLastCommentTime(first.created)`.
- For each comment: invokes `contributionService.processComment(it)`.

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
3. If `botCache.isUserIgnored(submission.author)` -> `return`.
4. Search phrase: `val response = phraseService.findPhrase(submission)?.substitute(submission) ?: return`.
5. Send Telegram alert:
   `telegramNotifier.sendMessage(getString("telegram.new_submission", submission.title, response, submission.url))`.
6. Post Reddit reply: `redditService.replyToSubmission(submission, response)`.

### 3.2 `processComment(comment: Comment)`

1. If `comment.author == AuthorUsername` -> delegate to `commandService.processAuthorCommand(comment)` and `return`.
2. Call `checkForBotMention(comment)`.
3. Skip conditions (return immediately if any is true):
    - `comment.author == BotUsername`
    - `botCache.isUserIgnored(comment.author)`
    - `botCache.isSubmissionIgnored(comment.submissionFullName)`
    - `redditService.getCommentSubmission(comment)?.let { botCache.isUserIgnored(it.author) } == true`
4. Reply-to-Bot Check:
    - If `redditService.findParentComment(comment)?.author == BotUsername`:
        - If `comment.body.isBadBot()`:
            - `botCache.ignoreUser(comment.author, comment.fullName)`
            - Send `telegram.new_ignored_user`
            - Reply with `reddit.new_user_ignored`
        - Else if `comment.body.isGoodBot() && randomRoll() <= GOOD_BOT_REPLY_CHANCE`:
            - `val phrase = phraseService.getPhrase(PhraseType.GoodBot)?.substitute(comment)`
            - If non-null:
                - `val response = getString("reddit.good_bot_footer", phrase)`
                - Send `telegram.good_bot_praise`
                - Reply with `response`
        - `return`
5. Search phrase: `val response = phraseService.findPhrase(comment)?.substitute(comment) ?: return`.
6. Send Telegram alert:
   `telegramNotifier.sendMessage(getString("telegram.new_comment", comment.body, response, comment.url))`.
7. Post Reddit reply: `redditService.replyToComment(comment, response)`.

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
