package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4 - Sub-Resource for Sensor Readings.
 * Handles /api/v1/sensors/{sensorId}/readings
 *
 * This class is instantiated by the sub-resource locator in SensorResource.
 * It receives the sensorId as constructor context so it knows which sensor
 * it is operating on.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // Returns all historical readings for this sensor.
    // -----------------------------------------------------------------------
    @GET
    public Response getReadings() {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        List<SensorReading> history = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(history).build();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    // Appends a new reading to this sensor's history.
    //
    // Business rules:
    //   - Sensor status "MAINTENANCE" -> throws SensorUnavailableException -> 403
    //   - On success, updates sensor.currentValue with the new reading value.
    // -----------------------------------------------------------------------
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // State constraint: MAINTENANCE sensors cannot accept readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                    "and cannot accept new readings."
            );
        }

        // Auto-generate id and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Append reading to history
        DataStore.readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // Side effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    // Fetch a single reading by ID.
    // -----------------------------------------------------------------------
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> history = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        return history.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(errorBody("Reading '" + readingId + "' not found for sensor '" + sensorId + "'."))
                        .build());
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
