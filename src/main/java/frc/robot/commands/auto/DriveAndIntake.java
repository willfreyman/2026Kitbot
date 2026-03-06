// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.auto;

import edu.wpi.first.wpilibj2.command.ParallelDeadlineGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.commands.AutoDrive;
import frc.robot.commands.Intake;
import frc.robot.commands.Shake;
import frc.robot.subsystems.CANDriveSubsystem;
import frc.robot.subsystems.CANFuelSubsystem;

/**
 * Autonomous: Drive forward aggressively while intaking everything
 * Good for: Collecting scattered fuel on field
 */
public class DriveAndIntake extends SequentialCommandGroup {

  public DriveAndIntake(CANDriveSubsystem driveSubsystem, CANFuelSubsystem fuelSubsystem) {
    addCommands(
        // Drive forward fast while intaking
        new ParallelDeadlineGroup(
            new AutoDrive(driveSubsystem, 0.8, 0.0).withTimeout(3.0),
            new Intake(fuelSubsystem)
        ),

        // Shake to dislodge any stuck fuel
        new Shake(driveSubsystem),

        // Continue intaking while driving slower
        new ParallelDeadlineGroup(
            new AutoDrive(driveSubsystem, 0.4, 0.0).withTimeout(1.5),
            new Intake(fuelSubsystem)
        ),

        // Final shake to settle fuel
        new Shake(driveSubsystem)
    );
  }
}