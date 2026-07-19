// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.Constants.FuelConstants;
import frc.robot.subsystems.CANFuelSubsystem;

// NOTE:  Consider using this command inline, rather than writing a subclass.  For more
// information, see:
// https://docs.wpilib.org/en/stable/docs/software/commandbased/convenience-features.html
public class Juggle extends SequentialCommandGroup {
  /** Creates a new Juggle. Slow launch that pops fuel back into the hopper. */
  public Juggle(CANFuelSubsystem fuelSubsystem) {
    // Add your commands in the addCommands() call, e.g.
    // addCommands(new FooCommand((), new BarCommand());
    addCommands(
        new SpinUpSlow(fuelSubsystem).withTimeout(FuelConstants.SPIN_UP_SECONDS),
        new LaunchSlow(fuelSubsystem));
  }
}
