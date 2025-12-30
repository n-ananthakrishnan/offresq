package com.offresq.gateway.service;

import com.offresq.gateway.dto.DeviceStatusResponse;
import com.offresq.gateway.dto.LocationIngestRequest;
import com.offresq.gateway.dto.LocationResponse;
import com.offresq.gateway.model.Location;
import com.offresq.gateway.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final LocationBroadcaster broadcaster;

  public LocationResponse ingest(LocationIngestRequest req) {
    Location toSave = Location.builder()
        .deviceId(req.deviceId())
        .latitude(req.latitude())
        .longitude(req.longitude())
        .alert(req.alert())
        .rssi(req.rssi())
        .deviceTimestampMs(req.timestamp())
        .build();

    Location saved = locationRepository.save(toSave);
    LocationResponse response = toResponse(saved);

    broadcaster.publish(response);
    return response;
  }

  public List<LocationResponse> getLocations(Optional<String> deviceId, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 1000));
    var pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "receivedAt"));

    List<Location> rows = deviceId
        .map(id -> locationRepository.findByDeviceId(id, pageable))
        .orElseGet(() -> locationRepository.findAllByOrderByReceivedAtDesc(pageable));

    return rows.stream().map(this::toResponse).toList();
  }

  public Optional<LocationResponse> latest(Optional<String> deviceId) {
    return deviceId
        .map(locationRepository::findTopByDeviceIdOrderByReceivedAtDesc)
        .orElseGet(locationRepository::findTopByOrderByReceivedAtDesc)
        .map(this::toResponse);
  }

  /**
   * Simple “device list” by taking the newest location per device
   * from the most recent N rows.
   */
  public List<DeviceStatusResponse> deviceStatuses(int scanRows) {
    int safeScan = Math.max(50, Math.min(scanRows, 5000));
    var pageable = PageRequest.of(0, safeScan, Sort.by(Sort.Direction.DESC, "receivedAt"));
    List<Location> recent = locationRepository.findAllByOrderByReceivedAtDesc(pageable);

    Map<String, Location> latestPerDevice = new LinkedHashMap<>();
    for (Location loc : recent) {
      latestPerDevice.putIfAbsent(loc.getDeviceId(), loc);
    }

    return latestPerDevice.values().stream()
        .map(l -> new DeviceStatusResponse(
            l.getDeviceId(),
            l.getAlert(),
            l.getLatitude(),
            l.getLongitude(),
            l.getRssi(),
            l.getReceivedAt()
        ))
        .toList();
  }

  public void delete(long id) {
    locationRepository.deleteById(id);
  }

  public void clearAll() {
    locationRepository.deleteAllInBatch();
  }

  private LocationResponse toResponse(Location l) {
    return new LocationResponse(
        l.getId(),
        l.getDeviceId(),
        l.getLatitude(),
        l.getLongitude(),
        l.getAlert(),
        l.getRssi(),
        l.getDeviceTimestampMs(),
        l.getReceivedAt()
    );
  }
}