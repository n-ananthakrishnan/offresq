package com.offresq.gateway.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "locations",
    indexes = {
        @Index(name = "idx_locations_device_received", columnList = "device_id, received_at"),
        @Index(name = "idx_locations_received", columnList = "received_at")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_id", nullable = false, length = 64)
  private String deviceId;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  /**
   * 0 = normal, 1 = emergency
   */
  @Column(nullable = false)
  private int alert;

  @Column(nullable = true)
  private Integer rssi;

  /**
   * Timestamp sent by device/gateway in milliseconds (ESP32 uses millis()).
   * It's useful for debugging ordering, but not absolute time.
   */
  @Column(name = "device_timestamp_ms", nullable = true)
  private Long deviceTimestampMs;

  /**
   * Real server receive time.
   */
  @CreationTimestamp
  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;
}