package com.offresq.gateway.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for fetching road routes from OpenRouteService API.
 * Provides actual driving/walking routes instead of straight lines.
 */
@Service
public class RoadRoutingService {

  private static final String ORS_API_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
  private static final Logger log = LoggerFactory.getLogger(RoadRoutingService.class);
  
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Get route coordinates between two points via road.
   * 
   * @param startLat Starting latitude
   * @param startLon Starting longitude
   * @param endLat Ending latitude
   * @param endLon Ending longitude
   * @return Map with route path (list of [lon, lat] coordinates), distance in km, and duration in seconds
   */
  public Map<String, Object> getRoute(double startLat, double startLon, double endLat, double endLon) {
    try {
      // Build URL: ORS expects coordinates as [lon,lat]
      String coordinates = String.format("%f,%f|%f,%f", startLon, startLat, endLon, endLat);
      String url = ORS_API_URL;
      String jsonBody = String.format("{\"coordinates\":[[%f,%f],[%f,%f]]}", startLon, startLat, endLon, endLat);

      log.info("Requesting route from OpenRouteService: {}", url);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .header("Authorization", "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjVlNDJhYzgwMDdlYzQ3MzE5ZTdhNmE2ZDdlY2RiOGRjIiwiaCI6Im11cm11cjY0In0=")
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      log.info("ORS Response status: {}", response.statusCode());
      
      if (response.statusCode() != 200) {
        log.warn("ORS returned error status: {}", response.statusCode());
        log.debug("Response body: {}", response.body());
        return Map.of("error", "Route not found (status " + response.statusCode() + ")");
      }

      JsonNode root = objectMapper.readTree(response.body());
      JsonNode routes = root.path("routes");
      
      if (!routes.isArray() || routes.size() == 0) {
        log.warn("No routes found in ORS response");
        return Map.of("error", "No route found");
      }

      JsonNode route = routes.get(0);

      // Extract route geometry (list of [lon, lat] coordinates)
      List<List<Double>> geometry = new ArrayList<>();
      JsonNode geom = route.path("geometry").path("coordinates");
      
      if (!geom.isArray()) {
        log.warn("Geometry is not an array or missing");
        return Map.of("error", "Invalid geometry data");
      }
      
      for (JsonNode coord : geom) {
        if (coord.isArray() && coord.size() >= 2) {
          List<Double> point = new ArrayList<>();
          point.add(coord.get(0).asDouble()); // lon
          point.add(coord.get(1).asDouble()); // lat
          geometry.add(point);
        }
      }

      log.info("Extracted {} coordinate points from route", geometry.size());

      // Extract summary data
      JsonNode summary = route.path("summary");
      double distanceKm = summary.path("distance").asDouble(0) / 1000.0; // Convert meters to km
      double durationSeconds = summary.path("duration").asDouble(0);

      log.info("Route distance: {} km, duration: {} seconds", distanceKm, durationSeconds);

      return Map.of(
          "path", geometry,
          "distanceKm", distanceKm,
          "durationSeconds", durationSeconds,
          "durationMinutes", Math.round(durationSeconds / 60.0 * 10.0) / 10.0
      );

    } catch (IOException e) {
      log.error("IOException fetching route:", e);
      return Map.of("error", "IO error: " + e.getMessage());
    } catch (InterruptedException e) {
      log.error("InterruptedException fetching route:", e);
      Thread.currentThread().interrupt();
      return Map.of("error", "Request interrupted");
    } catch (Exception e) {
      log.error("Unexpected error fetching route:", e);
      return Map.of("error", "Failed to fetch route: " + e.getMessage());
    }
  }

  // Multi-stop optimization removed. Only single-stop road routing is supported.
}
