# AI Agent Guidelines & Architecture Rules

This repository is governed by the technical and functional specifications documented
in [.agents/specs/](.agents/specs/README.md).

## Core Directives for AI Agents

1. **Architecture & File Map**: See [.agents/specs/README.md](.agents/specs/README.md) for source file mapping and
   global system invariants.
2. **Reddit Bot Engine**: See [.agents/specs/bot-engine.md](.agents/specs/bot-engine.md) for 15-minute polling
   schedules, Reddit pagination bounding (max 10 pages of 5 items), and contribution filtering rules.
3. **Phrase Matching & Quotes**: See [.agents/specs/phrase-system.md](.agents/specs/phrase-system.md) for priority
   hierarchy (Zozo=11 down to Generic=0), least-used rotation algorithms, `{author}` placeholder replacement, quote
   source escaping, and "Good Bot" footer formatting.
4. **Author Commands**: See [.agents/specs/commands.md](.agents/specs/commands.md) for comment command parsing
   (`u/ZakBagansBot`), author authentication (`Mr_Bean355`), and ignore user/post operations.
5. **Telemetry & Telegram**:
   See [.agents/specs/telemetry-and-notifications.md](.agents/specs/telemetry-and-notifications.md) for Telegram bot
   commands (`/status`), Markdown mode, and event notification triggers.
6. **API & Security**: See [.agents/specs/api-and-security.md](.agents/specs/api-and-security.md) for REST endpoints
   (`/api/phrases`), CSRF handling, and Content Security Policy.
7. **Database & Cache**: See [.agents/specs/data-models.md](.agents/specs/data-models.md) for table schemas,
   `PhraseTypeConverter`, and `BotCache` operations.

## Testing & Verification

Always verify your changes before finishing a turn:

```bash
./gradlew test
```
