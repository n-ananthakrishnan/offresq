package com.offresq.gateway.dto;

import java.time.Instant;

public record DeviceStatusResponse(
    String deviceId,
    int alert,
    double latitude,
    double longitude,
    Integer rssi,
    Instant receivedAt
) {}