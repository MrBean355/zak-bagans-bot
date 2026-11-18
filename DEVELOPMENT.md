# Development & Configuration

This guide covers the tech stack, project structure, local setup, configuration, API endpoints, testing, and conventions
for working on **Zak Bagans Bot**.

## Tech Stack

- **Language & Runtime**: Kotlin on JVM
- **Framework**: Spring Boot (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-security`, `spring-boot-starter-validation`)
- **Database**: PostgreSQL (`org.postgresql:postgresql`)
- **Integrations**:
    - Reddit OAuth2 API via Spring `RestClient`
    - Telegram Bot API via `telegrambots-client` & `telegrambots-springboot-longpolling-starter`
    - Google Gemini API via Spring `RestClient` (content moderation & sentiment)
    - Markdown rendering via `org.commonmark:commonmark`
- **Testing & Quality**: JUnit Jupiter, MockK, JaCoCo, SonarCloud

## Prerequisites

- **Java 25** (JDK 25)
- **PostgreSQL** running locally on port `5432` with a database named `zakbot` (default dev credentials `postgres` /
  `root`, configurable in `src/main/resources/application-dev.properties`)
- **Telegram Bot credentials** (`TELEGRAM_TOKEN` and `TELEGRAM_CHAT_ID`) — required at startup
- **Optional for live integrations**:
    - Reddit script app credentials (`BOT_ACCOUNT_PASSWORD`, `BOT_CLIENT_SECRET`)
    - Google Gemini API key (`GEMINI_API_KEY`)

## Project Structure

Key classes under `src/main/kotlin/com/github/mrbean355/zakbot/`:

- `ZakBagansBot.kt` — scheduled polling loops (15-minute contribution check, midnight flair update)
- `service/ContributionService.kt` — processes posts and comments, coordinating moderation, commands, and replies
- `reddit/RedditService.kt` — Reddit OAuth2 API client
- `telegram/TelegramBot.kt` — Telegram long-polling listener, inline action callbacks, and notifications
- `service/PhraseService.kt` & `controller/PhraseApiController.kt` — quote rotation and REST API
- `service/BotStateService.kt` — persisted bot state (polling cursors and ignored users/posts)

Resources under `src/main/resources/` include `application.properties`, `application-dev.properties`,
`strings.properties` (message templates), and `static/` (`index.html`, `admin.html`).

## Running Locally

Start the application with the `dev` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

You will need `TELEGRAM_TOKEN` and `TELEGRAM_CHAT_ID` exported in your environment (or passed as Spring properties), as
they are validated at startup.

When running in `dev` mode (`application-dev.properties`):

- **Reddit replies are disabled** (`zakbot.replies.enabled=false`) so the bot will not post live replies to Reddit.
- **Telegram notifications remain enabled**.
- **Database**: Connects to `jdbc:postgresql://localhost:5432/zakbot` (`postgres` / `root`).
- **Admin user**: If `ADMIN_PASSWORD` is unset and no users exist in the database, `UserInitializer` creates a default
  `admin` / `password` account. If `ADMIN_PASSWORD` is provided, that user is created or updated on startup.
- **Web UI**:
    - Public phrase viewer: `http://localhost:8080/`
    - Admin dashboard: `http://localhost:8080/admin.html`

## Configuration

The bot can be configured using standard Spring Boot properties or environment variables:

| Property                                  | Environment Variable                      | Default                 | Description                                              |
|-------------------------------------------|-------------------------------------------|-------------------------|----------------------------------------------------------|
| `reddit.account-password`                 | `BOT_ACCOUNT_PASSWORD`                    | *(required)*            | Reddit account password for authentication               |
| `reddit.client-secret`                    | `BOT_CLIENT_SECRET`                       | *(required)*            | Reddit script application client secret                  |
| `telegram.token`                          | `TELEGRAM_TOKEN`                          | *(required)*            | Telegram Bot token for status updates and alerts         |
| `telegram.chat-id`                        | `TELEGRAM_CHAT_ID`                        | *(required)*            | Telegram chat ID to receive notifications                |
| `admin.username`                          | `ADMIN_USERNAME`                          | `admin`                 | Username for the admin web dashboard                     |
| `admin.password`                          | `ADMIN_PASSWORD`                          | *(empty)*               | Password for the admin web dashboard                     |
| `gemini.api-key`                          | `GEMINI_API_KEY`                          | *(empty)*               | Google Gemini API key for moderation and sentiment       |
| `gemini.model`                            | `GEMINI_MODEL`                            | `gemini-3.5-flash-lite` | Gemini model identifier used for interactions            |
| `gemini.enabled`                          | `GEMINI_ENABLED`                          | `true`                  | Toggle Gemini content moderation and sentiment           |
| `gemini.dry-run`                          | `GEMINI_DRY_RUN`                          | `true`                  | Send Telegram alerts for Gemini decisions without acting |
| `zakbot.replies.enabled`                  | `ZAKBOT_REPLIES_ENABLED`                  | `true`                  | Toggle posting live replies to Reddit                    |
| `zakbot.downvotes.enabled`                | `ZAKBOT_DOWNVOTES_ENABLED`                | `true`                  | Toggle automatic deletion of downvoted bot comments      |
| `zakbot.downvotes.delete-threshold`       | `ZAKBOT_DOWNVOTES_DELETE_THRESHOLD`       | `-3`                    | Score threshold at or below which bot comments delete    |
| `zakbot.moderation.sensitivity-threshold` | `ZAKBOT_MODERATION_SENSITIVITY_THRESHOLD` | `0.7`                   | Sensitivity score threshold (0.0–1.0) for moderation     |

## Telegram Admin Bot

The Telegram bot listens for commands and inline button callbacks from the configured `telegram.chat-id`:

- `/status` — Displays runtime telemetry (app version, uptime, JVM memory usage, Gemini moderation status, and database
  statistics) with an inline **Refresh** button.
- **Inline alert actions** — Outbound notifications attach contextual action buttons where applicable:
    - **Open on Reddit** — Direct link to the submission or comment
    - **Ignore User / Unignore User** — Toggle ignoring the Reddit author
    - **Ignore Post / Unignore Post** — Toggle ignoring the submission
    - **Delete Comment** — Delete the bot's Reddit reply (with confirmation)

## Web UI & REST API

- `GET /api/phrases` *(public)* — Returns all stored `PhraseDto` items (`id`, `content`, `type`, `source`).
- `POST /api/phrases` *(requires `ROLE_ADMIN`)* — Creates a new phrase (`CreatePhraseDto`: non-blank `content` <= 255
  chars, `type`, optional `source` <= 255 chars). Newly added phrases are initialized with
  `usages = findMinUsagesByType(type) ?: 0` so they enter rotation fairly.
- **CSRF**: Mutating requests use Spring's `CookieCsrfTokenRepository.withHttpOnlyFalse()`, expecting the `XSRF-TOKEN`
  cookie value in the `X-XSRF-TOKEN` header.

## Building, Testing & Coverage

Run the unit test suite:

```bash
./gradlew test
```

Run the full verification lifecycle (tests + checks):

```bash
./gradlew check
```

Generate the JaCoCo XML/HTML coverage report (`build/reports/jacoco/test/`):

```bash
./gradlew jacocoTestReport
```

Build the executable Spring Boot fat JAR (`build/libs/zakbot.jar`):

```bash
./gradlew bootJar
```

## Coding Conventions & Specifications

- **Dependency Injection**: Spring components and services must use explicit constructor injection with **no default
  parameter values** for dependencies (see [
  `.agents/rules/dependency-injection.md`](.agents/rules/dependency-injection.md)).
- **Message Strings**: Keep user-facing Reddit and Telegram messages in [
  `src/main/resources/strings.properties`](src/main/resources/strings.properties) via `getString()` rather than
  hardcoding them in Kotlin.
- **Commit Messages**: Follow the seven rules from [cbea.ms/git-commit](https://cbea.ms/git-commit/) (50-char
  capitalized imperative subject without a period, blank line, body wrapped at 72 chars explaining *what* and *why*).
- **Architecture & Specs**: Detailed functional and technical specifications live in [
  `.agents/specs/`](.agents/specs/README.md) (covering the bot engine, phrase rotation system, author commands,
  telemetry/Telegram, API/security, and database schema).
