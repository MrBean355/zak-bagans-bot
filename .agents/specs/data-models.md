# Specification: Database Schema & Data Models

This specification defines the PostgreSQL tables, JPA entities, converters, and cache operations in
`com.github.mrbean355.zakbot.db`.

- **Primary Implementation**:
  - Entities: `src/main/kotlin/com/github/mrbean355/zakbot/db/entity/*`
  - Repositories: `src/main/kotlin/com/github/mrbean355/zakbot/db/repo/*`
  - Cache Layer: `src/main/kotlin/com/github/mrbean355/zakbot/db/BotCache.kt`
  - Enum & Converter: `src/main/kotlin/com/github/mrbean355/zakbot/db/PhraseType.kt`, `PhraseTypeConverter.kt`
- **Unit Tests**:
  - `src/test/kotlin/com/github/mrbean355/zakbot/db/PhraseTypeTest.kt`
  - `src/test/kotlin/com/github/mrbean355/zakbot/db/PhraseTypeConverterTest.kt`

---

## 1. Persistence Invariants for AI Agents

1. **Integer Enum Mapping**: The database column `phrase.type` stores the integer ID of `PhraseType`, NOT the string
   name. Mapped via `PhraseTypeConverter: AttributeConverter<PhraseType, Int>`.
2. **Cold Start Cursors**: In `BotCache`, if no record exists for `"post"` or `"comment"` in `last_checked`, the cache
   MUST initialize the value to `currentTime()` (`SystemClock.currentTimeMillis`) to avoid polling from epoch 0.
3. **Transaction Boundaries**:
  - `BotCache` is annotated with `@Transactional`. Read-only queries (`isUserIgnored`, `isSubmissionIgnored`) are
    annotated with `@Transactional(readOnly = true)`.
4. **Primary Key Constraints**:
  - `ignored_user.id` is the Reddit username.
  - `ignored_submission.full_name` is the Reddit submission fullname (`t3_...`).
  - `last_checked.key` is `"post"` or `"comment"`.

---

## 2. Table Specifications & JPA Mappings

### 2.1 `phrase` (`PhraseEntity`)

```kotlin
@Entity
@Table(name = "phrase")
data class PhraseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val content: String,
    var usages: Int = 0,
    @Convert(converter = PhraseTypeConverter::class)
    val type: PhraseType,
    val source: String? = null,
)
```

- **Query `findMinUsagesByType`**:
  ```kotlin
  @Query("SELECT min(p.usages) FROM PhraseEntity p WHERE p.type = :type")
  fun findMinUsagesByType(type: PhraseType): Int?
  ```

### 2.2 `ignored_user` (`IgnoredUserEntity`)

```kotlin
@Entity
@Table(name = "ignored_user")
data class IgnoredUserEntity(
    @Id val id: String,
    val time: Instant,
    val source: String,
)
```

### 2.3 `ignored_submission` (`IgnoredSubmissionEntity`)

```kotlin
@Entity
@Table(name = "ignored_submission")
data class IgnoredSubmissionEntity(
    @Id val fullName: String,
    val time: Instant,
    val reason: String? = null,
)
```

### 2.4 `last_checked` (`LastCheckedEntity`)

```kotlin
@Entity
@Table(name = "last_checked")
data class LastCheckedEntity(
    @Id val key: String,
    val value: Instant,
)
```

### 2.5 `app_user` (`AppUserEntity`)

```kotlin
@Entity
@Table(name = "app_user")
data class AppUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val username: String,
    var password: String,
    val role: String = "ROLE_ADMIN",
)
```

---

## 3. `PhraseType` Enum Contract

```kotlin
enum class PhraseType(val id: Int) {
    Aaron(0),
    Answers(1),
    Feeling(2),
    Generic(3),
    Mercury(4),
    Situation(5),
    Trinity(6),
    Understand(7),
    Zozo(8),
    GoodBot(9);
}
```

- **JSON Deserialization**: Supports parsing from either number (`0`–`9`), numeric string (`"0"`–`"9"`), or name
  (`"Aaron"`, `"aaron"`).

---

## 4. Verification Commands

```bash
# Run database enum and converter tests
./gradlew test --tests "*PhraseTypeTest*" --tests "*PhraseTypeConverterTest*"
```
