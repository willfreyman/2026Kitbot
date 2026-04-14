// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean constants. This class should not be used for any other
 * purpose. All constants should be declared globally (i.e. public static). Do
 * not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the constants are needed, to reduce verbosity.
 */
public final class Constants {
  public static final class DriveConstants {
    // Motor controller IDs for drivetrain motors
    public static final int LEFT_LEADER_ID = 1;
    public static final int LEFT_FOLLOWER_ID = 2;
    public static final int RIGHT_LEADER_ID = 3;
    public static final int RIGHT_FOLLOWER_ID = 4;

    // Current limit for drivetrain motors. 60A is a reasonable maximum to reduce
    // likelihood of tripping breakers or damaging CIM motors
    public static final int DRIVE_MOTOR_CURRENT_LIMIT = 40;
  }

  public static final class FuelConstants {
    // Motor controller IDs for Fuel Mechanism motors
    public static final int FEEDER_MOTOR_ID = 6;
    public static final int INTAKE_MOTOR_ID = 5;

    public static final int SHOOTER_LEFT_MOTOR_ID = 7;
    public static final int SHOOTER_RIGHT_MOTOR_ID = 8;

    // Current limit and nominal voltage for fuel mechanism motors.
    public static final int FEEDER_MOTOR_CURRENT_LIMIT = 40;
    public static final int LAUNCHER_MOTOR_CURRENT_LIMIT = 40;

    // Voltage values for various fuel operations. These values may need to be tuned
    // based on exact robot construction.
    // See the Software Guide for tuning information
    public static final double INTAKING_FEEDER_VOLTAGE = -6;
    public static final double INTAKING_INTAKE_VOLTAGE = 6;
    public static final double LAUNCHING_FEEDER_VOLTAGE = 9;
    public static final double LAUNCHING_SHOOTER_VOLTAGE = -4;
    public static final double LAUNCHING_INTAKE_VOLTAGE = 9;
    public static final double SPIN_UP_FEEDER_VOLTAGE = -4;
    public static final double SPIN_UP_SECONDS = 1;


    public static final double INTAKING_FEEDER_RPM = -2800;
    public static final double INTAKING_INTAKE_RPM = 2800;
    public static final double LAUNCHING_FEEDER_RPM = 2800;
    public static final double LAUNCHING_SHOOTER_SLOW_RPM = -1300;
    public static final double LAUNCHING_SHOOTER_RPM = -3450 ; // -3500 normally, -1300 for slow shoot
    public static final double LAUNCHING_INTAKE_RPM = 2800;
    public static final double SPIN_UP_FEEDER_RPM = -1000;
        
    
  }

  public static final class OperatorConstants {
    // Port constants for driver and operator controllers. These should match the
    // values in the Joystick tab of the Driver Station software
    public static final int DRIVER_CONTROLLER_PORT = 0;
    public static final int OPERATOR_CONTROLLER_PORT = 0;

    // This value is multiplied by the joystick value when rotating the robot to
    // help avoid turning too fast and beign difficult to control
    public static final double DRIVE_SCALING = .7;
    public static final double ROTATION_SCALING = .8;
  }

  public static final class ShakeConstants {
    public static final double SHAKE_SPEED = 1;

    // Kept short to return to same position and dislodge stuck fuel
    public static final double SHAKE_DURATION = 0.3;
  }
}
