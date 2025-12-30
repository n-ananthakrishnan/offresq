package com.offresq.gateway.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.offresq.gateway.dto.LocationResponse;

@Component
public class LocationBroadcaster {

  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter register() {
    SseEmitter emitter = new SseEmitter(0L); // no timeout
    emitters.add(emitter);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));

    return emitter;
  }

  public void publish(LocationResponse location) {
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name("location").data(location));
      } catch (IOException e) {
        emitters.remove(emitter);
      }
    }
  }
}