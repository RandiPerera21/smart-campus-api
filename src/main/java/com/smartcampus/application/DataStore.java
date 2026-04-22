package com.smartcampus.application;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized in-memory data store.
 *
 * Uses ConcurrentHashMap for thread safety because JAX-RS creates a new
 * resource instance per request, meaning multiple requests can run
 * concurrently and simultaneously access this shared state.
 * ConcurrentHashMap ensures no data loss or race conditions occur.
 */
public class DataStore {

    // Map of roomId -> Room
    public static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // Map of sensorId -> Sensor
    public static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Map of sensorId -> List of SensorReading
    public static final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // Seed some demo data so GET /api/v1/rooms is not empty on first run
    static {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-102", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LAB-102");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);

        r1.getSensorIds().add(s1.getId());
        r2.getSensorIds().add(s2.getId());

        readings.put(s1.getId(), new ArrayList<>());
        readings.put(s2.getId(), new ArrayList<>());
    }
}
