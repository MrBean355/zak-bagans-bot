# Zak Bagans Reddit Bot

![Zak Bagans](docs/zak.jpg)

*There are things in this world we will never fully understand... <sup>understand</sup>*

[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=MrBean355_zak-bagans-bot&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=MrBean355_zak-bagans-bot)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=MrBean355_zak-bagans-bot&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=MrBean355_zak-bagans-bot)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=MrBean355_zak-bagans-bot&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=MrBean355_zak-bagans-bot)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=MrBean355_zak-bagans-bot&metric=coverage)](https://sonarcloud.io/summary/new_code?id=MrBean355_zak-bagans-bot)

## Welcome

This is a Reddit bot for [r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/) which replies to posts and
comments that mention various Zak-related keywords with random quotes.

Credits go to [u/shiverstar](https://www.reddit.com/user/shiverstar/) for the amazing idea! It was discussed in
[this post](https://www.reddit.com/r/GhostAdventures/comments/mguuyi/we_need_a_zakbot/).

## How It Works

Every 15 minutes, the bot checks for new posts and comments on
[r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/). The content of each post/comment is checked for various
keywords, and a reply may be sent if the keywords match. **Keywords are checked in this order**:

1. `zozo` - 25% chance to send a random
   [Z̶̞̼̔̍o̶̮͇̕z̷̜͓̅̽ỡ̵̗ ̶̹͚̔̔p̵̂͜ḣ̷͓̜̏r̷͙͘̕ȃ̴̰̞s̵̹̗̈́̔e̴͚̻̒͊](https://zak-bagans-bot.herokuapp.com#zozo).
2. `mercury` - 25% chance to send a random [mercury phrase](https://zak-bagans-bot.herokuapp.com#mercury).
3. `situation` - 20% chance to send a random [situation phrase](https://zak-bagans-bot.herokuapp.com#situation).
4. `I feel` or `I'm feeling` - 25% chance to send a random
   [feeling phrase](https://zak-bagans-bot.herokuapp.com#feeling).
5. `3` or `three` - 10% chance to send a random [trinity phrase](https://zak-bagans-bot.herokuapp.com#trinity).
6. `Aaron` - 10% chance to send a random [Aaron phrase](https://zak-bagans-bot.herokuapp.com#aaron).
7. `understand` - 20% chance to send a random [understand phrase](https://zak-bagans-bot.herokuapp.com#understand).
8. `we want answers` - 25% chance to send a random [answers phrase](https://zak-bagans-bot.herokuapp.com#answers).
9. `Zak` or `Bagans` - 20% chance to send a random [generic phrase](https://zak-bagans-bot.herokuapp.com#generic).

Each post/comment will only receive, at most, one reply from the bot. If the bot sends a reply for one of the keywords,
it will not check for any of the others.

All keywords have a chance to send a reply. If the chance prevents a reply from being sent, the next keyword in the list
will be checked instead. For example, if a comment mentions "situation", there's a 20% chance to reply with a "situation
phrase", and an 80% chance to skip to the next keyword in the list.

## Opting Out

If you find the bot annoying, you can reply to one of its comments with `bad bot`. This will make the bot ignore all of
your future posts and comments.

## Special Commands

There are some commands that can be sent to the bot via comments on Reddit, and **only the bot's author can send them**.

- `!ZakBagansBot ignore_user <username>` - Ignore the user's future posts and comments.
- `!ZakBagansBot unignore_user <username>` - Stop ignoring the user.
- `!ZakBagansBot ignore_post <reason>` - Ignore all comments on the current post. The reason is optional.
- `!ZakBagansBot unignore_post` - Stop ignoring the current post.

## Contributing

Any contributions by the community are welcome!

### Adding Responses

If you would like to add more Zak responses to the bot, please
[search the existing responses](https://zak-bagans-bot.herokuapp.com) first, to see if there's a similar one already
added. If not, you can [open an issue on GitHub](https://github.com/MrBean355/zak-bagans-bot/issues/new/choose), using
the issue template to get started. If you're unfamiliar with GitHub, feel free to
[chat to me on Reddit](https://www.reddit.com/user/Mr_Bean355) instead.

### Other Contributions

- Pull requests are welcome!
- [Open an issue on GitHub](https://github.com/MrBean355/zak-bagans-bot/issues/new/choose) for any feedback related to
  the project.
- [Message me on Reddit](https://www.reddit.com/user/Mr_Bean355) if you'd prefer.

## Local Development

### Prerequisites

- **Java 25** (JDK 25)
- **PostgreSQL** running locally on port 5432 with a database named `zakbot` (credentials `postgres` / `root`),
  configurable in `src/main/resources/application-dev.properties`.

### Running Locally

Start the application with the `dev` profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

When running in `dev` mode:

- **Reddit replies are disabled** (`zakbot.replies.enabled=false`) to prevent sending live replies.
- **Telegram notifications are stubbed**, logging messages to standard output instead of contacting Telegram.
- **Admin user**: An initial admin account (`admin` / `password`) is automatically created if no users exist.
- **Web UI**: Access the phrase viewer at `http://localhost:8080/` and the admin panel at
  `http://localhost:8080/admin.html`.

### Configuration

The bot can be configured using standard Spring Boot properties or environment variables:

| Property                  | Environment Variable     | Default    | Description                                      |
|---------------------------|--------------------------|------------|--------------------------------------------------|
| `reddit.account-password` | `BOT_ACCOUNT_PASSWORD`   | *(empty)*  | Reddit account password for authentication       |
| `reddit.client-secret`    | `BOT_CLIENT_SECRET`      | *(empty)*  | Reddit script application client secret          |
| `telegram.token`          | `TELEGRAM_TOKEN`         | *(empty)*  | Telegram Bot token for status updates and alerts |
| `telegram.chat-id`        | `TELEGRAM_CHAT_ID`       | `44692593` | Telegram chat ID to receive notifications        |
| `admin.username`          | `ADMIN_USERNAME`         | `admin`    | Username for the admin web dashboard             |
| `admin.password`          | `ADMIN_PASSWORD`         | *(empty)*  | Password for the admin web dashboard             |
| `zakbot.replies.enabled`  | `ZAKBOT_REPLIES_ENABLED` | `true`     | Toggle posting live replies to Reddit            |

### Testing

Run unit tests and verify the build:

```bash
./gradlew check
```
