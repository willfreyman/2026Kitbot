# Team 10686: 2026 Kitbot robot code

FRC robot code (WPILib 2026, Java, command-based). The people working here are
high-school students who are learning Java and Git. They are not professional
developers.

## Always

- **Teach as you go.** Before running a command or changing code, say in one
  plain sentence what it does and why. Explain errors before fixing them. Keep
  it short and friendly, and never make anyone feel dumb for a mistake.
- **Branch + pull request for every change.** Never commit or push directly to
  `main`. Never force-push. Never merge a PR without the `merge-pr` checks
  (description, no conflicts, build passes).
- **Look before touching Git:** run `git status` first and say what it shows.
  Inspect existing staged work before adding files. Review `git diff --cached`
  before every commit; never include or unstage another task's work silently.
- **Ask before anything that can lose work** (`reset --hard`, `clean`, discarding
  changes, deleting branches), and before changing CAN IDs, current limits,
  pneumatic pressures, motor inversion, vendordeps, or the team number.
- **Commit before every deploy**, so everyone knows what code is on the robot.
- **Ask before every deploy, including rollback.** Confirm the intended robot
  and commit, that the robot is disabled, people are clear, and an operator is
  ready at the Driver Station. Building or troubleshooting is not permission to
  deploy. Firmware updates need separate explicit approval.
- In Bash, use `bash ./gradlew build`; in PowerShell, use `.\gradlew.bat build`.
  Local PR checks reduce mistakes but cannot lock `main` against concurrent
  merges. GitHub enforcement needs separately approved server-side checks.

## Skills

| Skill | Use it for |
|---|---|
| `git-workflow` | setup, starting a branch, committing, pushing, opening a PR, undoing mistakes |
| `merge-pr` | merging a PR safely, fixing merge conflicts |
| `build-deploy` | building, simulating, deploying, fixing build/deploy errors |
| `robot-code` | how the code is organized and conventions for changing it |
| `competition-day` | pre-event checklist, pit changes, rolling back |
