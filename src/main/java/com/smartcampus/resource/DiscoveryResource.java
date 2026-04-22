package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint.
 * GET /api/v1 - Returns API metadata and HATEOAS resource links.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new HashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "v1.0.0");
        response.put("status", "operational");
        response.put("admin_contact", "admin@smartcampus.edu");
        response.put("timestamp", System.currentTimeMillis());

        // HATEOAS links - clients can navigate the entire API from here
        Map<String, String> links = new HashMap<>();
        links.put("self",          "/api/v1");
        links.put("rooms",         "/api/v1/rooms");
        links.put("sensors",       "/api/v1/sensors");
        links.put("sensor_filter", "/api/v1/sensors?type={type}");
        links.put("readings",      "/api/v1/sensors/{sensorId}/readings");
        response.put("_links", links);

        return Response.ok(response).build();
    }
}
