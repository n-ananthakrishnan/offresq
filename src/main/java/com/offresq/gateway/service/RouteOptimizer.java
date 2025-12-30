package com.offresq.gateway.service;

import java.util.*;

/**
 * Simple TSP (Traveling Salesman Problem) route optimizer using nearest neighbor heuristic.
 * Optimizes routes from a starting point through multiple waypoints.
 */
public class RouteOptimizer {

  private static final double EARTH_RADIUS_KM = 6371.0;

  /**
   * Calculate distance between two coordinates in kilometers (Haversine formula).
   */
  public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
    double toRad = Math.PI / 180.0;
    double dLat = toRad * (lat2 - lat1);
    double dLon = toRad * (lon2 - lon1);
    
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
               Math.cos(toRad * lat1) * Math.cos(toRad * lat2) *
               Math.sin(dLon / 2) * Math.sin(dLon / 2);
    
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  }

  /**
   * Optimize route using nearest neighbor heuristic.
   * Starts from (startLat, startLon) and visits all waypoints in optimized order.
   * 
   * @param startLat Starting latitude
   * @param startLon Starting longitude
   * @param waypoints List of waypoints with deviceId, latitude, longitude
   * @return Optimized route as list of waypoint objects with order and cumulative distance
   */
  public static List<Map<String, Object>> optimizeRoute(
      double startLat, 
      double startLon, 
      List<Map<String, Double>> waypoints) {
    
    if (waypoints == null || waypoints.isEmpty()) {
      return Collections.emptyList();
    }

    // Mark all as unvisited
    List<Integer> unvisited = new ArrayList<>();
    for (int i = 0; i < waypoints.size(); i++) {
      unvisited.add(i);
    }

    List<Map<String, Object>> route = new ArrayList<>();
    double currentLat = startLat;
    double currentLon = startLon;
    double totalDistance = 0;

    // Nearest neighbor: always go to the nearest unvisited waypoint
    while (!unvisited.isEmpty()) {
      int nearestIdx = -1;
      double minDist = Double.MAX_VALUE;

      for (int idx : unvisited) {
        Map<String, Double> wp = waypoints.get(idx);
        double dist = distanceKm(currentLat, currentLon, wp.get("latitude"), wp.get("longitude"));
        if (dist < minDist) {
          minDist = dist;
          nearestIdx = idx;
        }
      }

      if (nearestIdx >= 0) {
        Map<String, Double> wp = waypoints.get(nearestIdx);
        totalDistance += minDist;

        Map<String, Object> step = new HashMap<>();
        step.put("order", route.size() + 1);
        step.put("deviceId", wp.get("deviceId"));
        step.put("latitude", wp.get("latitude"));
        step.put("longitude", wp.get("longitude"));
        step.put("distanceFromPrevious", minDist);
        step.put("cumulativeDistance", totalDistance);

        route.add(step);

        currentLat = wp.get("latitude");
        currentLon = wp.get("longitude");
        unvisited.remove(Integer.valueOf(nearestIdx));
      }
    }

    return route;
  }
}
