// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import static com.revrobotics.PersistMode.kPersistParameters;
import static com.revrobotics.ResetMode.kResetSafeParameters;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkFlexConfig;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import static frc.robot.Constants.FuelConstants.FEEDER_MOTOR_CURRENT_LIMIT;
import static frc.robot.Constants.FuelConstants.FEEDER_MOTOR_ID;
import static frc.robot.Constants.FuelConstants.INTAKE_MOTOR_ID;
import static frc.robot.Constants.FuelConstants.SHOOTER_LEFT_MOTOR_ID;
import static frc.robot.Constants.FuelConstants.SHOOTER_RIGHT_MOTOR_ID;


public class CANFuelSubsystem extends SubsystemBase {
  
  private final SparkFlex feederRoller;
  private final SparkFlex intakeRoller;
  
  private final SparkFlex ShooterLeftRoller;
  private final SparkFlex ShooterRightRoller;

  private final SparkClosedLoopController feederController;
  private final SparkClosedLoopController intakeController;
  private final SparkClosedLoopController shooterLeftController;
  private final SparkClosedLoopController shooterRightController;

  /** Creates a new CANBallSubsystem. */
  //@SuppressWarnings("removal")
  public CANFuelSubsystem() {
    
    // create brushed motors for each of the motors on the launcher mechanism
    intakeRoller = new SparkFlex(INTAKE_MOTOR_ID, MotorType.kBrushless);
    feederRoller = new SparkFlex(FEEDER_MOTOR_ID, MotorType.kBrushless);
    
    ShooterLeftRoller = new SparkFlex(SHOOTER_LEFT_MOTOR_ID, MotorType.kBrushless);
    ShooterRightRoller = new SparkFlex(SHOOTER_RIGHT_MOTOR_ID, MotorType.kBrushless);
    // create the configuration for the feeder roller, set a current limit and apply
    // the config to the controller
    SparkFlexConfig feederConfig = new SparkFlexConfig();
    feederConfig.closedLoop.p(0.0008).i(0).d(0.0001);
    //feederConfig.inverted(true);
    feederConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    feederRoller.configure(feederConfig, kResetSafeParameters, kPersistParameters);
    feederController = feederRoller.getClosedLoopController();

    // create the configuration for the launcher roller, set a current limit, set
    // the motor to inverted so that positive values are used for both intaking and
    // launching, and apply the config to the controller
    SparkFlexConfig intakeConfig = new SparkFlexConfig();
    intakeConfig.closedLoop.p(0.0008).i(0).d(0.0001);
    //launcherConfig.inverted(true);
    intakeConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    intakeRoller.configure(intakeConfig, kResetSafeParameters, kPersistParameters);
    intakeController = intakeRoller.getClosedLoopController();

    SparkFlexConfig shooterLeftConfig = new SparkFlexConfig();
    shooterLeftConfig.closedLoop.p(0.0008).i(0).d(0.0001);
    shooterLeftConfig.inverted(true);
    shooterLeftConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    ShooterLeftRoller.configure(shooterLeftConfig, kResetSafeParameters, kPersistParameters);
    shooterLeftController = ShooterLeftRoller.getClosedLoopController();

    SparkFlexConfig shooterRightConfig = new SparkFlexConfig();
    shooterRightConfig.closedLoop.p(0.0008).i(0).d(0.0001);
    //launcherConfig.inverted(true);
    shooterRightConfig.smartCurrentLimit(FEEDER_MOTOR_CURRENT_LIMIT);
    ShooterRightRoller.configure(shooterRightConfig, kResetSafeParameters, kPersistParameters);
    shooterRightController = ShooterRightRoller.getClosedLoopController();
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

  public void setFeederRollerRPM(double rpm) {
    feederController.setSetpoint(rpm, ControlType.kVelocity);
  }
  public void setShooterRollerRPM(double rpm) {
    shooterLeftController.setSetpoint(rpm, ControlType.kVelocity);
    shooterRightController.setSetpoint(rpm, ControlType.kVelocity);
  }
  public void setIntakeRollerRPM(double rpm) {
    intakeController.setSetpoint(rpm, ControlType.kVelocity);
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
