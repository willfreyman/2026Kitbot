// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import static frc.robot.Constants.PneumaticConstants.EXTEND_CHANNEL;
import static frc.robot.Constants.PneumaticConstants.HUB_CAN_ID;
import static frc.robot.Constants.PneumaticConstants.MAX_PRESSURE_PSI;
import static frc.robot.Constants.PneumaticConstants.MIN_PRESSURE_PSI;
import static frc.robot.Constants.PneumaticConstants.PULSE_SECONDS;
import static frc.robot.Constants.PneumaticConstants.RETRACT_CHANNEL;
import static frc.robot.Constants.PneumaticConstants.UNCLOG_EXTEND_SECONDS;
import static frc.robot.Constants.PneumaticConstants.UNCLOG_RETRACT_SECONDS;

public class PneumaticSubsystem extends SubsystemBase {
  private final DoubleSolenoid piston =
      new DoubleSolenoid(HUB_CAN_ID, PneumaticsModuleType.REVPH, EXTEND_CHANNEL, RETRACT_CHANNEL);

  private final Compressor compressor = new Compressor(HUB_CAN_ID, PneumaticsModuleType.REVPH);

  // Whether the compressor's analog control loop is currently armed.
  // Even when true, the PH only runs the motor while pressure < MIN_PRESSURE_PSI.
  private boolean compressorActive = false;

  public PneumaticSubsystem() {
    piston.set(Value.kOff);
    compressor.disable();
  }

  // Pulse the extend coil for PULSE_SECONDS, then de-energize both coils.
  public Command extend() {
    return startEnd(() -> piston.set(Value.kForward), () -> piston.set(Value.kOff))
        .withTimeout(PULSE_SECONDS);
  }

  // Pulse the retract coil for PULSE_SECONDS, then de-energize both coils.
  public Command retract() {
    return startEnd(() -> piston.set(Value.kReverse), () -> piston.set(Value.kOff))
        .withTimeout(PULSE_SECONDS);
  }

  // Hold the extend coil (channel 14) energized for as long as the command is scheduled.
  public Command holdExtend() {
    return startEnd(() -> piston.set(Value.kForward), () -> piston.set(Value.kOff));
  }

  // Hold the retract coil (channel 15) energized for as long as the command is scheduled.
  public Command holdRetract() {
    return startEnd(() -> piston.set(Value.kReverse), () -> piston.set(Value.kOff));
  }

  // Unclog: arm the compressor, then continuously alternate the two solenoids
  // (extend for UNCLOG_PULSE_SECONDS, retract for UNCLOG_PULSE_SECONDS, repeat)
  // as if rapidly tapping D-pad up then down. Bind with toggleOnTrue so one press
  // starts it and the next press stops it. When cancelled, both coils de-energize.
  public Command unclog() {
    return Commands.repeatingSequence(
            runOnce(() -> piston.set(Value.kForward)),
            Commands.waitSeconds(UNCLOG_EXTEND_SECONDS),
            runOnce(() -> piston.set(Value.kReverse)),
            Commands.waitSeconds(UNCLOG_RETRACT_SECONDS))
        .beforeStarting(() -> {
          compressorActive = true;
          compressor.enableAnalog(MIN_PRESSURE_PSI, MAX_PRESSURE_PSI);
        })
        .finallyDo(interrupted -> piston.set(Value.kOff));
  }

  // Toggle the compressor's analog control loop on/off.
  // When armed, the PH firmware runs the motor only when pressure < MIN_PRESSURE_PSI
  // and stops at > MAX_PRESSURE_PSI. Press again to disarm entirely.
  public Command toggleCompressor() {
    return runOnce(() -> {
      compressorActive = !compressorActive;
      if (compressorActive) {
        compressor.enableAnalog(MIN_PRESSURE_PSI, MAX_PRESSURE_PSI);
      } else {
        compressor.disable();
      }
    });
  }
}
