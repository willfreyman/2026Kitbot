---
name: robot-code
description: >-
  How Team 10686's 2026 Kitbot Java code is organized and the conventions to follow
  when changing it. Covers subsystems, commands, autos, Constants, controller bindings
  in RobotContainer, REVLib SparkFlex setup, and the safety-sensitive values (CAN IDs,
  current limits, pneumatic pressure) that need a human's OK. Use whenever writing or
  changing robot code, adding a command, auto, subsystem, or button binding, tuning
  speeds/RPM/PID, or when someone asks how the code works or where something lives.
---

# Robot code conventions

WPILib 2026 **command-based** Java, using REVLib (SPARK Flex) motors, a REV
Pneumatic Hub, and a Limelight. Before changing a file, read it and any file it
calls. Follow the patterns already in the code rather than introducing new ones.

## How to talk while doing this

Students are learning Java and robotics at the same time. When you change code:
- Say which file you're changing and why, in robot terms ("the intake spins at
  this speed").
- After the change, explain what the robot will do differently and how to test
  it safely.
- Keep changes small: one behavior per change, so it's easy to test and to undo.
- Teach the pattern as you go ("Commands live in `commands/`, and buttons get
  hooked up in `RobotContainer`").

## Where things live

```text
src/main/java/frc/robot/
  Main.java            entry point (don't edit)
  Robot.java           robot lifecycle; keep logic out of here
  RobotContainer.java  creates subsystems, binds buttons, builds the auto chooser
  Constants.java       ALL numbers: IDs, speeds, RPMs, voltages, times, limits
  LimelightHelpers.java vendor helper library (don't edit)
  subsystems/          one class per mechanism; owns its motors/hardware
    CANDriveSubsystem    drivetrain
    CANFuelSubsystem     intake, feeder, two shooter rollers (SPARK Flex, RPM control)
    PneumaticSubsystem   compressor, hopper solenoid, pressure sensor
    OLEDPongSubsystem    fun OLED display (can be disabled in RobotContainer)
  commands/            one class per action (Intake, Eject, LaunchSequence, Shake, ...)
    auto/              autonomous routines (ShootAndBackup, TurnAndShoot, ...)
src/main/deploy/       files copied to the roboRIO
vendordeps/            vendor library versions (team decision to change)
```

Extra docs: `LIMELIGHT_README.md` (vision), `OLED_PONG_README.md`.

## Conventions

### Constants
- Every tunable number goes in `Constants.java`, in the inner class for its
  mechanism (`DriveConstants`, `FuelConstants`, `OperatorConstants`,
  `PneumaticConstants`, `ShakeConstants`). Add a new inner class for a new
  mechanism.
- Names are `UPPER_SNAKE_CASE`, and `public static final`.
- Add a short comment saying what the value does and its units (RPM, volts,
  seconds, amps, psi).
- Import with `import static frc.robot.Constants.FuelConstants.NAME;`, as the
  existing files do.
- Sign matters: several fuel RPMs are negative because of motor direction.
  Never flip a sign without asking which way the mechanism should spin.

### Subsystems
- `extends SubsystemBase`, live in `subsystems/`, create and configure their own
  hardware in the constructor.
- REVLib 2026 pattern (see `CANFuelSubsystem`): build a `SparkFlexConfig`, set
  `smartCurrentLimit`, PID via `config.closedLoop.p(..).i(..).d(..)`, then
  `motor.configure(config, kResetSafeParameters, kPersistParameters)`.
- Expose simple methods (`setIntakeRollerRPM`, `stop`) or command factories
  (`PneumaticSubsystem.holdExtend()`), not raw motor objects.
- Create each subsystem once, in `RobotContainer`, and pass it into commands.

### Commands
- One class per command in `commands/` that `extends Command`, calls
  `addRequirements(subsystem)` in the constructor, and fills in `initialize`,
  `execute`, `end`, and `isFinished` with a comment on each (see `Intake.java`).
- `end()` must stop any motors the command started.
- Small one-liners can use subsystem command factories instead (`run`,
  `runOnce`), like `PneumaticSubsystem` does. Match whatever the nearby code
  does.

### Autos
- Put them in `commands/auto/` as a `SequentialCommandGroup`, with a Javadoc
  saying what the auto does and when to use it (see `ShootAndBackup.java`).
- Put a `.withTimeout(...)` on anything that could run forever.
- Register each new auto in `RobotContainer` with
  `autoChooser.addOption("Readable Name", new MyAuto(...))`. Only change
  `setDefaultOption` if the student asks.

### Button bindings (`RobotContainer.configureBindings`)
- Use `CommandXboxController` triggers: `whileTrue` for hold-to-run, `onTrue`
  for press-once, `toggleOnTrue` for press-on/press-off.
- Put a comment above each binding naming the **actual** button. Several
  existing comments are out of date (for example, "left bumper" above a
  left-trigger binding), so fix the comment when you touch a binding.
- **Check for button clashes before adding a binding.** Right now
  `DRIVER_CONTROLLER_PORT` and `OPERATOR_CONTROLLER_PORT` are **both 0**, so
  "driver" and "operator" bindings are on the same physical controller. List
  every binding on both objects and make sure the new button isn't already used.
  If the ports ever change, update this note.
- When bindings change, the PR description must list them, since drivers need
  to know.

## Ask a human before changing these

These can break hardware, trip breakers, or make the robot unsafe. Explain the
risk and get a clear yes:

- **CAN IDs and channels** (`*_ID`, `HUB_CAN_ID`, `*_CHANNEL`): must match the
  physical wiring.
- **Current limits** (`*_CURRENT_LIMIT`): raising them can trip breakers or burn
  motors.
- **Pneumatic pressures** (`MIN_PRESSURE_PSI`, `MAX_PRESSURE_PSI`): 120 psi is
  the legal max. Never go above it.
- **Motor inversion** (`.inverted(...)`) and RPM signs.
- `vendordeps/*.json`, `build.gradle`, `.wpilib/wpilib_preferences.json` (team
  number).
- Removing a safety timeout or `isFinished` condition in an auto.

## After changing code

1. `bash ./gradlew build` must pass (`build-deploy`).
2. Explain how to test it on the robot safely, or in sim (`bash ./gradlew simulateJava`).
3. Commit with a message saying what changed on the robot (`git-workflow`).
