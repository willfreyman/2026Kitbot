---
name: competition-day
description: >-
  Keep Team 10686's robot code safe around competitions. Covers the pre-event checklist
  (everything merged, build passes, last-known-good code tagged, offline build cached),
  making pit fixes with no internet, rolling back to known-good code fast, and getting
  event changes back into main afterward. Use when a competition, scrimmage, or event
  is coming up, someone is in the pits, a change broke the robot during an event, or
  someone asks to roll back, go back to the old code, or what code is on the robot.
---

# Competition day

At an event, the most important thing is **always knowing what code is on the
robot and being able to get back to code that worked.** The normal branch + PR
workflow still applies, but the pit laptop is usually on the robot radio with
no internet, so commits happen locally and PRs happen later.

## How to talk while doing this

Students are stressed and short on time. Be calm, short, and specific. Explain
each step in one sentence. If something is risky right before a match, say so
plainly and offer the safe option (usually: roll back to the last good tag).

## Before the event (do this with internet, a day or more ahead)

Go through this with the student and report each item as done or not:

1. **All wanted PRs merged.** `gh pr list --state open`. Anything still open
   either gets merged through `merge-pr` or is knowingly left out.
2. **Pit laptop on the latest main:**

   ```bash
   git status            # must be clean
   git switch main
   git pull --ff-only
   ```

3. **Builds online, then offline.** This fills the download cache, so building
   works at the venue without internet:

   ```bash
   bash ./gradlew build
   bash ./gradlew build --offline
   ```

   Do this on **every** laptop that might deploy.
4. **Deploy and drive-test** this exact commit on the robot (`build-deploy`).
5. **Tag it as known-good** once it works on the real robot. A tag is a
   permanent bookmark on a commit. Ask before pushing it:

   ```bash
   git tag -a good/<event>-<yyyy-mm-dd> -m "Drive-tested before <event>"
   git push origin good/<event>-<yyyy-mm-dd>
   ```

   Example: `good/week1-2026-03-06`.
6. **Pick the deploy laptop.** One laptop deploys at the event, so there's never
   a question about whose code is on the robot. Others can write code but hand
   it off through Git.
7. Write down: the tag name, the commit ID (`git rev-parse --short HEAD`), and
   which laptop is the deploy laptop.

## At the event: making a change in the pits

1. Start an event branch from the good tag (or from main, if it's the same):

   ```bash
   git switch -c event/<event> good/<event>-<yyyy-mm-dd>
   ```

   If the branch already exists from earlier that day: `git switch event/<event>`.
2. Make **one** change at a time: a constant, a binding, an auto tweak.
3. `bash ./gradlew build --offline`.
4. **Commit before deploying.** Always. The message says what and why:

   ```bash
   git add src/main/java/frc/robot/Constants.java
   git diff --cached --check
   git diff --cached
   # Review the staged changes as described in git-workflow before committing.
   git commit -m "Lower shooter RPM to -4500, shots going over the goal in Q12"
   ```

5. Follow the `build-deploy` approval checkpoint: confirm the intended robot
   and commit, disabled state, clear test area, and operator readiness. Only
   after explicit approval, run `bash ./gradlew deploy --offline`. The operator
   decides when it is safe to enable and test.
6. If it's worse, roll back (next section). Don't stack more untested changes
   on top right before a match.

Never deploy uncommitted code at an event. If someone asks, commit it first
(even as `Testing: ...`). It takes 10 seconds and means you can always get back.

## Rolling back to known-good code (fast)

When the robot misbehaves after a change and a match is coming:

```bash
git status                        # commit anything first (keeps it, nothing lost)
git tag --list 'good/*'           # see the known-good tags
git switch --detach good/<event>-<yyyy-mm-dd>
bash ./gradlew build --offline
```

Explain: "This prepares the code that worked before. Your newer changes are
still saved on your branch; the robot has not changed yet."

Stop at the `build-deploy` approval checkpoint, even when a match is close.
Confirm the target robot, selected commit, disabled state, clear test area, and
operator readiness. Only after explicit approval, run `bash ./gradlew deploy
--offline` and report whether it succeeded.

To keep working afterwards: `git switch event/<event>`.

Rolling back a single bad commit instead (keeps the other changes):
`git revert <commit>` on the event branch, then build and deploy.

If a change works well in matches, it's fine to add a new tag to that commit,
like `good/<event>-<yyyy-mm-dd>-after-q20` (push it once there's internet).

## What code is on the robot?

- On the deploy laptop: `git log --oneline -1` and `git status`. If the last
  deploy came after the last commit with a clean status, that's what's on the
  robot.
- If unsure, say the running version is unknown. Do not deploy just to find
  out. Offer an explicitly approved deployment of a known commit through the
  `build-deploy` checkpoint when the team is ready.
- The deployed jar on the roboRIO contains a `backup/src` copy of the code it
  was built from, as a last resort.

## After the event (with internet)

Event changes must get back into `main`, or they're lost next season:

```bash
git switch event/<event>
git push -u origin HEAD
```

Then open a PR (`git-workflow`) titled like `Changes from <event>`. The
description lists each tuning change and why (the commit messages help), and
says "Tested on the robot at <event>". Merge it with `merge-pr`, which will
catch conflicts if main moved during the event.
