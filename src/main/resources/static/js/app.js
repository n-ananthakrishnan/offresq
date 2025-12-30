const API = {
  devices: "/api/devices",
  locations: "/api/locations",
  clearAll: "/api/locations",
  stream: "/api/stream/locations",
  optimizeRoute: "/api/route/optimize",
  roadRoute: "/api/route/road",
  optimizedRoadRoute: "/api/route/road/optimized"
};

let map;
let rescueMarker = null;
let victimMarkers = new Map();     // deviceId -> Leaflet marker
let latestByDevice = new Map();    // deviceId -> latest location (DeviceStatusResponse-like)
let selectedDeviceId = null;
let routeLine = null;
let selectedDevices = new Set();   // Multiple device selection for route optimization
let optimizedRouteLines = [];      // Polylines for optimized route

function $(id) { return document.getElementById(id); }

function fmtTime(iso) {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const toRad = d => d * Math.PI / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon/2) * Math.sin(dLon/2);
  return 2 * R * Math.asin(Math.sqrt(a));
}

function markerIcon(color) {
  return L.divIcon({
    className: "offresq-marker",
    html: `<div style="
      width: 16px; height: 16px; border-radius: 50%;
      background: ${color};
      border: 3px solid white;
      box-shadow: 0 2px 10px rgba(0,0,0,0.35);
    "></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  });
}

function initMap() {
  map = L.map("map", { zoomControl: true }).setView([20, 0], 2);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap contributors"
  }).addTo(map);

  map.on("click", (e) => {
    setRescueStart(e.latlng.lat, e.latlng.lng);
  });
}

function setRescueStart(lat, lon) {
  if (rescueMarker) map.removeLayer(rescueMarker);

  rescueMarker = L.marker([lat, lon], {
    icon: markerIcon("#6ea8fe"),
    draggable: true
  }).addTo(map);

  rescueMarker.bindPopup(`<b>Rescue Start</b><br/>${lat.toFixed(6)}, ${lon.toFixed(6)}`).openPopup();

  rescueMarker.on("dragend", () => {
    const p = rescueMarker.getLatLng();
    updateRouteUI(p.lat, p.lng);
  });

  updateRouteUI(lat, lon);
}

function clearRescueStart() {
  if (rescueMarker) map.removeLayer(rescueMarker);
  rescueMarker = null;
  if (routeLine) map.removeLayer(routeLine);
  routeLine = null;
  clearOptimizedRoute();

  $("routeStart").textContent = "—";
  $("routeDistance").textContent = "—";
}

function selectDevice(deviceId) {
  selectedDeviceId = deviceId;
  $("selectedDevice").textContent = deviceId;

  const loc = latestByDevice.get(deviceId);
  if (!loc) return;

  const m = victimMarkers.get(deviceId);
  if (m) {
    map.setView(m.getLatLng(), 15);
    m.openPopup();
  }

  updateRouteUI();
}

function updateRouteUI(forcedRescueLat, forcedRescueLon) {
  const rescue = rescueMarker ? rescueMarker.getLatLng() : null;
  const rLat = forcedRescueLat ?? (rescue ? rescue.lat : null);
  const rLon = forcedRescueLon ?? (rescue ? rescue.lng : null);

  if (rLat != null && rLon != null) {
    $("routeStart").textContent = `${rLat.toFixed(5)}, ${rLon.toFixed(5)}`;
  }

  if (!selectedDeviceId) {
    $("routeEnd").textContent = "—";
    $("routeDistance").textContent = "—";
    if (routeLine) map.removeLayer(routeLine);
    routeLine = null;
    return;
  }

  const victim = latestByDevice.get(selectedDeviceId);
  if (!victim) return;

  $("routeEnd").textContent = `${victim.latitude.toFixed(5)}, ${victim.longitude.toFixed(5)}`;

  if (rLat == null || rLon == null) {
    $("routeDistance").textContent = "Set rescue start on map";
    if (routeLine) map.removeLayer(routeLine);
    routeLine = null;
    return;
  }

  // Fetch real road route
  fetchRoadRoute(rLat, rLon, victim.latitude, victim.longitude, victim.alert === 1);
}

async function fetchRoadRoute(startLat, startLon, endLat, endLon, isEmergency) {
  try {
    console.log("Fetching road route from", startLat, startLon, "to", endLat, endLon);
    
    const response = await fetch(API.roadRoute, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        startLatitude: startLat,
        startLongitude: startLon,
        endLatitude: endLat,
        endLongitude: endLon
      })
    });

    const data = await response.json();
    console.log("Road route response:", data);

    if (data.error) {
      console.warn("Road routing error:", data.error);
      // Fallback to straight line
      const km = haversineKm(startLat, startLon, endLat, endLon);
      $("routeDistance").textContent = `${km.toFixed(2)} km (straight line - ${data.error})`;

      if (routeLine) map.removeLayer(routeLine);
      routeLine = L.polyline([[startLat, startLon], [endLat, endLon]], {
        color: isEmergency ? "#ff4d4d" : "#2ecc71",
        weight: 4,
        opacity: 0.85,
        dashArray: "6 8"
      }).addTo(map);
      return;
    }

    const distanceKm = data.distanceKm || 0;
    const durationMinutes = data.durationMinutes || 0;
    $("routeDistance").textContent = `${distanceKm.toFixed(2)} km (${Math.round(durationMinutes)} min via road)`;

    // Draw road route on map
    if (routeLine) map.removeLayer(routeLine);

    if (!data.path || data.path.length === 0) {
      console.warn("No path data in response");
      const km = haversineKm(startLat, startLon, endLat, endLon);
      $("routeDistance").textContent = `${km.toFixed(2)} km (straight line - no path data)`;
      
      routeLine = L.polyline([[startLat, startLon], [endLat, endLon]], {
        color: isEmergency ? "#ff4d4d" : "#2ecc71",
        weight: 4,
        opacity: 0.85,
        dashArray: "6 8"
      }).addTo(map);
      return;
    }

    // Convert path from [lon, lat] to [lat, lon] for Leaflet
    const path = data.path.map(coord => [coord[1], coord[0]]);
    console.log("Drawing route with", path.length, "points");
    
    routeLine = L.polyline(path, {
      color: isEmergency ? "#ff4d4d" : "#2ecc71",
      weight: 4,
      opacity: 0.85
    }).addTo(map);
    
    console.log("Road route drawn successfully");

  } catch (e) {
    console.error("Error fetching road route:", e);
    const km = haversineKm(startLat, startLon, endLat, endLon);
    $("routeDistance").textContent = `${km.toFixed(2)} km (error: ${e.message})`;
  }
}

function upsertVictimMarker(deviceId, loc) {
  const color = (loc.alert === 1) ? "#ff4d4d" : "#2ecc71";

  let marker = victimMarkers.get(deviceId);
  if (!marker) {
    marker = L.marker([loc.latitude, loc.longitude], { icon: markerIcon(color) }).addTo(map);
    victimMarkers.set(deviceId, marker);
  } else {
    marker.setLatLng([loc.latitude, loc.longitude]);
    marker.setIcon(markerIcon(color));
  }

  const popup = `
    <div style="min-width: 220px">
      <div style="font-weight:800;font-size:14px">${deviceId}</div>
      <div style="margin-top:6px">
        <div><b>Status:</b> ${loc.alert === 1 ? "EMERGENCY" : "NORMAL"}</div>
        <div><b>Lat:</b> ${loc.latitude.toFixed(6)}</div>
        <div><b>Lon:</b> ${loc.longitude.toFixed(6)}</div>
        <div><b>RSSI:</b> ${loc.rssi ?? "—"}</div>
        <div><b>Received:</b> ${fmtTime(loc.receivedAt)}</div>
      </div>
      <div style="margin-top:10px">
        <button class="btn btn-sm btn-primary" onclick="window.__selectDevice('${deviceId.replaceAll("'", "\\'")}')">
          Select
        </button>
        <button class="btn btn-sm btn-outline-secondary" onclick="window.__toggleDeviceSelection('${deviceId.replaceAll("'", "\\'")}')">
          ${selectedDevices.has(deviceId) ? "Deselect" : "Add to Route"}
        </button>
      </div>
    </div>
  `;
  marker.bindPopup(popup);

  marker.off("click");
  marker.on("click", () => selectDevice(deviceId));
}

function renderDeviceList() {
  const q = $("searchBox").value.trim().toLowerCase();
  const filter = $("filterSelect").value;

  const items = Array.from(latestByDevice.values())
    .filter(d => {
      if (q && !d.deviceId.toLowerCase().includes(q)) return false;
      if (filter === "emergency" && d.alert !== 1) return false;
      if (filter === "normal" && d.alert !== 0) return false;
      return true;
    })
    .sort((a, b) => {
      if (a.alert !== b.alert) return b.alert - a.alert; // emergency first
      return new Date(b.receivedAt) - new Date(a.receivedAt);
    });

  $("deviceCount").textContent = String(latestByDevice.size);

  const list = $("deviceList");
  if (items.length === 0) {
    list.innerHTML = `<div class="empty-state">No devices match your filter.</div>`;
    return;
  }

  list.innerHTML = items.map(d => {
    const cls = d.alert === 1 ? "emergency" : "normal";
    const badge = d.alert === 1 ? "EMERGENCY" : "NORMAL";
    const isSelected = selectedDevices.has(d.deviceId);
    const selectedClass = isSelected ? " selected-device" : "";
    return `
      <div class="device-card ${cls}${selectedClass}" onclick="window.__selectDevice('${d.deviceId.replaceAll("'", "\\'")}')">
        <div class="device-title">
          <span>${d.deviceId}</span>
          <span class="badge-pill ${cls}">${badge}</span>
          ${isSelected ? '<span class="badge bg-success">✓</span>' : ''}
        </div>
        <div class="device-meta">
          <div>Lat: <b>${d.latitude.toFixed(6)}</b> | Lon: <b>${d.longitude.toFixed(6)}</b></div>
          <div>RSSI: <b>${d.rssi ?? "—"}</b> | Received: <b>${fmtTime(d.receivedAt)}</b></div>
        </div>
      </div>
    `;
  }).join("");
}

async function refreshDevicesOnce() {
  const res = await fetch(API.devices);
  const devices = await res.json();

  latestByDevice.clear();
  devices.forEach(d => latestByDevice.set(d.deviceId, d));

  devices.forEach(d => upsertVictimMarker(d.deviceId, d));
  renderDeviceList();
}

function startSSE() {
  const statusEl = $("connStatus");
  let es;

  function setStatus(text) { statusEl.textContent = text; }

  try {
    es = new EventSource(API.stream);

    es.onopen = () => setStatus("SSE: Connected");
    es.onerror = () => setStatus("SSE: Disconnected (fallback refresh)");

    es.addEventListener("location", (evt) => {
      // evt.data is LocationResponse
      const loc = JSON.parse(evt.data);

      // Convert LocationResponse -> device status shape used by UI
      const d = {
        deviceId: loc.deviceId,
        alert: loc.alert,
        latitude: loc.latitude,
        longitude: loc.longitude,
        rssi: loc.rssi,
        receivedAt: loc.receivedAt
      };

      latestByDevice.set(d.deviceId, d);
      upsertVictimMarker(d.deviceId, d);
      renderDeviceList();

      if (selectedDeviceId === d.deviceId) updateRouteUI();
    });

  } catch (e) {
    setStatus("SSE: Failed");
  }

  // fallback: periodic refresh if SSE dies
  setInterval(async () => {
    if (!es || es.readyState === 2) {
      try { await refreshDevicesOnce(); } catch {}
    }
  }, 4000);
}

// exposed for popup button
window.__selectDevice = selectDevice;

function clearOptimizedRoute() {
  optimizedRouteLines.forEach(line => map.removeLayer(line));
  optimizedRouteLines = [];
  $("optimizedRouteInfo").innerHTML = "";
}

async function optimizeRoute() {
  if (!rescueMarker) {
    alert("Please set rescue start point on the map first");
    return;
  }

  if (selectedDevices.size === 0) {
    alert("Please select at least one device to optimize route");
    return;
  }

  const rLat = rescueMarker.getLatLng().lat;
  const rLon = rescueMarker.getLatLng().lng;

  const btn = $("btnOptimizeRoute");
  btn.disabled = true;
  btn.textContent = "Optimizing road route...";

  try {
    const response = await fetch(API.optimizedRoadRoute, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        startLatitude: rLat,
        startLongitude: rLon,
        deviceIds: Array.from(selectedDevices)
      })
    });

    const data = await response.json();

    if (data.error) {
      alert("Error optimizing route: " + data.error);
      return;
    }

    // Clear previous optimized route
    clearOptimizedRoute();

    // Draw optimized road route on map
    const segments = data.segments;
    if (segments && segments.length > 0) {
      let cumulativePath = [[rLat, rLon]];

      segments.forEach((segment, idx) => {
        const path = segment.path;
        if (path && path.length > 0) {
          // path is array of [lon, lat], convert to [lat, lon] for Leaflet
          const latLonPath = path.map(coord => [coord[1], coord[0]]);
          
          // Draw segment polyline
          const polyline = L.polyline(latLonPath, {
            color: "#8b5cf6",
            weight: 3,
            opacity: 0.8
          }).addTo(map);
          optimizedRouteLines.push(polyline);

          cumulativePath = cumulativePath.concat(latLonPath);
        }

        // Draw waypoint marker with stop number
        const wpLat = segment.latitude;
        const wpLon = segment.longitude;
        const circle = L.circleMarker([wpLat, wpLon], {
          radius: 14,
          fillColor: "#8b5cf6",
          fillOpacity: 0.95,
          color: "white",
          weight: 2,
          className: "route-waypoint"
        }).addTo(map);

        const durationText = segment.durationFromPrevious > 0 
          ? ` (${Math.round(segment.durationFromPrevious / 60)} min)` 
          : "";

        circle.bindPopup(
          `<b>Stop ${segment.order}: ${segment.deviceId}</b><br/>` +
          `Distance: ${segment.distanceFromPrevious.toFixed(2)} km${durationText}<br/>` +
          `Cumulative: ${segment.cumulativeDistance.toFixed(2)} km`
        );

        optimizedRouteLines.push(circle);
      });

      // Show route info with detailed breakdown
      let html = `<div style="background:#f0f0f0;padding:10px;border-radius:4px;margin-top:10px">
        <b style="color:#8b5cf6">🚗 Optimized Road Route</b><br/>
        <div style="font-size:12px;margin-top:5px">
          Total Distance: <b>${data.totalDistance.toFixed(2)} km</b><br/>
          Estimated Time: <b>${Math.round(data.totalDurationMinutes)} minutes</b><br/>
          Stops: <b>${data.segments.length}</b>
        </div>
        <div style="font-size:11px;margin-top:8px;max-height:180px;overflow-y:auto">`;

      segments.forEach(step => {
        const durationText = step.durationFromPrevious > 0 
          ? ` • ${Math.round(step.durationFromPrevious / 60)} min` 
          : "";
        html += `<div style="padding:5px;border-bottom:1px solid #ddd;background:#fff">
          <b style="color:#8b5cf6">Stop ${step.order}</b> → ${step.deviceId}<br/>
          <span style="color:#666;font-size:10px">
            ↳ ${step.distanceFromPrevious.toFixed(2)} km${durationText}
          </span>
        </div>`;
      });

      html += `</div></div>`;
      $("optimizedRouteInfo").innerHTML = html;
    }

  } catch (e) {
    alert("Error: " + e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = "Optimize Multi-Route";
  }
}

window.addEventListener("load", async () => {
  initMap();

  $("btnRefresh").addEventListener("click", () => refreshDevicesOnce());
  $("btnClearRescue").addEventListener("click", () => clearRescueStart());

  if ($("btnOptimizeRoute")) {
    $("btnOptimizeRoute").addEventListener("click", () => optimizeRoute());
  }

  $("searchBox").addEventListener("input", () => renderDeviceList());
  $("filterSelect").addEventListener("change", () => renderDeviceList());

  $("btnClearAll").addEventListener("click", async () => {
    if (!confirm("Delete ALL stored locations in DB?")) return;
    await fetch(API.clearAll, { method: "DELETE" });
    latestByDevice.clear();
    victimMarkers.forEach(m => map.removeLayer(m));
    victimMarkers.clear();
    selectedDeviceId = null;
    selectedDevices.clear();
    $("selectedDevice").textContent = "—";
    clearOptimizedRoute();
    renderDeviceList();
  });

  await refreshDevicesOnce();
  startSSE();
});

window.__toggleDeviceSelection = function(deviceId) {
  if (selectedDevices.has(deviceId)) {
    selectedDevices.delete(deviceId);
  } else {
    selectedDevices.add(deviceId);
  }
  renderDeviceList();
};