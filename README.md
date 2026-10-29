# PropellerKart: Android Bluetooth Tilt Controller

An Android app that turns a phone into a **tilt controller** for a propeller kart. It reads the phone's **accelerometer**, packs the values into a short text frame, and streams them over **Bluetooth (RFCOMM / SPP)** to a PC every **50 ms**. A horn button is included.

<!-- TODO: add a photo or GIF of the app and the kart, e.g. docs/demo.gif -->
<!-- ![Demo](docs/demo.gif) -->

## Features

- Connects to a PC over **Bluetooth Classic (SPP, UUID `00001101-0000-1000-8000-00805F9B34FB`)**
- Live display of the accelerometer axes (x, y, z) and two level bars (horizontal = x, vertical = y)
- **Horn button** that toggles the `b` field between `0` (off) and `5` (on)
- Start/stop automatic sending at **20 frames per second** (every 50 ms)
- Shows the exact frame being sent
- Connection status and error messages (toasts)
- Fallback connection method if the standard RFCOMM socket fails

## Frame format

Each frame is plain ASCII, terminated by a carriage return (`CR`, `0x0D`):

```
#b=5#x=-7.12#y=+3.45#z=+2.23#<CR>
```

| Field | Meaning |
|---|---|
| `b` | Horn: `0` = inactive, `5` = active |
| `x` | Accelerometer X axis (m/s²), signed, 2 decimals |
| `y` | Accelerometer Y axis (m/s²), signed, 2 decimals |
| `z` | Accelerometer Z axis (m/s²), signed, 2 decimals |

## System overview

```
Phone (this app)  ──Bluetooth SPP, 50 ms frames──►  PC  ──►  TODO: how the PC drives the kart
   accelerometer                                    (serial / microcontroller / ...)
```

<!-- TODO: describe the receiving side and the kart hardware: PC program, microcontroller, motors/propeller, how the frame values map to speed and steering. -->

## Requirements

- Android phone with Bluetooth and an accelerometer (TODO: minimum Android version, see `minSdk` in `app/build.gradle.kts`)
- **Android Studio** (Gradle project, Java)
- A PC with Bluetooth, paired with the phone, and a program that listens on an incoming Bluetooth serial (SPP) port

## Setup and build

1. **Clone** the repo and open it in Android Studio, then let Gradle sync finish.
2. **Pair** the phone with your PC in the phone's Bluetooth settings.
3. **Set your PC's Bluetooth MAC address** in `MainActivity.java`:
   ```java
   private static final String PC_MAC = "XX:XX:XX:XX:XX:XX";
   ```
   On Windows, you can find it in the Bluetooth adapter properties or with `ipconfig /all` (look for the *Bluetooth Network Connection* entry's physical address).
4. **Run** the app on a physical device. The Bluetooth and accelerometer features don't work on the emulator.
5. Grant the **Bluetooth permissions** when prompted (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`).

## Usage

1. Start the receiving program on the PC and make sure it listens on the Bluetooth serial port.
2. In the app, tap **Connecter au PC**. The status turns green ("Connecté au PC").
3. Tap **Démarrer l'envoi auto** to begin streaming. The current frame appears under *Trame envoyée*.
4. Tilt the phone to steer and control the kart. Use **Klaxon** to toggle the horn.
5. Tap **Arrêter l'envoi** to stop.

## Project structure

```
app/src/main/
├── java/com/example/propellerkart/MainActivity.java   # sensor, Bluetooth, frame building
├── res/layout/activity_main.xml                       # UI
└── AndroidManifest.xml                                # Bluetooth permissions
```

## Possible improvements

- Scan and pick the PC from a device list instead of a hardcoded MAC address
- Low-pass filter on the accelerometer values
- Runtime permission handling flow for Android 12+
- Reconnect automatically after a lost connection

## Context

<!-- TODO: e.g. "School project for <course>, <school>, <year>." -->

## Authors

- Malek Mbarek
- Meryeme TODO: full name

## Credits

TODO: mention anything reused (libraries, tutorials, hardware kit).
