# Specification: Telegram Bot & Telemetry

This specification defines the Telegram notifier abstraction, long-polling telemetry bot, `/status` command, and
outbound event catalog.

- **Primary Implementation**:
    - Bot & Notifier: `src/main/kotlin/com/github/mrbean355/zakbot/telegram/TelegramBot.kt`, `TelegramAction.kt`,
      `TelegramNotifier.kt`
    - Configuration: `src/main/kotlin/com/github/mrbean355/zakbot/config/TelegramProperties.kt`
- **Unit Tests**:
    - `src/test/kotlin/com/github/mrbean355/zakbot/telegram/TelegramBotTest.kt`

---

## 1. Telemetry Invariants for AI Agents

1. **Formatting & Preview**: All Telegram messages MUST use `ParseMode.MARKDOWN` with link previews disabled
   (`LinkPreviewOptions.builder().isDisabled(true).build()`).
2. **Escaping**: Since Markdown mode is used, avoid unescaped Markdown syntax that could break Telegram message
   delivery.
3. **Chat ID Binding**: Outbound messages are always sent to `telegramProperties.chatId`.
4. **Resilient Delivery**: Failures in `sendMessage()` (e.g. Telegram API outages or Markdown parsing issues) MUST be
   caught and logged as errors, never propagating exceptions that could abort background processing.

---

## 2. Inbound Telemetry (`/status` & Action Buttons)

Implemented in `TelegramBot.consume()`:

- `/status` (from `telegramProperties.chatId`): sends `buildStatusMessage()` with a `Refresh` (`rs`) inline button.
- Callback queries (from `telegramProperties.chatId`):
    - `iu:<username>` / `uu:<username>`: ignores or unignores a Reddit user in `BotStateService`, answers with a toast, and
      toggles the button.
    - `ip:<fullName>` / `up:<fullName>`: ignores or unignores a Reddit post in `BotStateService`, answers with a toast, and
      toggles the button.
    - `dc:<fullName>`: deletes the bot's reply via `RedditService.deleteCommentById()`, answers with a toast, and marks
      the button `Deleted` (`cd`).
    - `rs`: refreshes the `/status` message in place via `EditMessageText` and answers with a toast.

### 2.1 Telemetry Composition

`buildStatusMessage()` aggregates runtime data using the template `telegram.bot_status_response`:

```text
telegram.bot_status_response=📊 *ZakBot Status*\n\n*System*\n• Version: `%s`\n• Uptime: %s\n• Memory: `%s`\n• Gemini: `%s`\n\n*Reddit (/r/%s)*\n• Quotes in DB: %d\n• Ignored users: %d\n• Ignored posts: %d
```

- **Version**: `buildProperties.version`
- **Uptime**: Calculated via `Duration.between(Instant.ofEpochMilli(applicationContext.startupDate), Instant.now())`.
  Formats only non-zero units (days, hours, minutes, seconds). Defaults to `"< 1 second"` if startup duration is zero.
- **Memory**: `${usedMb}MB / ${maxMb}MB` where:
    - `usedMb = (totalMemory - freeMemory) / (1024 * 1024)`
    - `maxMb = maxMemory / (1024 * 1024)`
- **Gemini**: `"Disabled"` if `!geminiProperties.enabled`, `"No API key"` if `geminiProperties.apiKey.isBlank()`,
  `"Dry run"` if `geminiProperties.dryRun`, otherwise `"Enabled"`.
- **Subreddit**: `SubredditName` constant.
- **Counters**:
    - Quotes in DB: `phraseRepository.count()`
    - Ignored Users: `ignoredUserRepository.count()`
    - Ignored Posts: `ignoredSubmissionRepository.count()`

---

## 3. Outbound Event Notification Catalog

| String Resource Key                     | Trigger Location                                | Parameters                                                     | Action Buttons                                                                                  |
|:----------------------------------------|:------------------------------------------------|:---------------------------------------------------------------|:------------------------------------------------------------------------------------------------|
| `telegram.bot_status_response`          | `TelegramBot` (`/status` command)               | version, uptime, memory, gemini status, subreddit, DB counts   | `RefreshStatus`                                                                                 |
| `telegram.new_submission`               | `ContributionService.processSubmission`         | `submission.title`, `response`                                 | `IgnoreUser`, `IgnoreSubmission`, `DeleteComment` (if reply ID returned), `OpenUrl`             |
| `telegram.new_comment`                  | `ContributionService.processComment`            | `comment.body`, `response`                                     | `IgnoreUser`, `IgnoreSubmission`, `DeleteComment` (if reply ID returned), `OpenUrl`             |
| `telegram.new_bot_mention_submission`   | `ContributionService.checkForBotMention`        | `submission.title`                                             | `IgnoreUser` (if `author != BotUsername`), `IgnoreSubmission`, `OpenUrl`                        |
| `telegram.new_bot_mention_comment`      | `ContributionService.checkForBotMention`        | `comment.body`                                                 | `IgnoreUser` (if `author != BotUsername`), `IgnoreSubmission` (`submissionFullName`), `OpenUrl` |
| `telegram.new_ignored_user`             | `ContributionService.processComment` (bad bot)  | `comment.author`                                               | `UnignoreUser`, `OpenUrl`                                                                       |
| `telegram.good_bot_praise`              | `ContributionService.processComment` (good bot) | `comment.author`, `response`                                   | `IgnoreUser`, `DeleteComment` (if reply ID returned), `OpenUrl`                                 |
| `telegram.deleted_downvoted_comment`    | `ContributionService.checkDownvotedComments`    | `comment.score`, `comment.body`                                | `OpenUrl`                                                                                       |
| `telegram.sensitive_submission_ignored` | `ContributionService.processSubmission`         | `submission.title`, `sensitivityPercent`, `reason` (or "None") | `IgnoreUser`, `UnignoreSubmission`, `OpenUrl`                                                   |
| `telegram.sensitive_submission_dry_run` | `ContributionService.processSubmission`         | `submission.title`, `sensitivityPercent`, `reason` (or "None") | `IgnoreUser`, `IgnoreSubmission`, `OpenUrl`                                                     |
| `telegram.submission_moderated`         | `ContributionService.processSubmission`         | `submission.title`, `sensitivityPercent`, `reason` (or "None") | `IgnoreUser`, `IgnoreSubmission`, `OpenUrl`                                                     |
| `telegram.sensitive_comment_skipped`    | `ContributionService.processComment`            | `comment.body`, `sensitivityPercent`, `reason` (or "None")     | `IgnoreUser`, `IgnoreSubmission` (`submissionFullName`), `OpenUrl`                              |
| `telegram.sensitive_comment_dry_run`    | `ContributionService.processComment`            | `comment.body`, `sensitivityPercent`, `reason` (or "None")     | `IgnoreUser`, `IgnoreSubmission` (`submissionFullName`), `OpenUrl`                              |
| `telegram.bot_reply_sentiment_dry_run`  | `ContributionService.evaluateBotReplySentiment` | `comment.author`, `comment.body`, `sentiment.name`             | `IgnoreUser`, `OpenUrl`                                                                         |
| `telegram.gemini_evaluation_failed`     | `GeminiService`                                 | `contextText`, `errorSummary`                                  | `OpenUrl`                                                                                       |
| `telegram.command_ignore_user`          | `CommandService.ignoreUser`                     | `username`                                                     | `UnignoreUser`, `OpenUrl`                                                                       |
| `telegram.command_unignore_user`        | `CommandService.unignoreUser`                   | `username`                                                     | `IgnoreUser`, `OpenUrl`                                                                         |
| `telegram.command_ignore_post`          | `CommandService.ignoreSubmission`               | `reason` (or "None")                                           | `UnignoreSubmission` (`submissionFullName`), `OpenUrl`                                          |
| `telegram.command_unignore_post`        | `CommandService.unignoreSubmission`             | *(none)*                                                       | `IgnoreSubmission` (`submissionFullName`), `OpenUrl`                                            |
| `telegram.new_quote`                    | `PhraseService.addPhrase`                       | `content`, `type.name`, `source` (or "None")                   | *(none)*                                                                                        |
| *(hardcoded template)*                  | `ExceptionNotifier.notify`                      | `source`, `exceptionName`, `message`, `stackTrace`             | *(none)*                                                                                        |

---

## 4. Verification Commands

```bash
# Run Telegram bot tests
./gradlew test --tests "*TelegramBotTest*"
```
