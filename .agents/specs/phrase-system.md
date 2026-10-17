# Specification: Phrase Detection & Response Pipeline

This specification defines keyword evaluation, probability gating, least-used rotation, quote sources, and "Good Bot"
appreciation in `PhraseService`.

- **Primary Implementation**:
  - Service: `src/main/kotlin/com/github/mrbean355/zakbot/service/PhraseService.kt`
  - Phrase Interface: `src/main/kotlin/com/github/mrbean355/zakbot/phrases/Phrase.kt`
  - Keyword Phrase Implementations: `src/main/kotlin/com/github/mrbean355/zakbot/phrases/*`
- **Unit Tests**:
  - `src/test/kotlin/com/github/mrbean355/zakbot/service/PhraseServiceTest.kt`

---

## 1. Pipeline Invariants for AI Agents

1. **Pre-Sanitization**: All input text passed to `findPhrase()` MUST have URLs removed via `UrlRegex` before keyword
   checking:
   ```kotlin
   private val UrlRegex = """[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)""".toRegex()
   val text = input.lowercase().replace(UrlRegex, "")
   ```
2. **Descending Priority Evaluation**: Phrases must be checked in descending order of `priority`
   (`phrases.sortedByDescending { it.priority }`). The first phrase where
   `Random.nextFloat() <= phrase.getReplyChance(text)` succeeds claims the reply. No subsequent phrases are checked.
3. **Least-Used Selection**: When fetching quotes for a `PhraseType`, only quotes matching `it.usages == lowestUsage`
   are eligible candidates. If multiple candidates exist, pick one via `random()`.
4. **Immediate Usage Increment**: The chosen candidate's `usages` counter must be incremented (`entity.usages++`) and
   persisted immediately before formatting.
5. **Parentheses Escaping**: If `entity.source` is non-null, closing parentheses MUST be escaped
   (`source.replace("(", "\\(").replace(")", "\\)")`) before calling
   `getString("reddit.quote_source_prefix", entity.content, escapedSource)`.
6. **Author Substitution**: Any occurrence of `{author}` in quote text must be replaced with the triggering user's
   Reddit username.

---

## 2. Phrase Hierarchy & Keyword Implementations

| Priority | Class              | PhraseType              | Enum ID | Keyword Condition                  | Reply Probability |
|:--------:|:-------------------|:------------------------|:-------:|:-----------------------------------|:-----------------:|
|  **11**  | `ZozoPhrase`       | `PhraseType.Zozo`       |    8    | `"zozo" in message`                |   25% (`0.25f`)   |
|  **10**  | `MercuryPhrase`    | `PhraseType.Mercury`    |    4    | `"mercury" in message`             |   25% (`0.25f`)   |
|  **9**   | `SituationPhrase`  | `PhraseType.Situation`  |    5    | `"situation" in message`           |   20% (`0.20f`)   |
|  **8**   | `FeelingPhrase`    | `PhraseType.Feeling`    |    2    | `"i feel" in message               |                   | "i'm feeling" in message` | 25% (`0.25f`) |
|  **7**   | `TrinityPhrase`    | `PhraseType.Trinity`    |    6    | `"3" in message                    |                   | "three" in message` | 10% (`0.10f`) |
|  **3**   | `AaronPhrase`      | `PhraseType.Aaron`      |    0    | `\b(aaron)\b` in message           |   10% (`0.10f`)   |
|  **2**   | `UnderstandPhrase` | `PhraseType.Understand` |    7    | `"understand" in message`          |   20% (`0.20f`)   |
|  **1**   | `AnswersPhrase`    | `PhraseType.Answers`    |    1    | `"we want answers" in message`     |   25% (`0.25f`)   |
|  **0**   | `GenericPhrase`    | `PhraseType.Generic`    |    3    | `"zak" in message                  |                   | "bagans" in message` | 20% (`0.20f`) |
|   N/A    | Direct reply check | `PhraseType.GoodBot`    |    9    | `GOOD_BOT_PHRASES` in comment body |   10% (`0.10f`)   |

---

## 3. Formatting Contract

### 3.1 Quote with Source

```properties
reddit.quote_source_prefix=%s\n\n^(Quote from: %s)
```

- Content: `"We want answers."`
- Source: `"S01E01"`
- Output:
  ```text
  We want answers.

  ^(Quote from: S01E01)
  ```

### 3.2 Quote without Source

- Content: `"I feel a dark presence."`
- Source: `null`
- Output:
  ```text
  I feel a dark presence.
  ```

### 3.3 Good Bot Praise Response

- Triggered when comment is a reply to `BotUsername` and
  `comment.body.filter(Char::isLetter).lowercase() in setOf("goodbot", "greatbot", "bestbot")`.
- 10% chance: `randomRoll() <= 0.1f`.
- Quote fetched from `phraseService.getPhrase(PhraseType.GoodBot)?.substitute(comment)`.
- Applied footer:
  ```properties
  reddit.good_bot_footer=%s\n\n^(Thank you ❤️)
  ```
- Example output:
  ```text
  Thank you, ghost_hunter!

  ^(Thank you ❤️)
  ```

---

## 4. Verification Commands

```bash
# Run all phrase service and rotation tests
./gradlew test --tests "*PhraseServiceTest*"
```
