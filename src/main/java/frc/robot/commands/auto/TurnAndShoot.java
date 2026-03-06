// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.auto;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.commands.AutoDrive;
import frc.robot.commands.LaunchSequence;
import frc.robot.subsystems.CANDriveSubsystem;
import frc.robot.subsystems.CANFuelSubsystem;

/**
 * Autonomous: Turn to face target, then shoot
 * Good for: Side starting positions
 */
public class TurnAndShoot extends SequentialCommandGroup {

  public TurnAndShoot(CANDriveSubsystem driveSubsystem, CANFuelSubsystem fuelSubsystem, boolean turnLeft) {
    double turnSpeed = turnLeft ? -0.6 : 0.6;

    addCommands(
        // Turn toward center (adjust time based on testing)
        new AutoDrive(driveSubsystem, 0.0, turnSpeed).withTimeout(0.75),

        // Wait to stabilize
        new WaitCommand(0.5),

        // Shoot
        new LaunchSequence(fuelSubsystem).withTimeout(3.0),

        // Back up slightly
        new AutoDrive(driveSubsystem, -0.5, 0.0).withTimeout(1.0)
    );
  }
}