# OffResq GPS Gateway - Spring Boot Backend

A Spring Boot application that receives GPS location data from ESP32-S3 devices via WiFi and visualizes the coordinates on an interactive map.

## Features

✅ **REST API** - Receive GPS data from ESP32-S3 via HTTP POST  
✅ **Real-time Visualization** - Interactive map using Leaflet.js with OpenStreetMap  
✅ **Location Storage** - H2 in-memory database with JPA persistence  
✅ **Multi-device Support** - Track multiple devices simultaneously  
✅ **Emergency Alerts** - Visual indicators for alert status  
✅ **Auto-refresh** - Real-time updates every 3 seconds  

## Architecture

```
┌─────────────────┐
│   ESP32-S3      │
│  (with LoRa)    │
└────────┬────────┘
         │ WiFi HTTP POST
         │ /api/location
         │
┌────────▼────────────────────────────────────────┐
│        Spring Boot Gateway Application           │
├─────────────────────────────────────────────────┤
│  REST API                                       │
│  ├── POST /api/location (receive data)         │
│  ├── GET /api/locations (get all)              │
│  ├── GET /api/locations/{deviceId} (by device)│
│  ├── DELETE /api/location/{id} (delete one)   │
│  └── DELETE /api/locations/clear (delete all) │
├─────────────────────────────────────────────────┤
│  Web UI                                         │
│  └── / (Interactive map visualization)         │
├─────────────────────────────────────────────────┤
│  Database                                       │
│  └── H2 (In-Memory)                            │
└─────────────────────────────────────────────────┘
         │
         └── Browser: http://localhost:8080/
             Leaflet.js + OpenStreetMap
```

## Project Structure

```
gateway/
├── pom.xml
├── src/main/java/com/offresq/gateway/
│   ├── GatewayApplication.java
│   ├── controller/
│   │   ├── LocationController.java  (REST API)
│   │   └── ViewController.java       (HTML routes)
│   ├── model/
│   │   └── Location.java            (Entity)
│   └── repository/
│       └── LocationRepository.java   (JPA Repo)
├── src/main/resources/
│   ├── application.properties         (Configuration)
│   └── templates/
│       └── index.html                 (Map UI)
└── HELP.md
```

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **ESP32-S3 with LoRa module** (running the provided Arduino code)

## Installation & Setup

### 1. Update ESP32-S3 Configuration

Edit the Arduino sketch on your ESP32-S3 and update:

```c
const char* ssid = "Your_WiFi_SSID";              // Your WiFi name
const char* password = "Your_WiFi_Password";      // Your WiFi password
const char* serverURL = "http://YOUR_PC_IP:8080/api/location";
const char* deviceID = "DEVICE_001";              // Unique ID
```

**Find your PC IP address:**
- **Windows:** Open Command Prompt and run `ipconfig` → Look for IPv4 Address
- **Linux/Mac:** Open Terminal and run `ifconfig` or `hostname -I`

### 2. Build the Spring Boot Application

```bash
cd gateway
mvn clean package
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

Or directly:

```bash
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

The server will start on `http://localhost:8080`

### 4. Access the Web UI

Open your browser and navigate to:

```
http://localhost:8080/
```

You'll see an interactive map where GPS locations from your ESP32-S3 devices will appear in real-time.

## API Endpoints

### Receive Location Data
```
POST /api/location
Content-Type: application/json

{
  "deviceId": "DEVICE_001",
  "latitude": 37.7749,
  "longitude": -122.4194,
  "alert": 0,
  "rssi": -85,
  "timestamp": 1234567890000
}

Response:
{
  "message": "Location saved successfully",
  "id": 1
}
```

### Get All Locations
```
GET /api/locations

Response:
[
  {
    "id": 1,
    "deviceId": "DEVICE_001",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "alert": 0,
    "rssi": -85,
    "timestamp": 1234567890000,
    "createdAt": "2025-12-30T10:30:45"
  },
  ...
]
```

### Get Locations for Specific Device
```
GET /api/locations/{deviceId}

Example: GET /api/locations/DEVICE_001
```

### Get Latest Location
```
GET /api/location/latest
```

### Delete Specific Location
```
DELETE /api/location/{id}

Example: DELETE /api/location/1
```

### Clear All Locations
```
DELETE /api/locations/clear
```

## Web UI Features

### 🗺️ Interactive Map
- **Zoom & Pan:** Use mouse wheel or touch gestures
- **Markers:** Each GPS location appears as a colored marker
  - 🟢 **Green:** Normal status (alert = 0)
  - 🔴 **Red:** Emergency alert (alert = 1)

### 📍 Information Panel
- Click any marker to view detailed information:
  - Device ID
  - Latitude & Longitude (6 decimal places)
  - Signal Strength (RSSI in dBm)
  - Timestamp

### 🎮 Control Buttons
- **🔄 Refresh:** Manually refresh locations
- **📍 Center Map:** Center on latest location
- **🗑️ Clear All:** Delete all stored locations

### 📊 Live Counter
- Shows total number of tracked locations
- Updates automatically every 3 seconds

## Data Fields

| Field | Type | Description |
|-------|------|-------------|
| `deviceId` | String | Unique identifier for the transmitter |
| `latitude` | Double | GPS latitude (-90 to 90) |
| `longitude` | Double | GPS longitude (-180 to 180) |
| `alert` | Integer | Alert status (0=normal, 1=emergency) |
| `rssi` | Integer | Signal strength in dBm (LoRa) |
| `timestamp` | Long | Milliseconds since device started |
| `createdAt` | DateTime | Server timestamp (auto-generated) |

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:offresq
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console (optional)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### Switching to MySQL (Optional)

Update `pom.xml` to include MySQL:
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/offresq_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

## Troubleshooting

### ESP32-S3 can't connect to server
1. ✅ Check WiFi credentials are correct
2. ✅ Verify PC IP address is correct: `ipconfig` (Windows) or `ifconfig` (Linux/Mac)
3. ✅ Ensure both devices are on the same network
4. ✅ Check Windows Firewall: allow Java through firewall
5. ✅ Try disabling VPN temporarily

### No locations appearing on map
1. ✅ Check ESP32 serial output for errors
2. ✅ Verify `/api/locations` returns data: `curl http://localhost:8080/api/locations`
3. ✅ Check browser console for JavaScript errors (F12)
4. ✅ Try clearing cache and refreshing (Ctrl+Shift+R)

### Server won't start
1. ✅ Ensure Java 21+ is installed: `java -version`
2. ✅ Check if port 8080 is already in use
3. ✅ Change port in `application.properties`: `server.port=8081`

## Example curl Commands

```bash
# Send location data
curl -X POST http://localhost:8080/api/location \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "DEVICE_001",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "alert": 0,
    "rssi": -85,
    "timestamp": 1234567890000
  }'

# Get all locations
curl http://localhost:8080/api/locations

# Get locations for a device
curl http://localhost:8080/api/locations/DEVICE_001

# Delete a location
curl -X DELETE http://localhost:8080/api/location/1

# Clear all locations
curl -X DELETE http://localhost:8080/api/locations/clear
```

## Performance Notes

- **In-Memory Database:** H2 stores data in RAM (clears on restart)
- **Auto-refresh:** Map updates every 3 seconds
- **Concurrent Devices:** Supports multiple ESP32-S3 devices simultaneously
- **Scalability:** For production, switch to MySQL/PostgreSQL and add indexing

## Future Enhancements

- 📊 Historical data tracking and analytics
- 🛣️ Route visualization with polylines
- ⏱️ Timestamp filtering and date range selection
- 🔐 Authentication and user management
- 📱 Mobile-responsive UI
- 💾 Export data to CSV/JSON
- 🎨 Custom markers and clustering
- 📡 WebSocket for true real-time updates

## License

This project is part of the OffResq system.

## Support

For issues or questions:
1. Check the Arduino serial output
2. Verify network connectivity
3. Check browser console for errors (F12)
4. Review server logs in terminal

---

**Happy tracking! 📍🗺️**
