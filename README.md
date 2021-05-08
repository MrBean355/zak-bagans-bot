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

1. `mercury` - 75% chance to send a random [mercury phrase](src/main/resources/phrases/mercury.txt).
2. `situation` - 50% chance to send a random [situation phrase](src/main/resources/phrases/situation.txt).
3. `I feel` or `I'm feeling` - 50% chance to send a random [feeling phrase](src/main/resources/phrases/feeling.txt).
4. `Aaron` - 25% chance to send a random [Aaron phrase](src/main/resources/phrases/aaron.txt).
5. `understand` - 50% chance to send a random [understand phrase](src/main/resources/phrases/understand.txt).
6. `we want answers` - 50% chance to send a random [answers phrase](src/main/resources/phrases/answers.txt).
7. `Zak` or `Bagans` - 40% chance send a random [generic phrase](src/main/resources/phrases/generic.txt).

Each post/comment will only receive, at most, one reply from the bot. If the bot sends a reply for one of the keywords,
it will not check for any of the others.

All keywords have a chance to send a reply. If the chance prevents a reply from being sent, the next keyword in the list
will be checked instead. For example, if a comment mentions "situation", there's a 50% chance to reply with a
"situation phrase", and a 50% chance to skip to the next keyword in the list.

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
