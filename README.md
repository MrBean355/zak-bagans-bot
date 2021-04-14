# Zak Bagans Reddit Bot

This is a Reddit bot for [r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/) which replies to messages that
mention Zak with random quotes.

Credits go to [u/shiverstar](https://www.reddit.com/user/shiverstar/) for the idea! It was discussed
in [this post](https://www.reddit.com/r/GhostAdventures/comments/mguuyi/we_need_a_zakbot/).

## How It Works

Every 5 minutes, the bot checks for new posts and comments
on [r/GhostAdventures](https://www.reddit.com/r/GhostAdventures/). The content of each post/comment is checked in this
order:

1. Mentions `Aaron`: chance to send an [Aaron phrase](src/main/resources/phrases/aaron.txt).
2. Mentions `we want answers`: send an [answers phrase](src/main/resources/phrases/answers.txt).
3. Mentions `understand`: chance to send an [understand phrase](src/main/resources/phrases/understand.txt).
4. Mentions `Zak` or `Bagans`: send a [generic phrase](src/main/resources/phrases/generic.txt).

## Contributing

Any contributions by the community are welcome!

### Pull Requests

If you're familiar with making pull requests on GitHub, feel free to go ahead!

- Have a look at the various [response files](src/main/resources/phrases). There are comments inside each file that
  explain them a bit better.
- To add a new response, simply add a new line in the respective file and open a pull request to the `develop` branch!

### Contact Me

You can also open a GitHub issue on this project if you'd like to provide feedback.

If you're unfamiliar with GitHub, feel free to send me
a [private message on Reddit](https://www.reddit.com/user/Mr_Bean355) with your feedback!