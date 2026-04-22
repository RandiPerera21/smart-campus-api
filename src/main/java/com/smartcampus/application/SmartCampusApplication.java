package com.smartcampus.application;

import com.smartcampus.exception.*;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application entry point.
 *
 * The URL base path /api/v1 is declared in web.xml servlet-mapping.
 * @ApplicationPath is kept here as documentation only; web.xml takes precedence
 * when deploying to an external Tomcat server via NetBeans.
 *
 * Lifecycle: JAX-RS creates a NEW resource instance per HTTP request (per-request
 * scope). Shared state lives in DataStore using static ConcurrentHashMaps, which
 * are thread-safe and prevent data loss across concurrent requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Resources
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);

        // Exception mappers
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);

        // Logging filter
        register(LoggingFilter.class);

        // JSON via Jackson
        register(JacksonFeature.class);
    }
}
