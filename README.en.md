# QFloatingX

QQ Enhanced Floating Window Plugin - A Feature-Rich Xposed Module

## Project Overview

QFloatingX is an Xposed floating window plugin designed for Tencent QQ, offering a rich set of enhanced features including message statistics, floating window control, mock location, color picker, and more.

## Features

### Core Features

- **Floating Window Management** - Supports showing, hiding, dragging, and closing floating windows
- **Message Statistics** - Tracks daily/weekly/monthly sent and received messages with categorization by message type
- **Mock Location** - Supports GPS location spoofing
- **Color Picker** - Offers multiple color selection methods including RGB, HSV, color wheel, and color bars

### Message Handling

- **Message Replay** - Supports resending messages
- **Message Forwarding** - Forwards messages via server
- **Double-Tap Messages** - Triggers quick actions on double-tapping messages
- **Message Menu** - Displays an extended menu on long-pressing messages

### Utility Tools

- **HTML Preview** - Built-in HTML file viewer with draggable bottom panel
- **QZone Assistant** - Auto-like and auto-comment on QQ Space posts
- **PB Sender** - Supports sending raw Protocol Buffers data
- **Image Editing** - Supports image cropping, scaling, and blurring

### Scripting System

- **Hot-Swappable Scripts** - Supports dynamic loading and execution of BeanShell scripts
- **Predefined Templates** - Includes multiple built-in script templates
- **Scheduled Tasks** - Supports timed recurring task execution
- **Message Queue** - Supports batch message sending

### UI Enhancements

- **Diverse Dialog Styles** - Multiple modern dialog designs
- **Dark Mode** - Supports light/dark theme switching
- **Custom Backgrounds** - Supports image backgrounds and blur effects
- **Rounded Windows** - Supports configurable window corner radius

## File Structure

```
QFloatingX/
├── API/
│   ├── api.java         # Core API
│   ├── api2.java        # Mock Location
│   ├── api3.java        # Message Statistics
│   ├── api4.java        # Floating Window Management
│   ├── api5.java        # Message Handling
│   ├── api6.java        # Runtime Status
│   ├── api7.java        # HTML Preview
│   ├── api8.java        # QZone Assistant
│   ├── api9.java        # PB Sender
│   ├── ColorPicker.java # Color Picker
│   ├── Dialog.java      # Dialog Utilities
│   ├── function.java    # Functions & Scripts
│   ├── import.java      # Import Declarations
│   ├── setwindow.java   # Window Settings
│   └── uitools.java     # UI Tools
├── main.java            # Entry Point
├── info.prop            # Module Information
└── desc.txt             # Description File
```

## Technology Stack

- **Language**: Java
- **Framework**: Xposed Framework
- **Target App**: Tencent QQ
- **Minimum Android Version**: Compatible with Android versions supported by QQ

## Usage Instructions

### Prerequisites

1. Xposed Framework or LSPosed (or compatible framework) installed
2. Root permissions granted
3. Tencent QQ app installed

### Installation Steps

1. Download and install the QFloatingX module
2. Enable the module in Xposed/LSPosed framework
3. Reboot device or restart QQ app
4. Access features via floating window or module entry points within QQ

### Main Entry Points

- **Floating Window**: Appears in chat interface; supports dragging and tapping
- **Message Statistics**: Access statistics via module interface
- **Settings**: Configure module parameters within the module settings

## Feature Module Details

### Floating Window (api4.java)

Supports dragging, edge snapping, long-press to close, and tap actions. Customizable icon and transparency.

### Message Statistics (api3.java)

- Tracks text, image, file, voice, emoji, and other message types
- Supports today, yesterday, this week, this month, and custom date ranges
- Set daily message targets and view progress completion

### Mock Location (api2.java)

Uses Hook on LocationManager to simulate GPS location; toggle control available.

### QZone Assistant (api8.java)

- Automatically browses friends' QQ Space updates
- Supports auto-like and auto-comment
- Includes blacklist filtering

### PB Sender (api9.java)

Supports sending raw Protocol Buffers data for advanced debugging and testing.

## Configuration

Module configuration files are located in the `/config/` directory, including:

- `msg_stats.json` - Message statistics data storage
- Other feature-specific configuration files

## Notices

1. This module is intended solely for learning and educational purposes.
2. Certain features may violate Tencent QQ's Terms of Service—use at your own risk.
3. Some features require root permissions.
4. Mock location and similar functions should be used exclusively for testing purposes.

## Version Information

- Current Version: 2.3.1
- Compatible QQ Versions: Refer to module announcements

## License

This project is provided for learning and communication purposes only. Commercial use is strictly prohibited.