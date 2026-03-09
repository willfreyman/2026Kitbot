package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.LimelightHelpers;
import frc.robot.subsystems.CANDriveSubsystem;
import edu.wpi.first.wpilibj.DriverStation;
import static frc.robot.Constants.OperatorConstants.*;

/**
 * Drives toward detected fuel (yellow balls) using Limelight color pipeline.
 * Requires Pipeline 1 configured as Color/Retro for yellow in the Limelight web UI.
 *
 * Uses tx to steer toward the ball and ta (target area) to control forward speed.
 * Driver can override rotation with right stick, or add forward/back with left stick.
 */
public class AutoCollect extends Command {
  private final CANDriveSubsystem driveSubsystem;
  private final CommandXboxController driverController;
  private final String limelightName = "limelight-first";

  // Pipeline indices
  private static final int FUEL_PIPELINE = 1;
  private static final int DEFAULT_PIPELINE = 0;

  // PD steering control (matches AutoTarget)
  private static final double kP = 0.035;
  private static final double kD = 0.004;
  private static final double kSteerTolerance = 1.5;
  private static final double kMaxSteer = 0.5;
  private static final double kSmoothing = 0.3;

  // Forward drive control (based on target area)
  private static final double DRIVE_kP = 0.15;
  private static final double DRIVE_MAX = 0.4;
  private static final double DESIRED_AREA = 10.0;
  private static final double MIN_AREA = 0.5;

  // Driver override
  private static final double JOYSTICK_DEADBAND = 0.1;
  private boolean driverOverride = false;
  private double lastError = 0;
  private double smoothedOutput = 0;

  public AutoCollect(CANDriveSubsystem driveSubsystem, CommandXboxController driverController) {
    this.driveSubsystem = driveSubsystem;
    this.driverController = driverController;
    addRequirements(driveSubsystem);
  }

  @Override
  public void initialize() {
    driverOverride = false;
    lastError = 0;
    smoothedOutput = 0;
    LimelightHelpers.setPipelineIndex(limelightName, FUEL_PIPELINE);
    LimelightHelpers.setLEDMode_ForceOff(limelightName);
    DriverStation.reportWarning("AutoCollect: Searching for fuel...", false);
  }

  @Override
  public void execute() {
    double driverForward = -driverController.getLeftY() * DRIVE_SCALING;
    double driverRotation = -driverController.getRightX() * ROTATION_SCALING;
    boolean driverWantsRotation = Math.abs(driverRotation) > JOYSTICK_DEADBAND;
    boolean driverWantsMovement = Math.abs(driverForward) > JOYSTICK_DEADBAND;

    // Full override if driver is rotating
    if (driverWantsRotation) {
      driveSubsystem.driveArcade(driverForward, driverRotation);
      if (!driverOverride) {
        DriverStation.reportWarning("AutoCollect: Driver override", false);
        driverOverride = true;
      }
      return;
    } else if (driverOverride) {
      driverOverride = false;
    }

    boolean hasTarget = LimelightHelpers.getTV(limelightName);
    double tx = LimelightHelpers.getTX(limelightName);
    double ta = LimelightHelpers.getTA(limelightName);

    if (!hasTarget || ta < MIN_AREA) {
      // No fuel visible — let driver control
      driveSubsystem.driveArcade(driverWantsMovement ? driverForward : 0, 0);
      return;
    }

    // PD steering with smoothing (same logic as AutoTarget)
    double steerCommand;
    if (Math.abs(tx) <= kSteerTolerance) {
      steerCommand = 0;
      smoothedOutput = 0;
      lastError = tx;
    } else {
      double derivative = tx - lastError;
      steerCommand = (tx * kP) + (derivative * kD);
      lastError = tx;
      steerCommand = Math.max(-kMaxSteer, Math.min(kMaxSteer, steerCommand));
      smoothedOutput = (kSmoothing * steerCommand) + ((1.0 - kSmoothing) * smoothedOutput);
      steerCommand = smoothedOutput;
    }

    // Forward: proportional to distance (small area = far = drive faster)
    double driveCommand = (DESIRED_AREA - ta) * DRIVE_kP;
    driveCommand = Math.max(0, Math.min(driveCommand, DRIVE_MAX));

    // Blend with driver forward input if they want to override speed
    double finalForward = driverWantsMovement ? driverForward : driveCommand;

    driveSubsystem.driveArcade(finalForward, -steerCommand);

    if (Math.abs(tx) < 2.0) {
      DriverStation.reportWarning("AutoCollect: Locked on fuel, area=" + String.format("%.1f", ta), false);
    }
  }

  @Override
  public void end(boolean interrupted) {
    driveSubsystem.driveArcade(0, 0);
    LimelightHelpers.setPipelineIndex(limelightName, DEFAULT_PIPELINE);
    LimelightHelpers.setLEDMode_PipelineControl(limelightName);
    DriverStation.reportWarning("AutoCollect: Ended", false);
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
