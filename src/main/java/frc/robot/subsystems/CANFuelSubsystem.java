// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import static com.revrobotics.PersistMode.kPersistParameters;
import static com.revrobotics.ResetMode.kResetSafeParameters;
import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkFlexConfig;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import static frc.robot.Constants.FuelConstants.FEEDER_MOTOR_CURRENT_LIMIT;


public class CANFuelSubsystem extends SubsystemBase {
  
  private final SparkFlex feederRoller;
  private final SparkFlex intakeRoller;
  
  private final SparkFlex ShooterLeftRoller;
  private final SparkFlex ShooterRightRoller;

  /** Creates a new CANBallSubsystem. */
  //@SuppressWarnings("removal")
  public CANFuelSubsystem() {
      int INTAKE_MOTOR_ID = 5;
      int FEEDER_MOTOR_ID = 6;
      int SHOOTER_LEFT_MOTOR_ID = 7;
      int SHOOTER_RIGHT_MOTOR_ID = 8;

    //These are made locally because it was mad when they were in constants.java
    
    // create brushed motors for each of the motors on the launcher mechanism
    intakeRoller = new SparkFlex(INTAKE_MOTOR_ID, MotorType.kBrushless);
    feederRoller = new SparkFlex(FEEDER_MOTOR_ID, MotorType.kBrushless);
    
    ShooterLeftRoller = new SparkFlex(SHOOTER_LEFT_MOTOR_ID, MotorType.kBrushless);
    ShooterRightRoller = new SparkFlex(SHOOTER_RIGHT_MOTOR_ID, MotorType.kBrushless);
    // create the configuration for the feeder roller, set a current limit and apply
    // the config to the controller
    SparkFlexConfig feederConfig = new SparkFlexConfig();
    feederConfig.inverted(true);
    feederConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    feederRoller.configure(feederConfig, kResetSafeParameters, kPersistParameters);

    // create the configuration for the launcher roller, set a current limit, set
    // the motor to inverted so that positive values are used for both intaking and
    // launching, and apply the config to the controller
    SparkFlexConfig intakeConfig = new SparkFlexConfig();
    //launcherConfig.inverted(true);
    intakeConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    intakeRoller.configure(intakeConfig, kResetSafeParameters, kPersistParameters);

    SparkFlexConfig shooterLeftConfig = new SparkFlexConfig();
    shooterLeftConfig.inverted(true);
    shooterLeftConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    ShooterLeftRoller.configure(shooterLeftConfig, kResetSafeParameters, kPersistParameters);

    SparkFlexConfig shooterRightConfig = new SparkFlexConfig();
    //launcherConfig.inverted(true);
    shooterRightConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    ShooterRightRoller.configure(shooterRightConfig, kResetSafeParameters, kPersistParameters);
  }

  // A method to set the voltage of the intake roller
  public void setIntakeRoller(double voltage) {
    intakeRoller.setVoltage(voltage);
  }

  // A method to set the voltage of the shooter rollers
  public void setShooterRollers(double voltage) {
    ShooterLeftRoller.setVoltage(voltage);
    ShooterRightRoller.setVoltage(voltage);
  }

  // A method to set the voltage of the intake roller
  public void setFeederRoller(double voltage) {
    feederRoller.setVoltage(voltage);
  }

  // A method to stop the rollers
  public void stop() {
    ShooterLeftRoller.set(0);
    ShooterRightRoller.set(0);
    feederRoller.set(0);
    intakeRoller.set(0);
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
