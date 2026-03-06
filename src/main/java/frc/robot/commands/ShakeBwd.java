// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.CANDriveSubsystem;
import static frc.robot.Constants.ShakeConstants.*;

/* You should consider using the more terse Command factories API instead https://docs.wpilib.org/en/stable/docs/software/commandbased/organizing-command-based.html#defining-commands */
public class ShakeBwd extends Command {
  /** Creates a new ShakeBwd command to drive the robot backward. */

  CANDriveSubsystem DriveSubsystem;

  public ShakeBwd(CANDriveSubsystem DriveSystem) {
    addRequirements(DriveSystem);
    this.DriveSubsystem = DriveSystem;
  }

  // Called when the command is initially scheduled. Drive the robot backward
  @Override
  public void initialize() {
    DriveSubsystem
        .driveArcade(-SHAKE_SPEED, 0);
  }

  // Called every time the scheduler runs while the command is scheduled. This
  // command doesn't require updating any values while running
  @Override
  public void execute() {
  }

  // Called once the command ends or is interrupted. Stop the drive motors
  @Override
  public void end(boolean interrupted) {
    DriveSubsystem.driveArcade(0, 0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    // Let the timeout handle termination
    return false;
  }
}
