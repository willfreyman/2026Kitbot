# OLED Pong System - Comprehensive Technical Documentation

## Overview

The OLED Pong System is a complete Pong game implementation that runs on a 128x64 SSD1306 OLED display connected to the RoboRIO via I2C. This system demonstrates advanced FRC programming concepts including background threading, I2C communication, real-time graphics rendering, and non-blocking subsystem design.

## System Architecture

### Core Components

1. **OLEDPongSubsystem.java** - Main subsystem handling all game logic and display communication
2. **RobotContainer.java** - Integration point with robot command system and emergency controls
3. **SSD1306 OLED Display** - 128x64 monochrome display connected via I2C

## Hardware Setup

### Display Connection
- **Display**: SSD1306 128x64 OLED (I2C interface)
- **RoboRIO Connection**: Onboard I2C port
- **Wiring**:
  - VCC → 3.3V
  - GND → Ground
  - SDA → I2C Data (onboard port)
  - SCL → I2C Clock (onboard port)
- **I2C Address**: 0x3C (configurable to 0x3D if needed)

## OLEDPongSubsystem.java - Complete Technical Breakdown

### Display Management

#### Display Initialization (`initDisplay()`)
```java
private void initDisplay() {
    // SSD1306 initialization sequence
    sendCommand((byte)0xAE); // Display off
    sendCommand((byte)0xD5); // Set display clock divide ratio
    // ... (complete initialization sequence)
    sendCommand((byte)0xAF); // Display on
}
```

**Key Features**:
- Complete SSD1306 initialization sequence
- 180-degree rotation enabled (lines 155-156)
- Horizontal addressing mode for efficient updates
- Configurable contrast and timing

#### Display Buffer System
```java
private byte[] displayBuffer = new byte[SCREEN_WIDTH * PAGES]; // 1024 bytes
```

**Buffer Architecture**:
- **Size**: 1024 bytes (128 pixels × 8 pages)
- **Format**: Monochrome bitmap, 8 pixels per byte vertically
- **Page System**: 8 pages of 8 pixels each (total 64 pixel height)
- **Pixel Access**: `setPixel(x, y, boolean)` with automatic page/bit calculation

#### I2C Communication
```java
private void sendCommand(byte command) {
    byte[] data = {COMMAND_MODE, command};
    i2c.writeBulk(data);
}

private void updateDisplay() {
    // Send display buffer in 64-byte chunks
    for (int i = 0; i < displayBuffer.length; i += chunkSize) {
        // ... chunked transmission
    }
}
```

**Communication Features**:
- Chunked data transmission (64-byte chunks) for reliability
- Separate command and data modes
- Error handling with silent recovery

### Threading Architecture

#### Background Game Loop
```java
oledThread = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r);
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY); // Lowest priority
    t.setName("OLED-Pong-Thread");
    return t;
});
```

**Threading Design**:
- **Thread Type**: Daemon thread with minimum priority
- **Update Rate**: 50ms intervals (20 FPS)
- **Non-blocking**: Main robot loop unaffected
- **Isolation**: All game logic runs in background thread

### Game Logic Implementation

#### Ball Physics
```java
private void updateBall() {
    // Move ball
    ballX += ballVelX;
    ballY += ballVelY;

    // Wall collision detection
    if (ballY <= 0 || ballY >= SCREEN_HEIGHT - BALL_SIZE) {
        ballVelY = -ballVelY;
        ballY = Math.max(0, Math.min(SCREEN_HEIGHT - BALL_SIZE, ballY));
    }
}
```

**Physics Features**:
- Velocity-based movement system
- Perfect wall collision detection
- Paddle collision with spin effects
- Automatic ball reset after scoring

#### AI Paddle System
```java
private void updatePaddles() {
    // Calculate EXACT position where ball will reach paddle
    double timeToReachPaddle = (ballX - PADDLE_OFFSET - PADDLE_WIDTH) / Math.abs(ballVelX);
    double predictedY = ballY + (ballVelY * timeToReachPaddle);

    // ALWAYS account for wall bounces perfectly
    while (predictedY < 0 || predictedY > SCREEN_HEIGHT - BALL_SIZE) {
        if (predictedY < 0) {
            predictedY = -predictedY;
        } else if (predictedY > SCREEN_HEIGHT - BALL_SIZE) {
            predictedY = 2 * (SCREEN_HEIGHT - BALL_SIZE) - predictedY;
        }
    }
}
```

**AI Features**:
- **Perfect Prediction**: Calculates exact ball trajectory including wall bounces
- **Visual Interest**: Small random variations (±1 pixel) for realistic movement
- **Dynamic Speed**: Faster movement when paddle is far from target
- **Smooth Motion**: Gradual movement to target position

### Graphics Rendering

#### Custom Font System
```java
private static final byte[][] FONT_5X7 = {
    // '0' through '9' in 5x7 bitmap format
    {0x3E, 0x51, 0x49, 0x45, 0x3E}, // '0'
    // ... additional characters
};
```

**Font Features**:
- 5×7 pixel characters for numbers and letters
- Support for "GAME OVER", score display, and winner text
- Efficient bitmap rendering

#### Drawing Functions
```java
private void setPixel(int x, int y, boolean on) {
    // Convert x,y coordinates to buffer position
    int page = y / 8;
    int bit = y % 8;
    int index = page * SCREEN_WIDTH + x;

    if (on) {
        displayBuffer[index] |= (1 << bit);
    } else {
        displayBuffer[index] &= ~(1 << bit);
    }
}
```

**Graphics System**:
- Pixel-level drawing control
- Rectangle drawing for paddles and UI elements
- Text rendering with custom font
- Efficient buffer manipulation

### Game State Management

#### State System
```java
private enum GameState { PLAYING, GAME_OVER }
private GameState gameState = GameState.PLAYING;
```

**State Features**:
- **PLAYING**: Normal game with ball movement and paddle AI
- **GAME_OVER**: Score display and brief pause before restart
- **Score Tracking**: Automatic reset after either player reaches 10 points
- **Visual Feedback**: Game over screen with final scores

### Safety and Error Handling

#### Thread Safety
```java
public void disableOLED() {
    oledEnabled = false;
    if (oledThread != null) {
        oledThread.shutdown();
    }
}
```

#### Error Recovery
```java
try {
    // Display operations
    updateDisplay();
} catch (Exception e) {
    // Silently continue - don't let exceptions stop the game
}
```

**Safety Features**:
- Emergency disable functionality
- Silent error recovery to prevent robot crashes
- Thread cleanup on subsystem shutdown
- Safe I2C communication with error handling

## RobotContainer.java Integration

### Subsystem Initialization
```java
private static final boolean ENABLE_OLED_PONG = true;
private final OLEDPongSubsystem pongSubsystem = ENABLE_OLED_PONG ? new OLEDPongSubsystem() : null;
```

**Integration Features**:
- **Enable/Disable Flag**: Easy system-wide enable/disable
- **Conditional Initialization**: Only creates subsystem if enabled
- **Memory Safety**: Null-safe throughout codebase

### Emergency Controls
```java
operatorController.start().and(operatorController.back())
    .onTrue(new InstantCommand(() -> {
        pongSubsystem.disableOLED();
        DriverStation.reportWarning("OLED Pong disabled via controller", false);
    }));
```

**Emergency Features**:
- **Button Combination**: Start + Back buttons on operator controller
- **Immediate Shutdown**: Instant disable of OLED system
- **Status Reporting**: Clear feedback to operators

## Performance Characteristics

### Timing and Performance
- **Update Rate**: 20 FPS (50ms intervals)
- **Thread Priority**: Minimum (does not interfere with robot control)
- **Memory Usage**: ~1KB display buffer + minimal game state
- **CPU Impact**: Negligible on main robot loop

### Real-World Usage
- **Autonomous**: Continues running during auto mode
- **Teleop**: Provides entertainment without affecting robot performance
- **Disabled**: Continues running when robot is disabled
- **Competition**: Safe for use during competition (isolated background operation)

## Troubleshooting

### Common Issues

1. **Display Not Working**
   - Check I2C address (try 0x3D if 0x3C fails)
   - Verify wiring connections
   - Check power supply (3.3V)

2. **Game Performance Issues**
   - Disable OLED system via controller or code flag
   - Check for I2C conflicts with other devices

3. **Thread Issues**
   - Monitor DriverStation for exception messages
   - Use emergency disable if needed

### Debug Features
```java
public void drawTestPattern() {
    // Draws border and X pattern for display verification
}
```

## Technical Innovation

This OLED Pong system demonstrates several advanced FRC programming concepts:

1. **Background Threading**: Proper isolation of non-critical systems
2. **I2C Communication**: Low-level hardware interface programming
3. **Real-time Graphics**: Efficient bitmap manipulation and rendering
4. **AI Programming**: Predictive algorithms with visual appeal
5. **Safety Design**: Robust error handling and emergency controls

The system serves as both an entertaining addition to the robot and a comprehensive example of advanced programming techniques in the FRC environment.