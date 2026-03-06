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
 * Autonomous: Shoot preloaded fuel, then back up away from line
 * Good for: When you have preloaded fuel and want a simple, reliable auto
 */
public class ShootAndBackup extends SequentialCommandGroup {

  public ShootAndBackup(CANDriveSubsystem driveSubsystem, CANFuelSubsystem fuelSubsystem) {
    addCommands(
        // Wait a moment for robot to settle
        new WaitCommand(0.5),

        // Shoot any preloaded fuel (spin up + launch)
        new LaunchSequence(fuelSubsystem).withTimeout(3.0),

        // Back away from line at 60% speed for 2 seconds
        new AutoDrive(driveSubsystem, -0.6, 0.0).withTimeout(2.0)
    );
  }
}