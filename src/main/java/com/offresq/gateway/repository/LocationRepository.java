package com.offresq.gateway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.offresq.gateway.model.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

  List<Location> findByDeviceId(String deviceId, Pageable pageable);

  List<Location> findAllByOrderByReceivedAtDesc(Pageable pageable);

  Optional<Location> findTopByOrderByReceivedAtDesc();

  Optional<Location> findTopByDeviceIdOrderByReceivedAtDesc(String deviceId);
}