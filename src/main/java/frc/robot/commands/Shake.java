

// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.subsystems.CANDriveSubsystem;
import static frc.robot.Constants.ShakeConstants.*;

/* You should consider using the more terse Command factories API instead https://docs.wpilib.org/en/stable/docs/software/commandbased/organizing-command-based.html#defining-commands */
public class Shake extends SequentialCommandGroup {
  /** Creates a new Shake command that oscillates the robot forward and backward. */

  public Shake(CANDriveSubsystem driveSubsystem) {

    addCommands(
        new ShakeFwd(driveSubsystem).withTimeout(SHAKE_DURATION),
        new ShakeBwd(driveSubsystem).withTimeout(SHAKE_DURATION));
  }
}