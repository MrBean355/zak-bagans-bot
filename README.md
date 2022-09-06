# Zak Bagans Reddit Bot

![Zak Bagans](zak.jpg)

*There are things in this world we will never fully understand... <sup>understand</sup>*

## Welcome

This is a Reddit bot for [r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/) which replies to posts and
comments that mention various Zak-related keywords with random quotes.

Credits go to [u/shiverstar](https://www.reddit.com/user/shiverstar/) for the amazing idea! It was discussed
in [this post](https://www.reddit.com/r/GhostAdventures/comments/mguuyi/we_need_a_zakbot/).

## How It Works

Every 5 minutes, the bot checks for new posts and comments
on [r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/). The content of each post/comment is checked for
various keywords, and a reply may be sent if the keywords match. **Keywords are checked in this order**:

1. `zozo` - 75% chance to send a
   random [Z̶̞̼̔̍o̶̮͇̕z̷̜͓̅̽ỡ̵̗ ̶̹͚̔̔p̵̂͜ḣ̷͓̜̏r̷͙͘̕ȃ̴̰̞s̵̹̗̈́̔e̴͚̻̒͊](https://zak-bagans-bot.herokuapp.com#zozo).
2. `mercury` - 75% chance to send a random [mercury phrase](https://zak-bagans-bot.herokuapp.com#mercury).
3. `situation` - 50% chance to send a random [situation phrase](https://zak-bagans-bot.herokuapp.com#situation).
4. `I feel` or `I'm feeling` - 50% chance to send a
   random [feeling phrase](https://zak-bagans-bot.herokuapp.com#feeling).
5. `3` or `three` - 33% chance to send a random [trinity phrase](https://zak-bagans-bot.herokuapp.com#trinity).
6. `Aaron` - 25% chance to send a random [Aaron phrase](https://zak-bagans-bot.herokuapp.com#aaron).
7. `understand` - 50% chance to send a random [understand phrase](https://zak-bagans-bot.herokuapp.com#understand).
8. `we want answers` - 50% chance to send a random [answers phrase](https://zak-bagans-bot.herokuapp.com#answers).
9. `Zak` or `Bagans` - 40% chance send a random [generic phrase](https://zak-bagans-bot.herokuapp.com#generic).

Each post/comment will only receive, at most, one reply from the bot. If the bot sends a reply for one of the keywords,
it will not check for any of the others.

All keywords have a chance to send a reply. If the chance prevents a reply from being sent, the next keyword in the list
will be checked instead. For example, if a comment mentions "situation", there's a 50% chance to reply with a
"situation phrase", and a 50% chance to skip to the next keyword in the list.

## Opting Out

If you find the bot annoying, you can reply to one of its comments with `bad bot`. This will make the bot ignore all of
your future posts and comments.

## Contributing

Any contributions by the community are welcome!

### Adding Responses

If you would like to add more Zak responses to the bot, please
[open an issue on GitHub](https://github.com/MrBean355/zak-bagans-bot/issues/new/choose), using the issue template to
get started. If you're unfamiliar with GitHub, feel free to
[chat to me on Reddit](https://www.reddit.com/user/Mr_Bean355) instead.

### Other Contributions

- Pull requests are welcome!
- [Open an issue on GitHub](https://github.com/MrBean355/zak-bagans-bot/issues/new/choose) for any feedback related to
- the project.
- [Message me on Reddit](https://www.reddit.com/user/Mr_Bean355) if you'd prefer.
