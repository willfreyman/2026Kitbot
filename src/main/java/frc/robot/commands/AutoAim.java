// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.LimelightHelpers;
import frc.robot.subsystems.CANDriveSubsystem;
import edu.wpi.first.wpilibj.DriverStation;

/**
 * Auto-aim command for retroreflective targets (non-AprilTag).
 * Uses Limelight vision to automatically aim at bright retroreflective targets.
 * Hold the button to continuously track the target.
 */
public class AutoAim extends Command {
  private final CANDriveSubsystem driveSubsystem;
  private final String limelightName = "limelight-first"; // Default Limelight name

  // PID constants for aiming (tune these for your robot)
  private static final double kP = 0.03; // Proportional gain for rotation
  private static final double kMinCommand = 0.05; // Minimum rotation speed to overcome friction
  private static final double kMaxCommand = 0.5; // Maximum rotation speed for safety
  private static final double kTolerance = 1.0; // Degrees of acceptable error

  // LED control
  private boolean ledWasOn = false;

  public AutoAim(CANDriveSubsystem driveSubsystem) {
    this.driveSubsystem = driveSubsystem;
    addRequirements(driveSubsystem);
  }

  @Override
  public void initialize() {
    // Turn on Limelight LEDs for better tracking
    LimelightHelpers.setLEDMode_ForceOn(limelightName);
    ledWasOn = true;

    // Set pipeline to 0 (or whatever pipeline you use for targeting)
    LimelightHelpers.setPipelineIndex(limelightName, 0);

    DriverStation.reportWarning("Auto-aim started", false);
  }

  @Override
  public void execute() {
    // Check if we have a valid target
    boolean hasTarget = LimelightHelpers.getTV(limelightName);

    if (hasTarget) {
      // Get the horizontal offset from the crosshair in degrees
      double tx = LimelightHelpers.getTX(limelightName);

      // Calculate rotation speed using proportional control
      double rotationSpeed = tx * kP;

      // Apply minimum command to overcome friction (deadband)
      if (Math.abs(rotationSpeed) > 0.001 && Math.abs(rotationSpeed) < kMinCommand) {
        rotationSpeed = Math.signum(rotationSpeed) * kMinCommand;
      }

      // Clamp the rotation speed for safety
      rotationSpeed = Math.max(-kMaxCommand, Math.min(kMaxCommand, rotationSpeed));

      // Drive with no forward/backward movement, only rotation
      // Negative because positive tx means target is to the right,
      // and we need to rotate clockwise (negative) to aim at it
      driveSubsystem.driveArcade(0, -rotationSpeed);

      // Optional: Report tracking status
      if (Math.abs(tx) < kTolerance) {
        // We're on target!
        DriverStation.reportWarning("Target locked! Offset: " + String.format("%.1f", tx) + "°", false);
      }
    } else {
      // No target found - stop rotating but you could also implement a search pattern
      driveSubsystem.driveArcade(0, 0);
      DriverStation.reportWarning("No target found", false);
    }
  }

  @Override
  public void end(boolean interrupted) {
    // Stop the robot
    driveSubsystem.driveArcade(0, 0);

    // Return LEDs to pipeline control
    LimelightHelpers.setLEDMode_PipelineControl(limelightName);

    DriverStation.reportWarning("Auto-aim ended", false);
  }

  @Override
  public boolean isFinished() {
    // This command runs until interrupted (when button is released)
    return false;
  }

  /**
   * Optional method to check if we're currently on target
   * Useful for other commands that might want to know if we're aimed
   */
  public boolean isOnTarget() {
    boolean hasTarget = LimelightHelpers.getTV(limelightName);
    if (!hasTarget) return false;

    double tx = LimelightHelpers.getTX(limelightName);
    return Math.abs(tx) < kTolerance;
  }
}