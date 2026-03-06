// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.LimelightHelpers;
import frc.robot.subsystems.CANDriveSubsystem;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import static frc.robot.Constants.OperatorConstants.*;

/**
 * Auto-targeting system for game targets.
 * Automatically configures target IDs based on alliance color:
 * - BLUE ALLIANCE: Helper=25, Primary=26 (targeting red side)
 * - RED ALLIANCE: Helper=9, Primary=10 (targeting blue side)
 * - UNKNOWN: Falls back to blue alliance settings
 *
 * CONTROL MODES (while X is held):
 * - AUTO-AIM ONLY: No joystick input → Auto-rotates to center target
 * - MIXED MODE: Left stick (forward/back) → Driver controls movement, auto-aim controls rotation
 * - FULL OVERRIDE: Right stick (rotation) → Complete manual control, auto-aim pauses
 * - AUTO-AIM RESUMES: When right stick returns to neutral (X still held)
 */
public class AutoTarget extends Command {
  private final CANDriveSubsystem driveSubsystem;
  private final CommandXboxController driverController;
  private final String limelightName = "limelight-first";
  private boolean wasScheduled = false;

  // Alliance-based target configuration
  // BLUE ALLIANCE (targeting red side)
  private static final int BLUE_PRIMARY_ID = 26;  // Blue targets red side
  private static final int BLUE_HELPER_ID = 25;   // Blue helper tag

  // RED ALLIANCE (targeting blue side)
  private static final int RED_PRIMARY_ID = 10;   // Red targets blue side
  private static final int RED_HELPER_ID = 9;     // Red helper tag

  // Current targets (set based on alliance in initialize())
  private int PRIMARY_TARGET_ID = BLUE_PRIMARY_ID;
  private int HELPER_TAG_ID = BLUE_HELPER_ID;

  // Helper tag strategy:
  // - Helper is positioned to the LEFT of the primary target
  // - If primary is visible: Always center on primary, ignore helper
  // - If ONLY helper is visible: Turn towards helper to find primary
  // - This guides robot towards target area when primary is out of view
  // - Since helper is LEFT of primary, turning towards helper will eventually reveal primary

  // Control parameters
  private static final double kP = 0.045; // Proportional gain (increased for better fine control) // was 0.035
  private static final double kMinCommand = 0.08; // Minimum rotation speed (reduced for finer control) // was 0.125
  private static final double kMaxCommand = 8; // Maximum rotation speed
  private static final double kTolerance = 1.5; // Degrees of acceptable error (tighter tolerance) // was 4

  // Driver override detection
  private static final double JOYSTICK_DEADBAND = 0.1; // Joystick deadband for override detection

  // Debug tracking
  private int executeCount = 0;
  private long startTime = 0;
  private double lastError = 0; // For derivative calculation (optional future enhancement)
  private boolean driverOverride = false;

  public AutoTarget(CANDriveSubsystem driveSubsystem, CommandXboxController driverController) {
    this.driveSubsystem = driveSubsystem;
    this.driverController = driverController;
    addRequirements(driveSubsystem);
  }

  @Override
  public void initialize() {
    // Reset debug counters
    executeCount = 0;
    startTime = System.currentTimeMillis();
    wasScheduled = true;

    DriverStation.reportWarning("=== AUTO-TARGET INITIALIZE START ===", false);

    // AUTOMATIC ALLIANCE DETECTION
    var alliance = DriverStation.getAlliance();
    if (alliance.isPresent()) {
      if (alliance.get() == Alliance.Blue) {
        PRIMARY_TARGET_ID = BLUE_PRIMARY_ID;
        HELPER_TAG_ID = BLUE_HELPER_ID;
        DriverStation.reportWarning("BLUE ALLIANCE detected - Targeting red side", false);
      } else if (alliance.get() == Alliance.Red) {
        PRIMARY_TARGET_ID = RED_PRIMARY_ID;
        HELPER_TAG_ID = RED_HELPER_ID;
        DriverStation.reportWarning("RED ALLIANCE detected - Targeting blue side", false);
      }
    } else {
      // Unknown alliance (practice mode) - default to blue
      PRIMARY_TARGET_ID = BLUE_PRIMARY_ID;
      HELPER_TAG_ID = BLUE_HELPER_ID;
      DriverStation.reportWarning("UNKNOWN ALLIANCE - Defaulting to blue alliance targets", false);
    }

    // Configure Limelight
    LimelightHelpers.setLEDMode_ForceOn(limelightName);
    LimelightHelpers.setPipelineIndex(limelightName, 0); // AprilTag pipeline

    // Set priority to selected target ID
    LimelightHelpers.setPriorityTagID(limelightName, PRIMARY_TARGET_ID);

    DriverStation.reportWarning("Auto-targeting configured - Primary: ID " + PRIMARY_TARGET_ID, false);
    if (HELPER_TAG_ID > 0) {
      DriverStation.reportWarning("  Helper: ID " + HELPER_TAG_ID + " (left side guide)", false);
    }
    DriverStation.reportWarning("  Driver can override with joysticks at any time", false);
  }

  @Override
  public void execute() {
    executeCount++;

    // Check for driver input (manual control via joysticks)
    double driverForward = -driverController.getLeftY() * DRIVE_SCALING;
    double driverRotation = -driverController.getRightX() * ROTATION_SCALING;

    // Check if driver is providing rotation input (full override if rotating)
    boolean driverWantsRotation = Math.abs(driverRotation) > JOYSTICK_DEADBAND;
    boolean driverWantsMovement = Math.abs(driverForward) > JOYSTICK_DEADBAND;

    if (driverWantsRotation) {
      // FULL OVERRIDE - driver wants to control rotation, so give full control
      driveSubsystem.driveArcade(driverForward, driverRotation);

      if (!driverOverride) { // Just started override
        DriverStation.reportWarning(">>> DRIVER OVERRIDE - Full manual control <<<", false);
        driverOverride = true;
      }
      return; // Skip all auto-aim logic
    } else if (driverOverride) {
      // Driver just released rotation stick, resuming auto-aim
      DriverStation.reportWarning(">>> AUTO-AIM RESUMED <<<", false);
      driverOverride = false;
    }

    // Store driver forward input to blend with auto-aim rotation later
    double finalForward = driverWantsMovement ? driverForward : 0.0;

    // Debug: Show execute is being called (less frequently)
    if (executeCount == 1 || executeCount % 100 == 0) {
      DriverStation.reportWarning("Execute #" + executeCount + " (running for " +
        (System.currentTimeMillis() - startTime) + "ms)", false);
    }

    // Use simple LimelightHelpers calls only (like AutoAim does)
    boolean hasTarget = LimelightHelpers.getTV(limelightName);
    double tx = LimelightHelpers.getTX(limelightName);
    double tid = LimelightHelpers.getFiducialID(limelightName);

    boolean foundPrimary = false;
    boolean foundHelper = false;
    double targetTx = 0.0;
    int targetId = -1;
    boolean usingHelper = false;

    // Check if we see a relevant AprilTag
    if (hasTarget) {
      int detectedId = (int)tid;

      if (detectedId == PRIMARY_TARGET_ID) {
        foundPrimary = true;
        targetTx = tx;
        targetId = PRIMARY_TARGET_ID;
      } else if (HELPER_TAG_ID > 0 && detectedId == HELPER_TAG_ID) {
        foundHelper = true;
        targetTx = tx;
        targetId = HELPER_TAG_ID;
        usingHelper = true;
      }

      // Debug output - only show first few times and then periodically
      if (executeCount <= 3 || executeCount % 50 == 0) {
        DriverStation.reportWarning("Target: id=" + detectedId + ", tx=" + tx, false);
      }
    }

    if (foundPrimary || foundHelper) {
      // Calculate rotation needed (error from center)
      double rotationSpeed = targetTx * kP;

      // If using helper, could use gentler control to avoid overshooting when primary appears
      // Uncomment to enable slower helper tracking:
      // if (usingHelper) {
      //   rotationSpeed *= 0.7; // 70% speed when following helper
      // }

      // Apply minimum command threshold only for larger errors (> 3 degrees)
      // For small errors, use natural proportional control for fine adjustments
      if (Math.abs(targetTx) > 3.0 && Math.abs(rotationSpeed) > 0.001 && Math.abs(rotationSpeed) < kMinCommand) {
        rotationSpeed = Math.signum(rotationSpeed) * kMinCommand;
      }

      // Clamp maximum speed
      rotationSpeed = Math.max(-kMaxCommand, Math.min(kMaxCommand, rotationSpeed));

      // Apply rotation with any driver forward input
      driveSubsystem.driveArcade(finalForward, -rotationSpeed); 
      
      // Status feedback
      if (usingHelper) {
        // Using helper - different messaging
        DriverStation.reportWarning("Following helper ID " + HELPER_TAG_ID + ": tx=" + String.format("%.1f", targetTx) + ", rotation=" + String.format("%.3f", rotationSpeed), false);
      } else {
        // Using primary - normal centering messages
        if (Math.abs(targetTx) <= kTolerance) {
          DriverStation.reportWarning("TARGET CENTERED! ID " + targetId + " tx=" + String.format("%.1f", targetTx), false);
        } else {
          DriverStation.reportWarning("Auto-targeting ID " + targetId + ": tx=" + String.format("%.1f", targetTx) + ", rotation=" + String.format("%.3f", rotationSpeed), false);
        }
      }

      // Show if driver is also moving forward/backward
      if (driverWantsMovement) {
        DriverStation.reportWarning("  + Driver movement: " + String.format("%.2f", finalForward), false);
      }
    } else {
      // No tags visible at all - still allow driver movement
      driveSubsystem.driveArcade(finalForward, 0);
      if (executeCount % 50 == 1) { // Less frequent when no target
        DriverStation.reportWarning("No tags visible", false);
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    // Calculate run time
    long runTime = wasScheduled ? (System.currentTimeMillis() - startTime) : 0;

    // Stop robot
    driveSubsystem.driveArcade(0, 0);

    // Return LEDs to pipeline control
    LimelightHelpers.setLEDMode_PipelineControl(limelightName);

    // Clear priority tag
    LimelightHelpers.setPriorityTagID(limelightName, -1);

    String reason = interrupted ? "INTERRUPTED" : "completed";
    DriverStation.reportWarning("=== AUTO-TARGET END (" + reason + ") ===", false);
    DriverStation.reportWarning("   Ran for " + runTime + "ms, executed " + executeCount + " times", false);
    DriverStation.reportWarning("   Was scheduled: " + wasScheduled, false);

    // Get stack trace if interrupted to see what interrupted us
    if (interrupted && executeCount < 5) {
      DriverStation.reportWarning("   Command was interrupted very quickly!", false);
      if (executeCount == 0) {
        DriverStation.reportWarning("   Command never executed! Check subsystem conflicts.", false);
      }
    }

    wasScheduled = false;
    driverOverride = false;
  }

  @Override
  public boolean isFinished() {
    return false; // Run until interrupted
  }
}