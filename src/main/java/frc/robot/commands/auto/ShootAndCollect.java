// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.auto;

import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.commands.AutoDrive;
import frc.robot.commands.Intake;
import frc.robot.commands.LaunchSequence;
import frc.robot.subsystems.CANDriveSubsystem;
import frc.robot.subsystems.CANFuelSubsystem;

/**
 * Autonomous: Shoot preload, collect more fuel, return and shoot again
 * Good for: When fuel is placed in front of robot
 */
public class ShootAndCollect extends SequentialCommandGroup {

  public ShootAndCollect(CANDriveSubsystem driveSubsystem, CANFuelSubsystem fuelSubsystem) {
    addCommands(
        // Shoot any preloaded fuel
        new LaunchSequence(fuelSubsystem).withTimeout(2.5),

        // Drive forward while intaking for 2 seconds
        new ParallelDeadlineGroup(
            new AutoDrive(driveSubsystem, 0.6, 0.0).withTimeout(2.0),
            new Intake(fuelSubsystem)
        ),

        // Continue intaking while stationary to ensure fuel is secured
        new Intake(fuelSubsystem).withTimeout(0.5),

        // Drive backward to shooting position
        new AutoDrive(driveSubsystem, -0.6, 0.0).withTimeout(2.0),

        // Wait to stabilize
        new WaitCommand(0.5),

        // Shoot collected fuel
        new LaunchSequence(fuelSubsystem).withTimeout(3.0)
    );
  }
}