# Specification: Telegram Bot & Telemetry

This specification defines the Telegram notifier abstraction, long-polling telemetry bot, `/status` command, and
outbound event catalog.

- **Primary Implementation**:
    - Bot & Notifier: `src/main/kotlin/com/github/mrbean355/zakbot/TelegramBot.kt`
    - Configuration: `src/main/kotlin/com/github/mrbean355/zakbot/config/TelegramProperties.kt`
- **Unit Tests**:
    - `src/test/kotlin/com/github/mrbean355/zakbot/TelegramBotTest.kt`

---

## 1. Telemetry Invariants for AI Agents

1. **Profile Isolation**: In the `dev` profile, the application MUST bind `StubTelegramNotifier` instead of
   `TelegramBot`. Never attempt to instantiate Telegram clients or make external network calls when `@Profile("dev")` is
   active.
2. **Formatting & Preview**: All Telegram messages MUST use `ParseMode.MARKDOWN` with link previews disabled
   (`LinkPreviewOptions.builder().isDisabled(true).build()`).
3. **Escaping**: Since Markdown mode is used, avoid unescaped Markdown syntax that could break Telegram message
   delivery.
4. **Chat ID Binding**: Outbound messages are always sent to `telegramProperties.chatId`.
5. **Resilient Delivery**: Failures in `sendMessage()` (e.g. Telegram API outages or Markdown parsing issues) MUST be
   caught and logged as errors, never propagating exceptions that could abort background processing.

---

## 2. Inbound Telemetry (`/status`)

Implemented in `TelegramBot.consume()`:

```kotlin
val text = update.message?.text.orEmpty().trim().lowercase()
if (text == "/status" || text.startsWith("/status@")) {
    sendMessage(buildStatusMessage())
}
```

### 2.1 Telemetry Composition

`buildStatusMessage()` aggregates runtime data using the template `telegram.bot_status_response`:
```text
telegram.bot_status_response=📊 *ZakBot Status*\n\n*System*\n• Version: `%s`\n• Uptime: %s\n• Memory: `%s`\n\n*Reddit (/r/%s)*\n• Quotes in DB: %d\n• Ignored users: %d\n• Ignored posts: %d
```

- **Version**: `buildProperties.version`
- **Uptime**: Calculated via `Duration.between(Instant.ofEpochMilli(applicationContext.startupDate), Instant.now())`.
  Formats only non-zero units (days, hours, minutes, seconds). Defaults to `"< 1 second"` if startup duration is zero.
- **Memory**: `${usedMb}MB / ${maxMb}MB` where:
    - `usedMb = (totalMemory - freeMemory) / (1024 * 1024)`
    - `maxMb = maxMemory / (1024 * 1024)`
- **Subreddit**: `SubredditName` constant.
- **Counters**:
    - Quotes in DB: `phraseRepository.count()`
    - Ignored Users: `ignoredUserRepository.count()`
    - Ignored Posts: `ignoredSubmissionRepository.count()`

---

## 3. Outbound Event Notification Catalog

| String Resource Key                   | Trigger Location                                | Parameters                                       |
|:--------------------------------------|:------------------------------------------------|:-------------------------------------------------|
| `telegram.new_submission`             | `ContributionService.processSubmission`         | `submission.title`, `response`, `submission.url` |
| `telegram.new_comment`                | `ContributionService.processComment`            | `comment.body`, `response`, `comment.url`        |
| `telegram.new_bot_mention_submission` | `ContributionService.checkForBotMention`        | `submission.title`, `submission.url`             |
| `telegram.new_bot_mention_comment`    | `ContributionService.checkForBotMention`        | `comment.body`, `comment.url`                    |
| `telegram.new_ignored_user`           | `ContributionService.processComment` (bad bot)  | `comment.author`, `comment.url`                  |
| `telegram.good_bot_praise`            | `ContributionService.processComment` (good bot) | `comment.author`, `response`, `comment.url`      |
| `telegram.deleted_downvoted_comment`  | `ContributionService.checkDownvotedComments`    | `comment.score`, `comment.body`, `comment.url`   |
| `telegram.command_ignore_user`        | `CommandService.ignoreUser`                     | `username`, `comment.url`                        |
| `telegram.command_unignore_user`      | `CommandService.unignoreUser`                   | `username`, `comment.url`                        |
| `telegram.command_ignore_post`        | `CommandService.ignoreSubmission`               | `reason` (or "None"), `comment.url`              |
| `telegram.command_unignore_post`      | `CommandService.unignoreSubmission`             | `comment.url`                                    |
| `telegram.new_quote`                  | `PhraseService.addPhrase`                       | `content`, `type.name`, `source` (or "None")     |

---

## 4. Verification Commands

```bash
# Run Telegram bot tests
./gradlew test --tests "*TelegramBotTest*"
```
