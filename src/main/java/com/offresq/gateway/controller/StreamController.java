package com.offresq.gateway.controller;

import com.offresq.gateway.service.LocationBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class StreamController {

  private final LocationBroadcaster broadcaster;

  @GetMapping("/api/stream/locations")
  public SseEmitter streamLocations() {
    return broadcaster.register();
  }
}