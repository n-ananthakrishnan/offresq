# ESP32-S3 Configuration Guide

## Quick Start for ESP32-S3 Arduino Code

This guide helps you configure your ESP32-S3 to send GPS data to the Spring Boot gateway.

## Step 1: Find Your PC's IP Address

### Windows
1. Open **Command Prompt** (Win + R, type `cmd`)
2. Type: `ipconfig`
3. Look for **IPv4 Address** under your network adapter (usually `192.168.x.x` or `10.x.x.x`)

Example output:
```
Ethernet adapter Ethernet:
   IPv4 Address . . . . . . . . . . . : 192.168.1.100
```

### Linux
```bash
hostname -I
# or
ip addr show
```

### macOS
```bash
ifconfig | grep "inet "
```

## Step 2: Update ESP32 Arduino Code

In your ESP32-S3 sketch, update these lines:

```c
// Line 7-8: WiFi Credentials
const char* ssid = "YOUR_WIFI_NAME";        // Change this!
const char* password = "YOUR_WIFI_PASSWORD"; // Change this!

// Line 11: Server URL - Replace with your PC's IP
const char* serverURL = "http://192.168.1.100:8080/api/location";
//                                ^^^ Replace with your actual IP!

// Line 14: Device ID (optional - for tracking multiple devices)
const char* deviceID = "DEVICE_001";  // Or use "ESP32_01", "TRACKER_A", etc.
```

**Example:**
```c
const char* ssid = "MyHomeWiFi";
const char* password = "MyPassword123";
const char* serverURL = "http://192.168.1.100:8080/api/location";
const char* deviceID = "LORA_GATEWAY_01";
```

## Step 3: LoRa Pin Configuration

The code uses these pins (already configured, verify if using custom board):

| LoRa Pin | ESP32-S3 GPIO |
|----------|---------------|
| SCK      | GPIO 7        |
| MISO     | GPIO 8        |
| MOSI     | GPIO 9        |
| SS (CS)  | GPIO 10       |
| RST      | GPIO 11       |
| DIO0/IRQ | GPIO 12       |
| LED      | GPIO 48       |

If your board uses different pins, update:
```c
#define LORA_SCK    7    // Change if needed
#define LORA_MISO   8
#define LORA_MOSI   9
#define LORA_SS     10
#define LORA_RST    11
#define LORA_DIO0   12
#define LED_PIN     48
```

## Step 4: Upload to ESP32-S3

### Arduino IDE Setup
1. Install **ESP32 Board Package** in Arduino IDE:
   - Tools → Board Manager → Search "esp32" → Install by Espressif
   
2. Select board: **ESP32-S3 Dev Module**
   - Tools → Board → esp32 → ESP32-S3 Dev Module

3. Install required libraries:
   - Sketch → Include Library → Manage Libraries
   - Search and install:
     - **LoRa** by Sandeep Mistry
     - **ArduinoJson** by Benoit Blanchon
     - **WiFi** (usually pre-installed)
     - **HTTPClient** (usually pre-installed)

4. Upload the sketch:
   - Select correct COM port (Tools → Port)
   - Click Upload arrow

## Step 5: Monitor ESP32 Output

1. Open Serial Monitor: Tools → Serial Monitor
2. Set baud rate to **115200**
3. You should see startup messages:

```
╔════════════════════════════════════════╗
║   OffResq Gateway/Receiver Starting    ║
║   ESP32-S3 + LoRa + WiFi              ║
╚════════════════════════════════════════╝

🔧 Initializing LoRa...
✅ LoRa initialized successfully
   Frequency: 433.0 MHz
   Spreading Factor: 12
   Bandwidth: 125 kHz

🔧 Connecting to WiFi...
   SSID: MyHomeWiFi
✅ WiFi Connected!
   IP Address: 192.168.1.50
   Signal Strength: -45 dBm
   Server: http://192.168.1.100:8080/api/location

✅ System Ready - Waiting for LoRa packets...
```

## Step 6: Test Data Transmission

Once the ESP32 receives GPS data (from LoRa or simulated), it will:
1. Parse the JSON
2. Validate coordinates
3. Send to server: `POST http://192.168.1.100:8080/api/location`

Serial output example:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📡 LoRa Packet #1 Received
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Data: {"lat":37.7749,"lon":-122.4194,"a":0}
RSSI: -95 dBm
SNR: 7.25 dB
Size: 40 bytes

📍 Parsed GPS Data:
   Latitude:  37.774900
   Longitude: -122.419400
   Alert:     ✅ Normal

📤 Sending to server...
   Payload: {"deviceId":"DEVICE_001","latitude":37.7749,"longitude":-122.4194,"alert":0,"rssi":-95,"timestamp":15234}
✅ Server Response [200]: {"message":"Location saved successfully","id":1}
```

## Step 7: View on Map

Open your browser:
```
http://localhost:8080/
```

You should see:
- 🗺️ Interactive map
- 📍 Markers for each location
- 🟢 Green marker = Normal status
- 🔴 Red marker = Emergency alert

## Troubleshooting

### ❌ WiFi not connecting
```
Solution:
1. Check SSID and password are correct (case-sensitive!)
2. Ensure ESP32-S3 is close to router
3. Try using 2.4 GHz WiFi (not 5 GHz)
4. Restart router and ESP32
```

### ❌ Can't connect to server
```
Solution:
1. Verify PC IP address: ipconfig (Windows)
2. Make sure IP in code matches exactly
3. Verify port 8080 is accessible
4. Disable Windows Firewall or add Java exception
5. Ping the server from ESP32:
   - Add in setup(): if(ping(serverURL)) Serial.println("Server reachable");
```

### ❌ LoRa not initializing
```
Solution:
1. Check LoRa module wiring matches pin definitions
2. Verify SPI connections: SCK, MOSI, MISO
3. Check CS (SS) pin is correct
4. Try different frequency: 433E6, 915E6, or 868E6
5. Use multimeter to check power supply to LoRa module
```

### ❌ No data on map
```
Solution:
1. Check Serial Monitor output for errors
2. Verify GPS data is being received by ESP32
3. Test API: curl http://localhost:8080/api/locations
4. Check browser console (F12) for JavaScript errors
5. Ensure H2 database is running (should be automatic)
```

## Testing Without LoRa

To test the API without LoRa, simulate data:

```bash
# Linux/Mac/PowerShell
curl -X POST http://localhost:8080/api/location \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "TEST_001",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "alert": 0,
    "rssi": -85,
    "timestamp": 1234567890000
  }'

# Verify it was received
curl http://localhost:8080/api/locations
```

## Default LoRa Settings

```c
Frequency:        433E6 (433 MHz)  // Change if needed
Spreading Factor: 12 (max range)
Bandwidth:        125 kHz
Coding Rate:      4/5
Preamble Length:  8
Sync Word:        0x12
CRC:              Enabled
```

These should match on all devices for successful communication!

## Next Steps

1. ✅ Configure ESP32-S3 code
2. ✅ Start Spring Boot application
3. ✅ Upload sketch to ESP32
4. ✅ Check serial output
5. ✅ Open http://localhost:8080
6. ✅ View real-time GPS tracking

---

Need help? Check the SETUP_GUIDE.md for more details!
