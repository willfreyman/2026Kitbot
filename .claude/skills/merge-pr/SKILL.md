---
name: merge-pr
description: >-
  Safely merge a pull request into main for Team 10686's robot code. Runs three
  blocking checks first (a real PR description, no merge conflicts with main, and
  bash ./gradlew build passing on the merged result), and walks students through resolving
  conflicts when there are any. Use whenever someone wants to merge a PR, asks "can
  this PR go in", "is my PR ready", or a PR shows a merge conflict.
---

# Merging a pull request

Anyone on the team may merge any PR, including their own, but **only after all
three checks pass.** If any check fails, the merge is blocked. Explain why in
plain words and help fix it. Don't merge anyway, even if someone says it's urgent
or "it's just a small change".

The three checks:

1. **Description**: the PR says what changed, why, and how it was tested.
2. **No conflicts**: the branch merges cleanly with the latest `main`.
3. **Build passes**: `bash ./gradlew build` succeeds on the branch *merged with* the
   latest `main`, not just on the branch by itself.

## How to talk while doing this

The person is a student who is new to Git. Before each step, say what you're
checking and why it matters for the robot. Example: "Now I'll check that this
code still compiles when combined with everyone else's newest changes. If it
doesn't, the robot won't get any code at all." Report each check as passed or
blocked with a one-line reason.

## Hard rules

- Never `gh pr merge --admin`, never push to `main`, never force-push.
- Never merge when a check is failing, skipped, or couldn't run (for example, no
  internet or the build can't download dependencies). "Couldn't check" counts as
  blocked.
- Never change GitHub settings or branch protection.
- Don't touch the student's own working folder for the checks. Use the temporary
  check folder described below so their unsaved work is safe.

## Step 1: find the PR

```bash
gh auth status
gh pr list --state open
gh pr view <N> --json number,title,body,state,isDraft,baseRefName,headRefName,headRefOid,author,mergeable,mergeStateStatus,url
```

Block, with an explanation, if:
- `state` isn't `OPEN`
- `isDraft` is true (the author marked it not ready; ask them)
- `baseRefName` isn't `main`

If `gh` is missing or not logged in, send them to "First-time setup" in the
`git-workflow` skill.

## Step 2: check the description

Read the title and body. **Block** if any of these are true:
- The body is empty, or the template from `.github/pull_request_template.md` is
  still unfilled (only the `<!-- -->` comments).
- It doesn't say what changed in a way a teammate would understand.
- It doesn't say how it was tested (real robot, simulation, or "not tested" with
  a reason). "Not tested" is allowed, but it has to be said out loud.
- The title is junk (`changed stuff`, `idk`, `update`, `asdf`). The title becomes
  the commit message on `main`, so it matters.

Also check the diff (`gh pr diff <N>`). If it changes button bindings, motor/CAN
IDs, current limits, speeds, or RPMs in `Constants.java`, the description must
list them under "Controls or constants changed?".

To fix it, draft a better title and body from the diff, show the student, ask
them to fill in testing, then:

```bash
gh pr edit <N> --title "Better title" --body-file /tmp/pr-<N>-body.md
```

## Steps 3–5: check conflicts, build, and merge

Run the following as one Bash block, after Steps 1–2 pass. Replace `123` with the
PR number. Leave `MERGE_REQUESTED=no` for a readiness check; set it to `yes` only
when the student explicitly requested the merge. A later merge requires running
these checks again. Do not assume shell variables survive separate tool calls.

The unique temporary worktree keeps the student's files untouched. Every setup,
merge, build, and refresh failure blocks the merge. Cleanup can remove only the
worktree successfully created by this run; a partial setup is left for inspection.

```bash
(
  set -euo pipefail
  PR=123
  MERGE_REQUESTED=no
  REPO=$(git rev-parse --show-toplevel)
  cd "$REPO"
  git fetch origin
  MAIN_SHA=$(git rev-parse origin/main)
  HEAD_SHA=$(gh pr view "$PR" --json headRefOid -q .headRefOid)
  test -n "$HEAD_SHA"
  git fetch origin "pull/$PR/head"
  test "$(git rev-parse FETCH_HEAD)" = "$HEAD_SHA"

  CHECK_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/kitbot-pr-${PR}.XXXXXX")
  CHECK_DIR="$CHECK_ROOT/worktree"
  CREATED=no
  cleanup() {
    result=$?
    trap - EXIT
    if [ "$CREATED" = yes ]; then
      if ! git -C "$REPO" worktree remove --force "$CHECK_DIR"; then
        echo "Cleanup failed; leave $CHECK_DIR for inspection." >&2
        result=1
      fi
    fi
    # Never recursively delete a failed or partially created check folder.
    if ! rmdir "$CHECK_ROOT"; then
      echo "Check folder retained at $CHECK_ROOT." >&2
      result=1
    fi
    exit "$result"
  }
  trap cleanup EXIT
  git worktree add --detach "$CHECK_DIR" "$HEAD_SHA"
  CREATED=yes
  if ! git -C "$CHECK_DIR" merge --no-commit --no-ff "$MAIN_SHA"; then
    git -C "$CHECK_DIR" diff --name-only --diff-filter=U
    echo "Blocked: merge check failed. No PR merge attempted." >&2
    exit 1
  fi
  (cd "$CHECK_DIR" && bash ./gradlew build)

  git fetch origin
  CURRENT_MAIN=$(git rev-parse origin/main)
  CURRENT_HEAD=$(gh pr view "$PR" --json headRefOid -q .headRefOid)
  if [ "$CURRENT_MAIN" != "$MAIN_SHA" ] || [ "$CURRENT_HEAD" != "$HEAD_SHA" ]; then
    echo "Blocked: main or the PR changed. Run the checks again." >&2
    exit 1
  fi
  STATE=$(gh pr view "$PR" --json state -q .state)
  DRAFT=$(gh pr view "$PR" --json isDraft -q .isDraft)
  BASE=$(gh pr view "$PR" --json baseRefName -q .baseRefName)
  MERGEABLE=$(gh pr view "$PR" --json mergeable -q .mergeable)
  if [ "$STATE" != OPEN ] || [ "$DRAFT" != false ] || [ "$BASE" != main ] || [ "$MERGEABLE" != MERGEABLE ]; then
    echo "Blocked: PR is not confirmed ready. Refresh and re-check." >&2
    exit 1
  fi
  echo "Local checks passed: main=$MAIN_SHA PR=$HEAD_SHA"
  if [ "$MERGE_REQUESTED" = yes ]; then
    gh pr merge "$PR" --squash --match-head-commit "$HEAD_SHA"
    # Do not report a queued merge as completed.
    gh pr view "$PR" --json state,mergedAt,url
  fi
)
```

A conflict means blocked: explain the listed files and follow "Resolving
conflicts" below. Other merge errors also block; don't mistake them for success.
A nonzero build exit means blocked, even if earlier output says something passed.
Show the first build error and use `build-deploy` to troubleshoot. GitHub's
`UNKNOWN` mergeability means wait and retry, not permission to merge.

**Limit of local checks:** `--match-head-commit` protects only the PR head.
`main` can still change between the last fetch and the server-side merge. Do not
claim these commands guarantee a build of the final merged result. Reliable
server-side enforcement requires required build checks with an up-to-date-branch
rule, or a correctly configured merge queue. Propose that as separate work; never
change GitHub settings as part of this skill. Until then, report that limitation
and ask teammates not to merge concurrently.

Only report success once GitHub reports `MERGED`. A pending or failed merge is
not complete. Show the PR link. Keep branch cleanup separate so merging doesn't
switch or delete anything in the student's working folder. Remind them that
pulling the new main requires checking for unsaved work first, and deploying is
a separate, explicitly approved step.

## Resolving conflicts

A conflict means two people changed the same lines differently, and Git needs a
human to pick. That's normal, not a disaster.

Ideally the **PR author** does this, because they know what their change was
for. If someone else is merging, they can do it with the author, or tell the
author it's blocked and why.

On the laptop of whoever is resolving:

1. Save their current work first. `git status` must be clean; if it isn't, commit
   it on their current branch (see `git-workflow`). Don't stash without asking.
2. Get onto the PR's branch:

   ```bash
   gh pr checkout <N>
   git pull --no-rebase
   git fetch origin
   git merge origin/main
   ```

3. List the conflicted files: `git diff --name-only --diff-filter=U`.
4. For each file, open it and find the blocks:

   ```text
   <<<<<<< HEAD
   (this branch's version)
   =======
   (main's version)
   >>>>>>> origin/main
   ```

   For each block, explain to the student in plain words what each side does
   (for example: "Your branch sets the feeder to 3500 RPM; main sets it to 3000
   and adds a slow-shoot speed.") and then:
   - If one side clearly includes the other, or they're unrelated additions (two
     new imports, two new commands), combine them and say what you did.
   - If it's a **robot behavior choice** (different speeds, RPMs, PID values,
     current limits, button bindings, auto routines), **ask the student which
     one is correct.** Don't pick for them. If they don't know, they should ask
     whoever made the other change.
   - Remove every `<<<<<<<`, `=======`, `>>>>>>>` line.
5. Check nothing is left: `git diff --check` and
   `grep -rn '^<<<<<<<\|^>>>>>>>' src/` should print nothing.
6. Make sure it builds: `bash ./gradlew build`. Fix any compile errors (often a
   duplicate import, or a method that was renamed on main).
7. Finish the merge:

   ```bash
   git add <each resolved file>
   git diff --cached --check
   git diff --cached
   # Review all staged changes before continuing; see git-workflow.
   git commit --no-edit
   git push
   ```

8. If it gets confusing, `git merge --abort` puts everything back the way it was
   before step 2's merge. Then start again or get help.
9. Start this skill again from Step 1. The checks have to pass on the new version.
