# OffResq GPS Gateway - Quick Reference Card

## 🚀 START HERE

### 1. Run Server (Pick One)
```bash
# Windows - Batch Script
run.bat

# Windows/Mac/Linux - PowerShell
./run.ps1

# Manual Maven
mvn spring-boot:run
```

### 2. Update ESP32 Code
```c
const char* ssid = "YOUR_WIFI";              // WiFi name
const char* password = "YOUR_PASSWORD";      // WiFi password
const char* serverURL = "http://YOUR_IP:8080/api/location";  // PC IP!
```

**Find Your IP:**
```bash
# Windows Command Prompt
ipconfig

# Linux/Mac Terminal
hostname -I
```

### 3. Open Browser
```
http://localhost:8080/
```

---

## 📍 URLs & Links

| URL | What |
|-----|------|
| `http://localhost:8080/` | 🗺️ Interactive Map |
| `http://localhost:8080/api-test` | 🧪 API Testing |
| `http://localhost:8080/h2-console` | 💾 Database View |
| `http://localhost:8080/api/locations` | 📊 Raw JSON Data |

---

## 🔌 API Quick Commands

```bash
# Send GPS data (from ESP32 or curl)
curl -X POST http://localhost:8080/api/location \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId":"DEVICE_001",
    "latitude":37.7749,
    "longitude":-122.4194,
    "alert":0,
    "rssi":-85,
    "timestamp":'$(date +%s000)'
  }'

# Get all locations
curl http://localhost:8080/api/locations

# Get specific device
curl http://localhost:8080/api/locations/DEVICE_001

# Delete all (careful!)
curl -X DELETE http://localhost:8080/api/locations/clear
```

---

## 📚 Documentation Map

```
START HERE ──────────────┐
                         ▼
        ┌────────────────────────────────────┐
        │   PROJECT_SUMMARY.md (5 min)       │
        │   • Overview                       │
        │   • Quick start                    │
        └────────────────────────────────────┘
                    │           │
        ┌──────────┴──┬────────┴──────────┐
        ▼             ▼                   ▼
    ┌─────────┐  ┌──────────┐  ┌──────────────┐
    │SETUP    │  │ESP32     │  │IMPLEMENTATION
    │GUIDE.md │  │CONFIG.md │  │CHECKLIST.md  
    │Server   │  │Hardware  │  │What's done   
    │setup    │  │config    │  │             
    └─────────┘  └──────────┘  └──────────────┘
        │
        ▼
    READY TO USE! 🚀
```

---

## 🎯 Common Tasks

### Test API Without Hardware
1. Open: `http://localhost:8080/api-test`
2. Click a location (San Francisco, London, etc.)
3. Click **Send Request**
4. Check map: `http://localhost:8080/`

### Clear All Data
```bash
curl -X DELETE http://localhost:8080/api/locations/clear
```

### View Database
```
http://localhost:8080/h2-console
```
- JDBC URL: `jdbc:h2:mem:offresq`
- Username: `sa`
- No password

### Change Server Port
Edit `src/main/resources/application.properties`:
```properties
server.port=8081  # or any port you like
```

---

## 🐛 Troubleshooting (Top 5)

| Problem | Fix |
|---------|-----|
| Can't connect ESP32 to server | Use correct PC IP from `ipconfig` |
| Port 8080 already in use | Change `server.port` in application.properties |
| No markers on map | Check `/api/locations` endpoint returns data |
| Build fails | Ensure Java 21+: `java -version` |
| WiFi won't connect on ESP32 | Check SSID/password are correct (case-sensitive) |

---

## 📋 What You Have

```
✅ Spring Boot REST API
✅ Interactive Leaflet.js Map
✅ H2 In-Memory Database
✅ Real-time GPS Tracking
✅ Emergency Alert System
✅ API Test Console
✅ Full Documentation
✅ Build & Run Scripts
```

---

## 🎓 File You MUST Read

### If You Have 5 Minutes
→ **PROJECT_SUMMARY.md**

### If You Have 15 Minutes
→ **SETUP_GUIDE.md**

### Before Using ESP32
→ **ESP32_CONFIG_GUIDE.md**

### To Verify Everything Works
→ **IMPLEMENTATION_CHECKLIST.md**

---

## 🔑 Key Concepts

**GPS Data Fields:**
- `latitude` (-90 to 90)
- `longitude` (-180 to 180)
- `alert` (0=normal, 1=emergency)
- `rssi` (signal strength)
- `timestamp` (milliseconds)

**Map Colors:**
- 🟢 **Green** = Normal (alert=0)
- 🔴 **Red** = Emergency (alert=1)

**Update Rate:**
- Auto-refresh every 3 seconds
- Data persists until deleted/restart

---

## 💻 System Requirements

- Java 21+
- Maven 3.8+
- Modern Web Browser
- WiFi (for ESP32)
- 100MB free disk space

---

## 🎯 Success Indicators

- [ ] Server runs without errors
- [ ] Browser opens to `http://localhost:8080/`
- [ ] API test page loads (`/api-test`)
- [ ] Can send test data
- [ ] Markers appear on map
- [ ] Click marker shows details
- [ ] ESP32 connects to WiFi
- [ ] GPS data appears in real-time

---

## 📞 Quick Support

### "Server won't start"
```bash
# Check Java version
java -version

# Check Maven
mvn -version

# Try building first
mvn clean compile
```

### "ESP32 can't connect"
```bash
# Find PC IP (Windows)
ipconfig | findstr IPv4

# Find PC IP (Linux/Mac)
hostname -I

# Verify it's correct in code
const char* serverURL = "http://192.168.x.x:8080/api/location";
```

### "No data on map"
```bash
# Check if data is saved
curl http://localhost:8080/api/locations

# Check database
http://localhost:8080/h2-console

# Try test console
http://localhost:8080/api-test
```

---

## 🚀 Production Checklist

- [ ] Test with real ESP32 hardware
- [ ] Verify WiFi connectivity
- [ ] Test emergency alert feature
- [ ] Validate GPS accuracy
- [ ] Check map responsiveness
- [ ] Test with multiple devices
- [ ] Verify data persistence
- [ ] Test API rate limits
- [ ] Document any customizations
- [ ] Backup configuration

---

## 🎉 You're Ready!

1. **Run:** `run.bat` (Windows) or `./run.ps1`
2. **Wait:** Server starts (5-10 seconds)
3. **Update:** ESP32 code with your IP
4. **Upload:** Arduino sketch to device
5. **Open:** `http://localhost:8080/`
6. **Track:** GPS locations in real-time! 📍

---

## 📖 Quick Links

```
Project Overview:     PROJECT_SUMMARY.md
Installation Guide:   SETUP_GUIDE.md
Hardware Setup:       ESP32_CONFIG_GUIDE.md
Checklist:           IMPLEMENTATION_CHECKLIST.md
File Reference:      FILES_REFERENCE.md
This Card:           README_QUICK_START.md
```

---

## ⚡ One-Minute Summary

You now have a **complete Spring Boot GPS tracking system** that:

1. **Receives** GPS data from ESP32-S3 via HTTP
2. **Stores** locations in H2 database
3. **Displays** on interactive Leaflet.js map
4. **Updates** in real-time (every 3 seconds)
5. **Alerts** with red markers for emergencies

**To start:** Run `run.bat` → Update ESP32 code → Open `localhost:8080` → Done! 🎉

---

**Version:** 1.0.0  
**Status:** ✅ Ready to Deploy  
**Last Updated:** December 30, 2025

```
Happy tracking! 📍🗺️✨
```
