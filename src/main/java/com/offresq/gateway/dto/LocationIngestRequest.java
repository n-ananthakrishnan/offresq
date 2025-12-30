package com.offresq.gateway.dto;

import jakarta.validation.constraints.*;

public record LocationIngestRequest(

    @NotBlank
    @Size(max = 64)
    String deviceId,

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    Double latitude,

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    Double longitude,

    @NotNull
    @Min(0)
    @Max(1)
    Integer alert,

    Integer rssi,

    Long timestamp
) {}