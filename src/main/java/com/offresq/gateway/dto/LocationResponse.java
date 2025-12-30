package com.offresq.gateway.dto;

import java.time.Instant;

public record LocationResponse(
    Long id,
    String deviceId,
    double latitude,
    double longitude,
    int alert,
    Integer rssi,
    Long deviceTimestampMs,
    Instant receivedAt
) {}