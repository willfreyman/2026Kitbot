---
name: git-workflow
description: >-
  Everyday Git for Team 10686's robot code, written for students who are new to Git.
  Covers first-time setup, starting work on a new branch, saving (committing) with a
  real message, pushing, opening a pull request with gh, bringing main into a branch,
  and undoing mistakes. Use whenever someone wants to start a change, save or commit,
  push, sync, "get the latest code", open a PR, or says they messed up Git, are lost,
  or committed to main by accident. For merging a PR, use the merge-pr skill.
---

# Git workflow for the robot code

The team's rule: **every change goes on its own branch and becomes a pull request
(PR).** Nobody pushes straight to `main`. Anyone on the team can merge a PR, but
only through the `merge-pr` skill, which blocks on conflicts, a failing build, or
an empty description.

The repo is `willfreyman/2026Kitbot` on GitHub. Students use their own laptops and
their own GitHub accounts.

## How to talk while doing this

The person you're helping is a high-school student who probably doesn't know Git.

- Before each Git command, say in one plain sentence what it does and why.
  Example: "`git pull` downloads everyone else's newest changes into your copy."
- Use the same words every time: **branch** = your own copy of the code to work
  in, **commit** = a saved snapshot with a note, **push** = upload your commits to
  GitHub, **pull** = download other people's commits, **PR** = a request to add
  your branch into `main`.
- Show command output when it matters, and explain any error before you fix it.
- Never make a student feel dumb for a mistake. Almost everything in Git can be
  undone, so say that.
- Keep it short. One idea at a time.

## Hard rules (never break these, even if asked casually)

1. Never `git push` to `main`. If someone asks, explain the rule and open a PR
   instead.
2. Never force-push (`--force`, `-f`, `--force-with-lease`).
3. Never run `git reset --hard`, `git clean`, `git checkout -- <file>`,
   `git restore <file>`, `git stash drop`, or `git branch -D` unless the student
   has said yes after you explained exactly what will be lost. Show the files
   first.
4. Never use `git add -A` or `git add .` without first showing `git status` and
   checking the list (see "Saving work").
5. Never merge a PR from this skill. Hand off to `merge-pr`.
6. Never `gh pr merge --admin`, and never change GitHub repo settings.

## Step 0: always look before doing anything

Run these and read them before every task:

```bash
git status
git branch --show-current
```

Tell the student, in plain words, which branch they're on and whether they have
unsaved changes. If they're on `main` with changes, that's fine: the next step
moves those changes onto a new branch without losing anything.

## First-time setup (once per laptop)

Check each item and fix only what's missing:

```bash
git --version                 # Git installed? (WPILib's VS Code does not install Git)
git config --global user.name   # should be the student's real name
git config --global user.email  # should match their GitHub account email
gh --version                  # GitHub CLI installed?
gh auth status                # logged in to GitHub?
```

- Missing name or email: `git config --global user.name "First Last"` and
  `git config --global user.email "them@example.com"`. Ask for the values;
  don't guess.
- No `gh`: point them to https://cli.github.com (Windows: `winget install --id GitHub.cli`,
  Mac: `brew install gh`), then `gh auth login` (choose GitHub.com, HTTPS, log in
  with a browser).
- No access to the repo: they need to be added as a collaborator on
  `willfreyman/2026Kitbot`. Tell them to ask a lead. Don't try to work around it.
- Don't have the code yet: `gh repo clone willfreyman/2026Kitbot`, then open that
  folder in WPILib VS Code.

## Starting new work

1. Look (Step 0).
2. Get the newest `main`:

   ```bash
   git switch main
   git pull --ff-only
   ```

   If they have uncommitted changes, skip `git switch main` and go straight to
   step 3. Creating the branch carries the changes along.

   If `git pull --ff-only` fails with "Not possible to fast-forward", someone
   committed directly to `main` on this laptop. See "I committed to main by
   accident" below.
3. Make a branch named `<firstname>/<short-topic>`, lowercase with dashes:

   ```bash
   git switch -c ruhan/shooter-pid-tuning
   ```

   Ask the student for their first name and what they're working on if you
   don't know. Good topics: `auto-shoot-and-backup`, `fix-intake-direction`,
   `pneumatic-unclog`. Bad: `stuff`, `test`, `branch2`.
4. Push it right away so it exists on GitHub:

   ```bash
   git push -u origin HEAD
   ```

## Saving work (committing)

Commit small and often: each time something works, or before trying something
risky.

1. Run `git status` and `git diff --cached` before staging anything. Explain
   what is already staged (selected for the next commit). If staged work belongs
   to another task or person, stop and ask; don't include or unstage it silently.
2. Watch for files that should **not** be committed, and leave them out:
   - anything in `build/`, `bin/`, `.gradle/`, or `*.class` (should already be
     ignored; if one appears, stop and tell them)
   - `.claude/settings.local.json` and stray `.claude/` folders inside `src/`
   - passwords, tokens, or `networktables.json` files
   - big files (videos, CAD exports, zip files)
   - files they didn't mean to touch. Ask about them.
3. Read `git diff` for unstaged changes and open any intended new files:
   untracked files do not appear in that diff. Summarize the changes in a sentence
   or two. Don't overwrite existing partial staging with a whole-file `git add`.
4. Stage only the intended files by name:

   ```bash
   git add src/main/java/frc/robot/Constants.java src/main/java/frc/robot/commands/Intake.java
   ```

5. Run `git diff --cached --check`, then read `git diff --cached`. This is the
   complete proposed commit, including anything staged before this task. Stop
   if it includes unrelated work, secrets, or unresolved conflict markers. Get
   the student's confirmation of what will be saved before committing.

   Write a real commit message: a short sentence about **what changed on the
   robot**, plus why if it isn't obvious. Draft one from the diff and let the
   student edit it.

   - Good: `Raise feeder RPM from 2500 to 3500 so shots leave faster`
   - Good: `Bind intake to left trigger and eject to left bumper`
   - Not allowed: `changed stuff`, `idk`, `fixes`, `asdf`, `final change`

   ```bash
   git commit -m "Raise feeder RPM from 2500 to 3500 so shots leave faster"
   ```

6. Push: `git push`. If it says "no upstream branch", run `git push -u origin HEAD`.

Before committing Java changes, check that the code compiles (`bash ./gradlew build`,
see the `build-deploy` skill). Never push code that doesn't build without telling
the student.

## Opening a pull request

When the branch does what it's supposed to:

1. Make sure everything is committed and pushed (`git status` should say
   "nothing to commit" and "up to date with origin/...").
2. Bring in the latest `main` first (next section) so conflicts show up now
   instead of at merge time.
3. Create the PR. The body must answer the questions in
   `.github/pull_request_template.md`. Draft answers from the commits and diff,
   then ask the student to confirm the "How was it tested?" part, because only
   they know.

   ```bash
   gh pr create --base main --title "Raise feeder RPM for faster shots" --body-file /tmp/pr-body.md
   ```

   Write the body to a temporary file first, so quotes and newlines survive.
4. Show them the PR link and explain that a teammate (or they themselves) can
   merge it with the `merge-pr` skill.

## Bringing main into your branch

Do this before opening a PR, when a PR shows a conflict, or when the branch is
more than a day or two old:

```bash
git status                       # must be clean; commit first if not
git fetch origin                 # download the newest info from GitHub
git merge origin/main            # mix the newest main into your branch
```

- "Already up to date": nothing to do.
- It merges cleanly: run `bash ./gradlew build`, then `git push`.
- **CONFLICT**: follow "Resolving conflicts" in the `merge-pr` skill. Don't guess.

Always use merge here, not rebase. Merge never requires a force-push.

## Undo and recovery

Before any fix, run `git status` and `git log --oneline --graph -15` and explain
what you see.

| Situation | What to do |
|---|---|
| Added a file by mistake (not committed yet) | `git restore --staged <file>` (the change stays, it's just not staged) |
| Want to throw away changes to one file | Destructive. Show `git diff <file>`, get a yes, then `git restore <file>` |
| Last commit has a typo in the message or is missing a file, **not pushed yet** | `git reset --soft HEAD~1` undoes the commit and keeps all the changes; then commit again |
| A pushed commit broke something | `git revert <commit>` makes a new commit that undoes it. Never rewrite pushed history |
| Started work on the wrong branch, not committed | `git switch -c <new-branch>` carries the changes over |
| "detached HEAD" | Usually from checking out a tag or commit. If they made changes, `git switch -c <name>` saves them; otherwise `git switch main` |
| Merge went badly and they want out | `git merge --abort` puts things back the way they were before the merge |
| Totally lost | Stop. Run `git status`, `git log --oneline --graph --all -20`, and explain the picture before changing anything |

### I committed to main by accident

This was the team's old habit, so it will happen.

1. `git status` and `git log --oneline origin/main..main` to show which commits
   are only on this laptop's `main`.
2. Save them on a branch: `git branch <firstname>/<topic>` (nothing is lost).
3. Confirm the branch has them: `git log --oneline -5 <firstname>/<topic>`.
4. Then put local `main` back to match GitHub. This uses `git reset --hard`, so
   explain it, make sure `git status` is clean, and get a clear yes:

   ```bash
   git switch main
   git reset --hard origin/main
   git switch <firstname>/<topic>
   git push -u origin HEAD
   ```

5. Open a PR from that branch.

If those commits were already **pushed** to `main`, don't rewrite anything. Tell
the student it's okay this time and tell a lead.
