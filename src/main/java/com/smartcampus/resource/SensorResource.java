package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations & Linking.
 * Manages /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // -----------------------------------------------------------------------
    // GET /api/v1/sensors
    // Optional query param: ?type=CO2  -> filters by sensor type.
    // Using @QueryParam is better than path segments for filtering because
    // query params are optional, do not change the resource identity, and
    // keep the URI clean. Path segments imply a different sub-resource.
    // -----------------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(DataStore.sensors.values());
        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/sensors
    // Registers a new sensor. Validates that the referenced roomId exists.
    // If roomId does not exist -> throws LinkedResourceNotFoundException -> 422.
    // @Consumes(APPLICATION_JSON): if client sends text/plain or XML,
    //   JAX-RS returns 415 Unsupported Media Type automatically.
    // -----------------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor id is required."))
                    .build();
        }
        if (DataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Validate that the roomId exists
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room '" + sensor.getRoomId() + "' does not exist. " +
                    "Cannot register sensor without a valid room reference."
            );
        }

        DataStore.sensors.put(sensor.getId(), sensor);

        // Link the sensor into the room's sensorIds list
        Room room = DataStore.rooms.get(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        // Initialise empty reading history for this sensor
        DataStore.readings.put(sensor.getId(), new ArrayList<>());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // Fetch a single sensor.
    // -----------------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId}
    // Removes sensor and unlinks it from parent room.
    // -----------------------------------------------------------------------
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // Unlink from parent room
        Room room = DataStore.rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        DataStore.sensors.remove(sensorId);
        DataStore.readings.remove(sensorId);
        return Response.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Part 4 - Sub-Resource Locator
    // GET/POST /api/v1/sensors/{sensorId}/readings
    //
    // A sub-resource locator returns an instance of another resource class.
    // JAX-RS delegates further path matching to that class.
    // Benefits: separation of concerns, each class stays small and testable,
    // avoids one huge "god controller" managing every nested path.
    // -----------------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
