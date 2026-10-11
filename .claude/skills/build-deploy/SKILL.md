---
name: build-deploy
description: >-
  Build, simulate, and deploy Team 10686's WPILib 2026 Java robot code to the roboRIO,
  and fix the common errors (Java/JAVA_HOME not found, compile errors, vendordep
  downloads failing offline, can't find the roboRIO, "No Robot Code" after deploy).
  Use whenever someone wants to build, compile, check the code, deploy, push code to
  the robot, run the simulator, or reports that the build or deploy failed.
---

# Build and deploy

Project facts:
- WPILib **2026** (GradleRIO 2026.2.1), Java 17, command-based.
- Team number **10686**, read from `.wpilib/wpilib_preferences.json`. Don't
  change it.
- Vendor libraries: REVLib 2026 (`vendordeps/REVLib.json`) and WPILib New
  Commands.
- roboRIO address: `roborio-10686-frc.local`, or `10.106.86.2` on the robot
  radio, or `172.22.11.2` over USB.

## How to talk while doing this

The student may never have used a terminal. Say what each command does before
running it ("`build` checks the code compiles, without touching the robot";
"`deploy` builds and then copies the code onto the robot"). When something fails,
find the **first** real error, quote the file and line, and explain it in one or
two sentences before fixing it. Don't paste a wall of Gradle output.

## Commands

Run from the repo root. In Bash (Linux, macOS, or Windows Git Bash), use
`bash ./gradlew`: the wrapper isn't marked executable in this repo, and Bash
can run it without changing permissions. In PowerShell or cmd, use
`.\gradlew.bat` with the same task, for example `.\gradlew.bat build`.

A laptop that has never built this code may still need WPILib and dependency
downloads. A wrapper command that starts successfully is not proof of a passing
robot build.

| Goal | Command |
|---|---|
| Check the code compiles | `bash ./gradlew build` |
| Same, with no internet (competition) | `bash ./gradlew build --offline` |
| Deploy to the robot | `bash ./gradlew deploy` |
| Deploy with no internet | `bash ./gradlew deploy --offline` |
| Run the simulator (opens a GUI) | `bash ./gradlew simulateJava` |

The WPILib VS Code buttons (**WPILib: Build Robot Code**, **WPILib: Deploy Robot
Code**, **WPILib: Simulate Robot Code**) run the same tasks. Students can use
either.

## Before every deploy

Deploying changes what a real, heavy, fast machine does. Every time:

1. **Commit first.** `git status` should be clean, so the team knows exactly
   which code is on the robot. If there are changes, commit them, even with a
   message like `Try shooter RPM -5500 for testing` (see `git-workflow`). Tell
   the student the commit's short ID (`git rev-parse --short HEAD`) and branch so
   they can note it.
2. Build first: `bash ./gradlew build` must pass (use `--offline` at an event).
3. Connect to the intended robot: robot radio Wi-Fi, Ethernet to the radio, or
   USB to the roboRIO. Confirm its identity; don't rely only on an address.
4. **Stop for explicit approval before every deploy, including rollback.** Name
   the robot and commit. Ask the person at the robot to confirm it is disabled,
   people are clear of moving parts, it is safely supported or in an appropriate
   test area, and an operator is at the Driver Station ready to disable/e-stop.
   A request to build or troubleshoot is not permission to deploy.
5. Only after confirmation, run `bash ./gradlew deploy` (or add `--offline`).
   Do not enable the robot remotely. Report whether deployment actually succeeded.

The deployed jar includes a copy of `src/` (`backup/src` inside the jar), so the
code on the robot can be recovered if a laptop is lost.

## Fixing common problems

### Java not found / JAVA_HOME errors

The WPILib installer ships its own Java 17. Point Gradle at it for this terminal:

- Windows (Git Bash): `export JAVA_HOME="/c/Users/Public/wpilib/2026/jdk"`
- Mac/Linux: `export JAVA_HOME="$HOME/wpilib/2026/jdk"`

If that folder doesn't exist, the WPILib 2026 installer isn't installed on this
laptop. Send them to the WPILib install docs; don't install some other JDK.

### Compile errors

Read the first `error:` line: `File.java:LINE: error: message`. Common ones in
this code:
- `cannot find symbol`: a typo, a missing `import`, or a constant that was
  renamed or deleted in `Constants.java`.
- `incompatible types`: for example, passing a `double` RPM where a `Command` is
  expected.
- `';' expected` / `class, interface, enum, or record expected`: a missing
  semicolon or an extra or missing `}`.
- Leftover `<<<<<<<` / `=======` / `>>>>>>>` lines from a merge: see
  "Resolving conflicts" in `merge-pr`.

Fix the first error, rebuild, and repeat. Later errors are often caused by the
first one.

### "Could not resolve" / download errors

Gradle needs internet the first time to download WPILib and REVLib pieces. After
one successful online build they're cached, and `--offline` works.

- Online: run `bash ./gradlew build` again; it's often just a flaky network.
- At competition with no internet: use `--offline`. If that fails too, this
  laptop has never built this version online, so use a laptop that has.
- Don't edit `vendordeps/*.json` or the GradleRIO version in `build.gradle` to
  "fix" this. Changing versions is a team decision and needs matching roboRIO
  and REV firmware.

### Can't find the roboRIO (deploy fails to connect)

1. Is the robot on, and has it finished booting (about 30–60 s)?
2. Is the laptop on the robot's network? Check the Wi-Fi name, or plug in USB.
3. `ping roborio-10686-frc.local`, then `ping 10.106.86.2` (radio) or
   `ping 172.22.11.2` (USB).
4. Turn off other network adapters or a VPN, and allow Java through the firewall
   on Windows.
5. Try USB directly to the roboRIO. It's the most reliable connection.

### Deploy succeeded but Driver Station says "No Robot Code" or code crashes

Open **WPILib: Start RioLog** in VS Code (or look at the Driver Station console)
and find the first exception (`Exception in thread`, `Unhandled exception`).
Common causes:
- A CAN ID in `Constants.java` doesn't match a real device (REV errors about a
  SPARK not found). Check the IDs with REV Hardware Client.
- A null object: something used before it was created in `RobotContainer`.
- REVLib firmware may be incompatible. Check versions first. Firmware changes
  require separate explicit approval and a qualified team member present; don't
  update devices automatically while troubleshooting.

## Simulation

`bash ./gradlew simulateJava` opens the sim GUI. Good for checking that commands are
bound to the right buttons and autos run in the right order. Motor behavior in sim
is not realistic, so a PR that was only simulated should say "sim-only" under
"How was it tested?".
