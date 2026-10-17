# Project Architecture & System Specifications

When implementing features, refactoring, or debugging code in this repository, always align changes with the project
specifications documented in `.agents/specs/`:

1. **System Architecture**: Consult [.agents/specs/README.md](../specs/README.md) for tech stack, topology, and
   component boundaries.
2. **Bot Engine & Polling**: Consult [.agents/specs/bot-engine.md](../specs/bot-engine.md) for scheduling intervals
   (15-min contributions, midnight flair), Reddit pagination rules, state cursors, and filtering pipelines.
3. **Phrase System**: Consult [.agents/specs/phrase-system.md](../specs/phrase-system.md) for keyword evaluation
   hierarchy, probability gating, least-used quote rotation, author name substitution, quote sources, and "good bot"
   praise logic.
4. **Author Commands**: Consult [.agents/specs/commands.md](../specs/commands.md) for comment command syntax
   (`u/ZakBagansBot`), author authentication check, and user/post ignore operations.
5. **Telemetry & Notifications**:
   Consult [.agents/specs/telemetry-and-notifications.md](../specs/telemetry-and-notifications.md) for Telegram bot
   commands (`/status`), profile switching (`dev` vs `!dev`), and notification triggers.
6. **API & Security**: Consult [.agents/specs/api-and-security.md](../specs/api-and-security.md) for REST endpoints
   (`/api/phrases`), DTO validation, CSRF cookie handling, Basic Auth, and Content Security Policy.
7. **Data Models**: Consult [.agents/specs/data-models.md](../specs/data-models.md) for PostgreSQL table schemas, JPA
   entities, converters, and `BotCache` persistence.
