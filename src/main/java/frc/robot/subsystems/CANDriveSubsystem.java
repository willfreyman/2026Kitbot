// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

//import com.revrobotics.spark.SparkBase.PersistMode;
//import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import static frc.robot.Constants.DriveConstants.*;

public class CANDriveSubsystem extends SubsystemBase {
  private final SparkMax leftLeader;
  private final SparkMax leftFollower;
  private final SparkMax rightLeader;
  private final SparkMax rightFollower;

  private final DifferentialDrive drive;
  
  @SuppressWarnings("removal")
  public CANDriveSubsystem() {
    // create brushed motors for drive
    leftLeader = new SparkMax(LEFT_LEADER_ID, MotorType.kBrushed);
    leftFollower = new SparkMax(LEFT_FOLLOWER_ID, MotorType.kBrushed);
    rightLeader = new SparkMax(RIGHT_LEADER_ID, MotorType.kBrushed);
    rightFollower = new SparkMax(RIGHT_FOLLOWER_ID, MotorType.kBrushed);

    // set up differential drive class
    drive = new DifferentialDrive(leftLeader, rightLeader);

    // Set can timeout. Because this project only sets parameters once on
    // construction, the timeout can be long without blocking robot operation. Code
    // which sets or gets parameters during operation may need a shorter timeout.
    leftLeader.setCANTimeout(250);
    rightLeader.setCANTimeout(250);
    leftFollower.setCANTimeout(250);
    rightFollower.setCANTimeout(250);

    // Build ONE base config, then clone/tweak for each motor
    SparkMaxConfig base = new SparkMaxConfig();
    base.voltageCompensation(12.0);
    base.smartCurrentLimit(40);
    base.idleMode(IdleMode.kBrake); // use kCoast if you prefer coasting

    // LEFT leader (not inverted)
    SparkMaxConfig leftLeaderConfig = new SparkMaxConfig();
    leftLeaderConfig.apply(base);
    leftLeaderConfig.inverted(true);

    // LEFT follower follows left leader
    SparkMaxConfig leftFollowerConfig = new SparkMaxConfig();
    leftFollowerConfig.apply(base);
    leftFollowerConfig.inverted(true);
    leftFollowerConfig.follow(leftLeader); // follow the actual object

    // RIGHT leader inverted (most drivetrains need this)
    SparkMaxConfig rightLeaderConfig = new SparkMaxConfig();
    rightLeaderConfig.apply(base);
    rightLeaderConfig.inverted(false);

    // RIGHT follower follows right leader (same inversion behavior as leader)
    SparkMaxConfig rightFollowerConfig = new SparkMaxConfig();
    rightFollowerConfig.apply(base);
    rightFollowerConfig.inverted(false);
    rightFollowerConfig.follow(rightLeader);

    // APPLY configs to hardware
    leftLeader.configure(leftLeaderConfig, SparkMax.ResetMode.kResetSafeParameters,
        SparkMax.PersistMode.kPersistParameters);

    leftFollower.configure(leftFollowerConfig, SparkMax.ResetMode.kResetSafeParameters,
        SparkMax.PersistMode.kPersistParameters);

    rightLeader.configure(rightLeaderConfig, SparkMax.ResetMode.kResetSafeParameters,
        SparkMax.PersistMode.kPersistParameters);

    rightFollower.configure(rightFollowerConfig, SparkMax.ResetMode.kResetSafeParameters,
        SparkMax.PersistMode.kPersistParameters);

    // Optional: makes arcadeDrive feel less “twitchy”
    drive.setDeadband(0.08);
  }

  @Override
  public void periodic() {
  }

  public void driveArcade(double xSpeed, double zRotation) {
    drive.arcadeDrive(xSpeed, zRotation);
  }

}
