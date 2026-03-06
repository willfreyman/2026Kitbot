# Limelight Vision System - Comprehensive Technical Documentation

## Overview

The Limelight Vision System provides comprehensive computer vision capabilities for FRC robots, featuring both AprilTag localization and retroreflective targeting. This system includes alliance-based automatic target configuration, seamless driver override controls, and robust fallback mechanisms for maximum reliability during competition.

## System Architecture

### Core Components

1. **AutoTarget.java** - Alliance-based AprilTag auto-targeting with driver override
2. **AutoAim.java** - Simple retroreflective target auto-aiming
3. **LimelightHelpers.java** - Comprehensive Limelight integration library (1947 lines)
4. **RobotContainer.java** - Command bindings and controller integration
5. **Robot.java** - Hardware initialization and port forwarding

## Hardware Setup

### Limelight Configuration
- **Device Name**: "limelight-first"
- **Connection**: Ethernet to RoboRIO or via USB with port forwarding
- **Pipeline 0**: AprilTag detection configured
- **Web Interface**: Access via roboRIO-[team]-FRC.local:5801 (USB) or limelight-[team].local (Ethernet)

## AutoTarget.java - Alliance-Based Auto-Targeting System

### Alliance-Based Target Configuration

```java
// BLUE ALLIANCE (targeting red side)
private static final int BLUE_PRIMARY_ID = 26;  // Blue targets red side
private static final int BLUE_HELPER_ID = 25;   // Blue helper tag

// RED ALLIANCE (targeting blue side)
private static final int RED_PRIMARY_ID = 10;   // Red targets blue side
private static final int RED_HELPER_ID = 9;     // Red helper tag
```

**Automatic Alliance Detection**:
```java
var alliance = DriverStation.getAlliance();
if (alliance.isPresent()) {
    if (alliance.get() == Alliance.Blue) {
        PRIMARY_TARGET_ID = BLUE_PRIMARY_ID;
        HELPER_TAG_ID = BLUE_HELPER_ID;
    } else if (alliance.get() == Alliance.Red) {
        PRIMARY_TARGET_ID = RED_PRIMARY_ID;
        HELPER_TAG_ID = RED_HELPER_ID;
    }
}
```

**Target Strategy**:
- **Blue Alliance**: Target IDs 25 (helper) and 26 (primary) on red side
- **Red Alliance**: Target IDs 9 (helper) and 10 (primary) on blue side
- **Helper Tag Logic**: Positioned LEFT of primary target, guides robot when primary not visible
- **Priority System**: Always prefers primary target when visible

### Driver Override System

#### Control Modes
```java
// Check for driver input
double driverForward = -driverController.getLeftY() * DRIVE_SCALING;
double driverRotation = -driverController.getRightX() * ROTATION_SCALING;

boolean driverWantsRotation = Math.abs(driverRotation) > JOYSTICK_DEADBAND;
boolean driverWantsMovement = Math.abs(driverForward) > JOYSTICK_DEADBAND;
```

**Four Control Modes**:
1. **AUTO-AIM ONLY**: No joystick input → Auto-rotates to center target
2. **MIXED MODE**: Left stick (forward/back) → Driver controls movement, auto-aim controls rotation
3. **FULL OVERRIDE**: Right stick (rotation) → Complete manual control, auto-aim pauses
4. **AUTO-AIM RESUMES**: When right stick returns to neutral (X still held)

#### Seamless Transitions
```java
if (driverWantsRotation) {
    // FULL OVERRIDE - driver wants to control rotation
    driveSubsystem.driveArcade(driverForward, driverRotation);
    if (!driverOverride) {
        DriverStation.reportWarning(">>> DRIVER OVERRIDE - Full manual control <<<", false);
        driverOverride = true;
    }
    return; // Skip all auto-aim logic
} else if (driverOverride) {
    DriverStation.reportWarning(">>> AUTO-AIM RESUMED <<<", false);
    driverOverride = false;
}
```

### Targeting Control System

#### PID Control Parameters
```java
private static final double kP = 0.025; // Proportional gain
private static final double kMinCommand = 0.04; // Minimum rotation speed
private static final double kMaxCommand = 0.35; // Maximum rotation speed
private static final double kTolerance = 1.0; // Degrees of acceptable error
```

#### Target Processing Logic
```java
// Decision logic: Primary takes priority, helper is fallback
if (foundPrimary) {
    // Primary target found - always use it, ignore helper
    targetTx = primaryTx;
    targetId = PRIMARY_TARGET_ID;
} else if (foundHelper) {
    // Only helper visible - turn towards it to find primary target
    targetTx = helperTx;
    targetId = HELPER_TAG_ID;
    usingHelper = true;
}
```

### Data Acquisition System

#### Dual Data Sources
```java
// NetworkTables access
var nt = NetworkTableInstance.getDefault();
var limelightTable = nt.getTable("limelight-first");
double ntTv = limelightTable.getEntry("tv").getDouble(-999.0);

// LimelightHelpers access
double tv = LimelightHelpers.getTV(limelightName) ? 1.0 : 0.0;
double tx = LimelightHelpers.getTX(limelightName);
```

#### JSON Fallback System
```java
var jsonResults = LimelightHelpers.getLatestResults(limelightName);
if (jsonResults != null && jsonResults.valid && jsonResults.targets_Fiducials != null) {
    for (var fiducial : jsonResults.targets_Fiducials) {
        if (fiducial.fiducialID == PRIMARY_TARGET_ID) {
            foundPrimary = true;
            primaryTx = fiducial.tx;
        }
    }
}
```

**Reliability Features**:
- **Primary**: NetworkTables direct access
- **Secondary**: LimelightHelpers wrapper functions
- **Fallback**: JSON parsing when NetworkTables fails
- **Error Detection**: Default values (-999) detect communication failures

### Limelight Configuration

#### Initialization Sequence
```java
@Override
public void initialize() {
    // Configure Limelight
    LimelightHelpers.setLEDMode_ForceOn(limelightName);
    LimelightHelpers.setPipelineIndex(limelightName, 0); // AprilTag pipeline
    LimelightHelpers.setPriorityTagID(limelightName, PRIMARY_TARGET_ID);
}
```

#### Cleanup and Reset
```java
@Override
public void end(boolean interrupted) {
    // Stop robot
    driveSubsystem.driveArcade(0, 0);
    // Return LEDs to pipeline control
    LimelightHelpers.setLEDMode_PipelineControl(limelightName);
    // Clear priority tag
    LimelightHelpers.setPriorityTagID(limelightName, -1);
}
```

## AutoAim.java - Retroreflective Target Auto-Aiming

### Simple Auto-Aim Implementation

```java
public class AutoAim extends Command {
    // PID constants for aiming
    private static final double kP = 0.03;
    private static final double kMinCommand = 0.05;
    private static final double kMaxCommand = 0.5;
    private static final double kTolerance = 1.0;
}
```

**Key Features**:
- **Target Type**: Retroreflective (bright) targets using pipeline 0
- **Control**: Proportional control for rotation only
- **Simplicity**: No driver override, pure auto-aim when button held
- **Usage**: Hold A button on driver controller for retroreflective targeting

### Target Processing
```java
@Override
public void execute() {
    boolean hasTarget = LimelightHelpers.getTV(limelightName);

    if (hasTarget) {
        double tx = LimelightHelpers.getTX(limelightName);
        double rotationSpeed = tx * kP;

        // Apply deadband and limits
        if (Math.abs(rotationSpeed) > 0.001 && Math.abs(rotationSpeed) < kMinCommand) {
            rotationSpeed = Math.signum(rotationSpeed) * kMinCommand;
        }
        rotationSpeed = Math.max(-kMaxCommand, Math.min(kMaxCommand, rotationSpeed));

        driveSubsystem.driveArcade(0, -rotationSpeed);
    }
}
```

## LimelightHelpers.java - Comprehensive Integration Library

### Library Architecture (1947 lines)

#### Core Data Structures

**LimelightResults Class**:
```java
public static class LimelightResults {
    @JsonProperty("pID") public double pipelineID;
    @JsonProperty("tl") public double latency_pipeline;
    @JsonProperty("cl") public double latency_capture;
    @JsonProperty("v") public boolean valid;
    @JsonProperty("Fiducial") public LimelightTarget_Fiducial[] targets_Fiducials;
    // ... many more fields
}
```

**AprilTag/Fiducial Target**:
```java
public static class LimelightTarget_Fiducial {
    @JsonProperty("fID") public double fiducialID;
    @JsonProperty("tx") public double tx;
    @JsonProperty("ty") public double ty;
    @JsonProperty("ta") public double ta;
    // Pose data in multiple coordinate systems
    public Pose3d getRobotPose_FieldSpace() { return toPose3D(robotPose_FieldSpace); }
}
```

#### Network Communication

**NetworkTables Integration**:
```java
public static NetworkTableEntry getLimelightNTTableEntry(String tableName, String entryName) {
    return getLimelightNTTable(tableName).getEntry(entryName);
}

public static double getLimelightNTDouble(String tableName, String entryName) {
    return getLimelightNTTableEntry(tableName, entryName).getDouble(0.0);
}
```

**JSON Processing**:
```java
public static LimelightResults getLatestResults(String limelightName) {
    ObjectMapper mapper = new ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    String jsonString = getJSONDump(limelightName);
    return mapper.readValue(jsonString, LimelightResults.class);
}
```

#### Vision Data Access Functions

**Basic Target Data**:
```java
// Target validity and position
public static boolean getTV(String limelightName)
public static double getTX(String limelightName) // Horizontal offset
public static double getTY(String limelightName) // Vertical offset
public static double getTA(String limelightName) // Target area
public static double getFiducialID(String limelightName) // AprilTag ID
```

**Robot Pose Estimation**:
```java
// 3D Poses in different coordinate systems
public static Pose3d getBotPose3d_wpiBlue(String limelightName)
public static Pose3d getBotPose3d_wpiRed(String limelightName)
public static Pose3d getBotPose3d_TargetSpace(String limelightName)

// 2D Poses for odometry integration
public static Pose2d getBotPose2d_wpiBlue(String limelightName)
public static PoseEstimate getBotPoseEstimate_wpiBlue(String limelightName)
```

#### Limelight Control Functions

**LED Control**:
```java
public static void setLEDMode_PipelineControl(String limelightName)
public static void setLEDMode_ForceOn(String limelightName)
public static void setLEDMode_ForceOff(String limelightName)
```

**Pipeline Management**:
```java
public static void setPipelineIndex(String limelightName, int pipelineIndex)
public static void setPriorityTagID(String limelightName, int ID)
```

**Advanced Configuration**:
```java
public static void SetRobotOrientation(String limelightName, double yaw, double yawRate,
    double pitch, double pitchRate, double roll, double rollRate)
public static void setCameraPose_RobotSpace(String limelightName, double forward,
    double side, double up, double roll, double pitch, double yaw)
```

### Pose Estimation System

#### MegaTag Integration
```java
public static PoseEstimate getBotPoseEstimate_wpiBlue_MegaTag2(String limelightName) {
    return getBotPoseEstimate(limelightName, "botpose_orb_wpiblue", true);
}

private static PoseEstimate getBotPoseEstimate(String limelightName, String entryName, boolean isMegaTag2) {
    // Extract pose data with timestamp and quality metrics
    var pose = toPose2D(poseArray);
    double latency = extractArrayEntry(poseArray, 6);
    int tagCount = (int)extractArrayEntry(poseArray, 7);
    double tagSpan = extractArrayEntry(poseArray, 8);

    return new PoseEstimate(pose, adjustedTimestamp, latency, tagCount,
        tagSpan, tagDist, tagArea, rawFiducials, isMegaTag2);
}
```

**Quality Metrics**:
- **Tag Count**: Number of AprilTags used for pose estimation
- **Tag Span**: Angular spread of detected tags (reliability indicator)
- **Average Distance**: Distance to detected tags
- **Average Area**: Visual size of tags (precision indicator)
- **Ambiguity**: Pose estimation confidence measure

## RobotContainer.java Integration

### Command Bindings
```java
// Auto-targeting system: HOLD X button on driver controller
driverController.x().whileTrue(new AutoTarget(driveSubsystem, driverController));

// Generic auto-aim (retroreflective targets): A button on driver controller
driverController.a().whileTrue(new AutoAim(driveSubsystem));
```

**Control Layout**:
- **X Button (Driver)**: Alliance-based AprilTag targeting with driver override
- **A Button (Driver)**: Simple retroreflective target auto-aim

### Integration Notes
```java
// Emergency OLED disable doesn't conflict with Limelight controls
// Both systems operate independently with no button conflicts
```

## Robot.java Hardware Configuration

### USB Port Forwarding
```java
@Override
public void robotInit() {
    LimelightHelpers.setupPortForwardingUSB(0);
}
```

**Port Forwarding Setup**:
- **USB Index 0**: Ports 5800-5809 forward to 172.29.0.1
- **Web Interface**: Access via roboRIO-[team]-FRC.local:5801
- **Stream Access**: Video streams available on forwarded ports

## Key Limelight Values

### NetworkTable Values (via LimelightHelpers)
- **getTV()**: Returns true if valid target detected
- **getTX()**: Horizontal offset in degrees (-29.8° to +29.8°)
  - Negative = target is left of crosshair
  - Positive = target is right of crosshair
  - 0 = perfectly centered
- **getTY()**: Vertical offset in degrees (-24.85° to +24.85°)
- **getTA()**: Target area (0-100% of image)
  - Larger values = closer to target
- **getFiducialID()**: Current AprilTag ID number

### Control Parameters (Tunable)
```java
// AutoTarget (alliance-based targeting)
kP = 0.025             // Balanced response
kMinCommand = 0.04     // Minimum speed to overcome friction
kMaxCommand = 0.35     // Safety limit
kTolerance = 1.0       // Degrees of acceptable error
JOYSTICK_DEADBAND = 0.1 // Driver override detection

// AutoAim (retroreflective targeting)
kP = 0.03              // Faster response
kMinCommand = 0.05     // Slightly higher minimum
kMaxCommand = 0.5      // Higher maximum speed
kTolerance = 1.0       // Same tolerance
```

## Performance and Reliability

### System Performance
- **Latency**: ~20-50ms total (capture + processing + network)
- **Update Rate**: 90+ Hz from Limelight
- **NetworkTables**: Automatic synchronization with robot code
- **JSON Fallback**: Ensures data availability even with NetworkTables issues

### Competition Reliability Features

#### Multi-Source Data Access
1. **Primary**: Direct NetworkTables access
2. **Secondary**: LimelightHelpers wrapper methods
3. **Fallback**: JSON parsing for complete target data

#### Error Handling
```java
try {
    String jsonString = getJSONDump(limelightName);
    results = mapper.readValue(jsonString, LimelightResults.class);
} catch (JsonProcessingException e) {
    results.error = "lljson error: " + e.getMessage();
}
```

#### Automatic Recovery
- Silent error recovery in AutoTarget system
- Graceful degradation when vision data unavailable
- Driver override always available as manual fallback

## Usage Examples

### Basic AprilTag Targeting
```java
// Simple target check
if (LimelightHelpers.getTV("limelight-first")) {
    double horizontalOffset = LimelightHelpers.getTX("limelight-first");
    double targetArea = LimelightHelpers.getTA("limelight-first");
    int tagID = (int)LimelightHelpers.getFiducialID("limelight-first");
}
```

### Pose Estimation for Odometry
```java
// Get pose estimate with quality metrics
PoseEstimate poseEst = LimelightHelpers.getBotPoseEstimate_wpiBlue("limelight-first");
if (LimelightHelpers.validPoseEstimate(poseEst)) {
    odometry.addVisionMeasurement(poseEst.pose, poseEst.timestampSeconds);
}
```

### Advanced Alliance-Based Targeting
```java
// Alliance detection happens automatically in AutoTarget.initialize()
// Just bind the command to a button:
driverController.x().whileTrue(new AutoTarget(driveSubsystem, driverController));
```

## Controller Button Mappings

### Driver Controller
| Button | Action | Description |
|--------|--------|-------------|
| X | AutoTarget | Alliance-based AprilTag targeting with driver override |
| A | AutoAim | Generic targeting (retroreflective targets) |

### Operator Controller
| Button | Action | Description |
|--------|--------|-------------|
| Left Bumper | Intake | Run intake motors |
| Right Bumper | LaunchSequence | Spin up and launch |
| A | Eject | Reverse intake |
| Y | Shake | Shake command |
| Start + Back | Emergency OLED Disable | Disable OLED Pong system |

## Troubleshooting

### Common Issues

1. **No Target Detection**
   - Check Limelight web interface (roboRIO-[team]-FRC.local:5801)
   - Verify pipeline configuration (Pipeline 0 = AprilTag)
   - Check lighting conditions and target visibility
   - Ensure correct AprilTag IDs are present

2. **NetworkTables Issues**
   - Monitor DriverStation for "NetworkTables not responding" messages
   - JSON fallback should provide data even when NT fails
   - Check network connectivity between Limelight and RoboRIO

3. **Alliance Detection Problems**
   - Verify FMS connection for automatic alliance detection
   - System defaults to Blue alliance settings if unknown
   - Check DriverStation alliance indicator

4. **Wrong Alliance Targets**
   - Blue Alliance should target IDs 25 (helper) and 26 (primary)
   - Red Alliance should target IDs 9 (helper) and 10 (primary)
   - Check FMS connection and alliance assignment

5. **Driver Override Not Working**
   - Verify JOYSTICK_DEADBAND setting (0.1)
   - Check controller stick calibration
   - Monitor DriverStation for override status messages

### Debug Features
```java
// Extensive debug output in AutoTarget
if (executeCount <= 3 || executeCount % 50 == 0) {
    DriverStation.reportWarning("Direct NT: tv=" + ntTv + ", tx=" + ntTx + ", tid=" + ntTid, false);
}

// Alliance configuration logging
DriverStation.reportWarning("BLUE ALLIANCE detected - Targeting red side", false);
DriverStation.reportWarning("Auto-targeting configured - Primary: ID " + PRIMARY_TARGET_ID, false);
```

## Testing Procedure
1. **Basic Connectivity**: Verify Limelight web interface access
2. **AprilTag Detection**: Test with known tag IDs in good lighting
3. **Alliance Detection**: Check automatic target assignment
4. **Driver Override**: Test smooth transitions between auto and manual
5. **Helper Tag Logic**: Verify fallback when primary not visible
6. **Competition Simulation**: Test with FMS-like alliance assignment

## Advanced Features
- **Alliance-Aware Programming**: Automatic target configuration based on FMS data
- **Seamless Human-Machine Interface**: Smooth transitions between auto and manual control
- **Robust Data Acquisition**: Multi-source data with automatic fallback mechanisms
- **Real-Time Control Systems**: Low-latency vision processing with PID control
- **Competition-Ready Architecture**: Comprehensive error handling and recovery systems

## Safety Notes
- Always limit maximum rotation speed
- Test in safe environment first
- Have manual override ready (release button stops)
- Monitor for overheating motors during extended use
- Driver override always takes precedence over auto-aim

## Technical Innovation

The Limelight Vision System demonstrates several advanced FRC programming concepts:

1. **Alliance-Aware Programming**: Automatic target configuration based on FMS data
2. **Seamless Human-Machine Interface**: Smooth transitions between auto and manual control
3. **Robust Data Acquisition**: Multi-source data with automatic fallback mechanisms
4. **Real-Time Control Systems**: Low-latency vision processing with PID control
5. **Competition-Ready Architecture**: Comprehensive error handling and recovery systems

This vision system provides both simple auto-aim capabilities for basic targeting and sophisticated alliance-based auto-targeting for advanced autonomous and semi-autonomous operations, making it suitable for all levels of FRC competition.