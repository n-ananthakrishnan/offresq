package com.offresq.gateway.controller;

import com.offresq.gateway.dto.DeviceStatusResponse;
import com.offresq.gateway.dto.LocationIngestRequest;
import com.offresq.gateway.dto.LocationResponse;
import com.offresq.gateway.service.LocationService;
import com.offresq.gateway.service.RouteOptimizer;
import com.offresq.gateway.service.RoadRoutingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationController {

  private final LocationService locationService;
  private final RoadRoutingService roadRoutingService;

  /**
   * Compatible with your ESP32 receiver (it posts to /api/location).
   */
  @PostMapping({"/location", "/locations"})
  public ResponseEntity<LocationResponse> receive(@Valid @RequestBody LocationIngestRequest payload) {
    LocationResponse saved = locationService.ingest(payload);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * GET /api/locations?limit=200
   * GET /api/locations?deviceId=DEVICE_001&limit=200
   */
  @GetMapping("/locations")
  public List<LocationResponse> getLocations(
      @RequestParam Optional<String> deviceId,
      @RequestParam(defaultValue = "200") int limit
  ) {
    return locationService.getLocations(deviceId, limit);
  }

  /**
   * GET /api/locations/latest
   * GET /api/locations/latest?deviceId=DEVICE_001
   */
  @GetMapping("/locations/latest")
  public ResponseEntity<?> latest(@RequestParam Optional<String> deviceId) {
    return locationService.latest(deviceId)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No locations available")));
  }

  /**
   * Sidebar devices view:
   * GET /api/devices?scanRows=1500
   */
  @GetMapping("/devices")
  public List<DeviceStatusResponse> devices(@RequestParam(defaultValue = "1500") int scanRows) {
    return locationService.deviceStatuses(scanRows);
  }

  @DeleteMapping("/locations/{id}")
  public ResponseEntity<?> delete(@PathVariable long id) {
    locationService.delete(id);
    return ResponseEntity.ok(Map.of("message", "Deleted", "id", id));
  }

  @DeleteMapping("/locations")
  public ResponseEntity<?> clearAll() {
    locationService.clearAll();
    return ResponseEntity.ok(Map.of("message", "All locations cleared"));
  }

  /**
   * Optimize route from a starting point through multiple devices.
   * POST /api/route/optimize with JSON body:
   * {
   *   "startLatitude": 40.7128,
   *   "startLongitude": -74.0060,
   *   "deviceIds": ["DEVICE_001", "DEVICE_002", "DEVICE_003"]
   * }
   */
  @PostMapping("/route/optimize")
  public ResponseEntity<?> optimizeRoute(@RequestBody Map<String, Object> request) {
    try {
      double startLat = ((Number) request.get("startLatitude")).doubleValue();
      double startLon = ((Number) request.get("startLongitude")).doubleValue();
      
      @SuppressWarnings("unchecked")
      List<String> deviceIds = (List<String>) request.get("deviceIds");
      
      if (deviceIds == null || deviceIds.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "deviceIds required"));
      }

      // Get latest locations for each device
      List<Map<String, Double>> waypoints = new ArrayList<>();
      for (String deviceId : deviceIds) {
        Optional<LocationResponse> loc = locationService.latest(Optional.of(deviceId));
        if (loc.isPresent()) {
          LocationResponse l = loc.get();
          Map<String, Double> wp = new HashMap<>();
          wp.put("deviceId", (double) 0); // placeholder, we'll set string version in result
          wp.put("latitude", l.latitude());
          wp.put("longitude", l.longitude());
          
          // Store deviceId as a field for reference
          waypoints.add(wp);
        }
      }

      // Optimize route
      List<Map<String, Object>> optimizedRoute = RouteOptimizer.optimizeRoute(startLat, startLon, waypoints);
      
      // Add deviceIds back to the results
      for (int i = 0; i < optimizedRoute.size(); i++) {
        if (i < deviceIds.size()) {
          optimizedRoute.get(i).put("deviceId", deviceIds.get(i));
        }
      }

      double totalDistance = optimizedRoute.isEmpty() ? 0 : 
          ((Number) optimizedRoute.get(optimizedRoute.size() - 1).get("cumulativeDistance")).doubleValue();

      return ResponseEntity.ok(Map.of(
          "startLatitude", startLat,
          "startLongitude", startLon,
          "optimizedRoute", optimizedRoute,
          "totalDistance", totalDistance,
          "waypointsCount", optimizedRoute.size()
      ));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Get real road route between two coordinates.
   * POST /api/route/road with JSON body:
   * {
   *   "startLatitude": 40.7128,
   *   "startLongitude": -74.0060,
   *   "endLatitude": 40.7580,
   *   "endLongitude": -73.9855
   * }
   */
  @PostMapping("/route/road")
  public ResponseEntity<?> getRoadRoute(@RequestBody Map<String, Object> request) {
    try {
      double startLat = ((Number) request.get("startLatitude")).doubleValue();
      double startLon = ((Number) request.get("startLongitude")).doubleValue();
      double endLat = ((Number) request.get("endLatitude")).doubleValue();
      double endLon = ((Number) request.get("endLongitude")).doubleValue();

      Map<String, Object> route = roadRoutingService.getRoute(startLat, startLon, endLat, endLon);
      return ResponseEntity.ok(route);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Inject test data for demonstration.
   * GET /api/test-data
   */
  @GetMapping("/test-data")
  public ResponseEntity<?> injectTestData() {
    try {
      // Sample rescue victim locations
      double[][] locations = {
        {40.7128, -74.0060}, // New York
        {34.0522, -118.2437}, // Los Angeles
        {41.8781, -87.6298}, // Chicago
        {29.7604, -95.3698}, // Houston
        {33.7490, -84.3880}, // Atlanta
      };

      String[] deviceIds = {"RESCUE_001", "RESCUE_002", "RESCUE_003", "RESCUE_004", "RESCUE_005"};
      boolean[] alerts = {true, false, true, false, true};

      List<LocationResponse> injected = new ArrayList<>();
      for (int i = 0; i < locations.length; i++) {
        LocationIngestRequest req = new LocationIngestRequest(
            deviceIds[i],
            locations[i][0],
            locations[i][1],
            alerts[i] ? "EMERGENCY" : "NORMAL",
            -65 + i,
            System.currentTimeMillis()
        );
        injected.add(locationService.ingest(req));
      }

      return ResponseEntity.ok(Map.of("message", "Test data injected", "count", injected.size(), "devices", injected));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
