# OffResq GPS Gateway - Project Summary

## 🎯 What Was Created

A complete Spring Boot backend system that:
1. **Receives GPS data** from ESP32-S3 devices via HTTP POST requests
2. **Stores locations** in an H2 in-memory database
3. **Visualizes coordinates** on an interactive Leaflet.js map in real-time
4. **Tracks multiple devices** simultaneously with alert status indicators

---

## 📁 Project Structure

```
gateway/
├── pom.xml                                    # Maven dependencies
├── run.bat                                    # Windows quick start script
├── run.ps1                                    # PowerShell quick start script
├── SETUP_GUIDE.md                            # Detailed setup instructions
├── ESP32_CONFIG_GUIDE.md                     # ESP32-S3 configuration guide
├── HELP.md                                    # Original Spring Boot help
│
├── src/main/java/com/offresq/gateway/
│   ├── GatewayApplication.java               # Spring Boot main class
│   ├── controller/
│   │   ├── LocationController.java           # REST API endpoints
│   │   └── ViewController.java                # HTML route handler
│   ├── model/
│   │   └── Location.java                     # JPA entity model
│   └── repository/
│       └── LocationRepository.java           # Database access layer
│
├── src/main/resources/
│   ├── application.properties                 # Server & DB configuration
│   └── templates/
│       └── index.html                         # Interactive map UI
│
└── src/test/                                  # Unit tests
```

---

## 🚀 Quick Start (3 Steps)

### Step 1: Run the Server
```bash
# Option A: Using batch script (Windows)
run.bat

# Option B: Using PowerShell (Windows/Mac/Linux)
./run.ps1

# Option C: Manual Maven command
mvn clean package
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

### Step 2: Configure ESP32-S3
Update the Arduino sketch with:
```c
const char* ssid = "YOUR_WIFI_NAME";
const char* password = "YOUR_WIFI_PASSWORD";
const char* serverURL = "http://YOUR_PC_IP:8080/api/location";  // ← Your actual IP!
const char* deviceID = "DEVICE_001";
```

### Step 3: Open the Map
Navigate to: **http://localhost:8080**

---

## 🔌 API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/location` | Receive GPS data from ESP32 |
| GET | `/api/locations` | Get all stored locations |
| GET | `/api/locations/{deviceId}` | Get locations for specific device |
| GET | `/api/location/latest` | Get most recent location |
| DELETE | `/api/location/{id}` | Delete specific location |
| DELETE | `/api/locations/clear` | Clear all locations |

### Example Request
```bash
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
```

---

## 🗺️ Web UI Features

### Interactive Map
- 🟢 **Green markers** = Normal status (alert = 0)
- 🔴 **Red markers** = Emergency alert (alert = 1)
- Click markers to view detailed information
- Zoom, pan, and navigate freely

### Control Panel (Top Right)
- **🔄 Refresh** - Manual location update
- **📍 Center Map** - Center on latest location
- **🗑️ Clear All** - Delete all stored locations
- **Location Counter** - Shows total GPS points

### Information Panel (Bottom Right)
Shows when you click a marker:
- Device ID
- Latitude & Longitude (6 decimals)
- Signal Strength (RSSI)
- Timestamp
- Alert Status

### Auto-Refresh
Map updates automatically every 3 seconds

---

## 📊 Database Schema

### Locations Table
```sql
CREATE TABLE locations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id VARCHAR(255) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  alert INT NOT NULL,
  rssi INT NOT NULL,
  timestamp BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📦 Technologies Used

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Backend | Spring Boot 4.0.1 | REST API server |
| Database | H2 + JPA | Data persistence |
| Frontend | Leaflet.js + OpenStreetMap | Interactive mapping |
| Build Tool | Maven | Project compilation |
| Language | Java 21 | Backend implementation |

---

## ⚙️ Configuration Files

### application.properties
```properties
server.port=8080                                 # Server port
spring.datasource.url=jdbc:h2:mem:offresq      # In-memory database
spring.jpa.hibernate.ddl-auto=create-drop      # Auto-create tables
spring.h2.console.enabled=true                 # H2 web console access
spring.h2.console.path=/h2-console             # Access at localhost:8080/h2-console
```

### pom.xml Dependencies
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database access
- `h2` - In-memory database
- `lombok` - Code generation
- `spring-boot-devtools` - Live reload

---

## 🔧 Key Classes

### LocationController
Handles all HTTP requests:
- `POST /api/location` - Save new GPS data
- `GET /api/locations` - Retrieve all locations
- `GET /api/locations/{deviceId}` - Filter by device
- `DELETE` endpoints for cleanup

### Location Entity
Represents GPS data with fields:
```java
- id (Long)
- deviceId (String)
- latitude (Double)
- longitude (Double)
- alert (Integer)
- rssi (Integer)
- timestamp (Long)
- createdAt (LocalDateTime)
```

### LocationRepository
JPA interface for database operations:
- `findAllByOrderByCreatedAtDesc()` - All locations, newest first
- `findByDeviceIdOrderByCreatedAtDesc()` - Filter by device

---

## 🎨 Frontend (index.html)

### Technologies
- **Leaflet.js v1.9.4** - Interactive maps
- **OpenStreetMap** - Free map tiles
- **Bootstrap 5** - Responsive styling
- **Vanilla JavaScript** - No framework needed

### Key Functions
- `loadLocations()` - Fetch data from API
- `updateMapAndInfo()` - Render markers on map
- `showInfoPanel()` - Display location details
- `refreshLocations()` - Manual update
- Auto-refresh every 3 seconds

---

## 🐛 Troubleshooting

### Build Issues
```bash
# Clean and rebuild
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Update dependencies
mvn dependency:resolve
```

### Runtime Issues

| Problem | Solution |
|---------|----------|
| Port 8080 in use | Change in `application.properties`: `server.port=8081` |
| ESP32 can't connect | Check IP address: `ipconfig` (Windows), `hostname -I` (Linux) |
| No locations on map | Verify `/api/locations` returns data via curl |
| Build fails | Ensure Java 21+ installed: `java -version` |

---

## 📚 Documentation

| File | Contents |
|------|----------|
| `SETUP_GUIDE.md` | Complete installation & API documentation |
| `ESP32_CONFIG_GUIDE.md` | ESP32-S3 Arduino code setup instructions |
| `HELP.md` | Original Spring Boot generated help |

---

## 🚢 Deployment Options

### Local Development
```bash
mvn spring-boot:run
```

### Packaged JAR
```bash
mvn clean package
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

### Docker (Optional Future Enhancement)
```dockerfile
FROM openjdk:21
COPY target/gateway-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## 💾 Data Persistence

### Current Setup (H2 In-Memory)
- ✅ Fast for development
- ✅ No configuration needed
- ❌ Data lost on restart
- ❌ Not suitable for production

### For Production (MySQL)
Update `pom.xml`:
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/offresq
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

---

## 🎓 Learning Resources

- Spring Boot: https://spring.io/projects/spring-boot
- Leaflet.js: https://leafletjs.com/
- JPA: https://spring.io/projects/spring-data-jpa
- Maven: https://maven.apache.org/guides/

---

## ✨ Next Steps

1. ✅ Update ESP32 code with your WiFi & IP
2. ✅ Run `run.bat` or `run.ps1`
3. ✅ Upload Arduino sketch to ESP32-S3
4. ✅ Open http://localhost:8080
5. ✅ Watch GPS locations appear in real-time!

---

## 📝 Notes

- **Auto-refresh** happens every 3 seconds
- **Markers stay on map** until you delete them
- **Green = Normal**, **Red = Emergency Alert**
- **H2 console** available at `/h2-console` for database inspection
- **CORS enabled** for cross-origin requests

---

## 🎉 You're All Set!

Your Spring Boot GPS tracking system is ready to receive and visualize real-time location data from ESP32-S3 devices!

**Questions or issues?** Check the detailed guides:
- 📖 `SETUP_GUIDE.md` - Complete documentation
- 🔧 `ESP32_CONFIG_GUIDE.md` - Hardware setup

**Happy tracking! 📍🗺️**
