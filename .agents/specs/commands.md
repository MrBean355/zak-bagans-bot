# Specification: Author Command System

This specification defines the author command parsing, authentication boundary, and execution semantics in
`CommandService`.

- **Primary Implementation**:
  - Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/CommandService.kt`
- **Unit Tests**:
  - `src/test/kotlin/com/github/mrbean355/zakbot/service/CommandServiceTest.kt`

---

## 1. Security & Execution Invariants for AI Agents

1. **Strict Author Authorization**: Commands MUST ONLY be processed if `comment.author == AuthorUsername`
   (`Mr_Bean355`). The check in `ContributionService.processComment()` is the gatekeeper.
2. **Mention Prefix Structure**: Commands must match `u/$BotUsername ` (case-insensitive).
  - Any leading forward slash (`/u/...`) is removed via `.removePrefix("/")`.
  - Matching is case-insensitive: `body.startsWith(COMMAND_PREFIX, ignoreCase = true)`.
3. **Markdown Pre-Rendering**: The raw comment body MUST be converted to plain text using CommonMark `asPlainText()`
   before checking the prefix. This eliminates escaped markdown characters (e.g. `ignore\_user` -> `ignore_user`).
4. **User Verification Before Ignore**: When `ignore_user` is executed, the bot MUST verify that the user exists on
   Reddit via `redditService.userExists(id)` before adding to `BotCache`.
5. **Contextual Scope for Posts**: For `ignore_post` and `unignore_post`, the target submission is always
   `comment.submissionFullName`.

---

## 2. Command Grammar & Parsing Rules

```kotlin
private const val COMMAND_PREFIX = "u/$BotUsername "

fun processAuthorCommand(comment: Comment) {
    val body = comment.body.asPlainText().removePrefix("/")
    if (!body.startsWith(COMMAND_PREFIX, ignoreCase = true)) {
        return
    }

    val command = body.substring(COMMAND_PREFIX.length).trim()
    val name = command.substringBefore(' ')
    val args = command.substringAfter(name).trim()

    when (name) {
        "ignore_user" -> ignoreUser(args, comment)
        "unignore_user" -> unignoreUser(args, comment)
        "ignore_post" -> ignoreSubmission(args, comment)
        "unignore_post" -> unignoreSubmission(comment)
    }
}
```

---

## 3. Command Action Matrix

### 3.1 `ignore_user <username>`

- **Args**: Target Reddit username.
- **Branch 1: Already Ignored**:
  - Check: `botCache.isUserIgnored(id) == true`
  - Action: No DB change.
  - Reply: `getString("reddit.command.ignoreUser.alreadyIgnored", id)`
- **Branch 2: Valid User**:
  - Check: `redditService.userExists(id) == true`
  - Action: `botCache.ignoreUser(id, comment.fullName)`
  - Reply: `getString("reddit.command.ignoreUser.success", id)`
  - Telegram: `getString("telegram.command_ignore_user", id, comment.url)`
- **Branch 3: User Not Found**:
  - Action: No DB change.
  - Reply: `getString("reddit.command.ignoreUser.notFound", id)`

### 3.2 `unignore_user <username>`

- **Args**: Target Reddit username.
- **Branch 1: Not Ignored**:
  - Check: `!botCache.isUserIgnored(id)`
  - Action: No DB change.
  - Reply: `getString("reddit.command.unignoreUser.notIgnored", id)`
- **Branch 2: Currently Ignored**:
  - Action: `botCache.unignoreUser(id)`
  - Reply: `getString("reddit.command.unignoreUser.success", id)`
  - Telegram: `getString("telegram.command_unignore_user", id, comment.url)`

### 3.3 `ignore_post [reason]`

- **Args**: Optional reason string.
- **Target**: `comment.submissionFullName`
- **Branch 1: Already Ignored**:
  - Check: `botCache.isSubmissionIgnored(submissionFullName) == true`
  - Action: No DB change.
  - Reply: `getString("reddit.command.ignoreSubmission.alreadyIgnored")`
- **Branch 2: Not Ignored**:
  - Action: `botCache.ignoreSubmission(submissionFullName, args.ifBlank { null })`
  - Reply: `getString("reddit.command.ignoreSubmission.success")`
  - Telegram: `getString("telegram.command_ignore_post", args.ifBlank { "None" }, comment.url)`

### 3.4 `unignore_post`

- **Target**: `comment.submissionFullName`
- **Branch 1: Not Ignored**:
  - Check: `!botCache.isSubmissionIgnored(submissionFullName)`
  - Action: No DB change.
  - Reply: `getString("reddit.command.unignoreSubmission.notIgnored")`
- **Branch 2: Currently Ignored**:
  - Action: `botCache.unignoreSubmission(submissionFullName)`
  - Reply: `getString("reddit.command.unignoreSubmission.success")`
  - Telegram: `getString("telegram.command_unignore_post", comment.url)`

---

## 4. Verification Commands

```bash
# Run all command service tests
./gradlew test --tests "*CommandServiceTest*"
```
