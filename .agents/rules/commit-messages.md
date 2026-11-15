# Git Commit Message Conventions

All Git commit messages in this repository must strictly adhere to the standards outlined in
[How to Write a Git Commit Message](https://cbea.ms/git-commit/) by Chris Beams.

---

## The Seven Rules

### 1. Separate Subject from Body with a Blank Line
Not all commits require a body—simple, self-explanatory changes can be one-liners. However, whenever
additional explanation or context is needed, always separate the subject line from the body with a
single blank line.

```text
Summarize change in 50 characters or less

More detailed explanatory text, if necessary. Wrap it to 72 characters.
Explain the problem that this commit is solving. Focus on why you are
making the change rather than how.
```

### 2. Limit the Subject Line to 50 Characters
* Aim for 50 characters or less for the subject line. Treat 72 characters as the hard ceiling.
* Being concise forces you to identify the single most important aspect of the change.
* Keeps `git log --oneline` clean, legible, and easy to skim.

### 3. Capitalize the Subject Line
* Begin the subject line with a capital letter.
* Example: `Add phrase matching unit tests`, not `add phrase matching unit tests`.

### 4. Do Not End the Subject Line with a Period
* Trailing punctuation is unnecessary space in a subject line.
* Example: `Catch exceptions in Reddit write operations`, not `Catch exceptions in Reddit write operations.`.

### 5. Use the Imperative Mood in the Subject Line
* Imperative mood means speaking or writing as if giving a command or instruction.
* **Test Rule**: A properly formed commit subject line must complete the following sentence:
  > *"If applied, this commit will **\<your subject line here\>***"*
* **Do Use**: `Add`, `Fix`, `Update`, `Change`, `Remove`, `Prevent`, `Refactor`, `Handle`.
* **Do NOT Use**: `Added`, `Adds`, `Adding`, `Fixed`, `Fixes`, `Fixing`, `Updates`.

### 6. Wrap the Body at 72 Characters
* Git never wraps text automatically when displaying commit logs (`git log` indents text by 4 spaces).
* Hard-wrap body paragraphs manually at 72 characters to ensure clean display margins across terminals and tools.

### 7. Use the Body to Explain *What* and *Why* vs. *How*
* The code diff already shows *how* the change was implemented.
* The commit body must explain:
  1. **Why** the change is necessary (what problem existed or what motivated the change).
  2. **What** the expected outcome or behaviour change is.
  3. Any side effects, trade-offs, or non-obvious context.

---

## Repository Examples

### Multi-line Feature/Enhancement
```text
Match Aaron keyword using word boundaries

Update AaronPhrase to match 'aaron' using regex word boundaries
rather than substring containment.

Prevents false positives on words that contain 'aaron' as a substring.
```

### Multi-line Bug Fix / Error Handling
```text
Catch exceptions in Reddit write operations

Wrap postCommentReply, setBotFlair, getFlairOptions, and
findParentComment in try-catch blocks with error logging.

Ensures transient Reddit API 5xx or network errors do not terminate
the scheduled polling cycle.
```

### Single-line Chores / Bumps
```text
Prepare for development of 3.2.0
```
