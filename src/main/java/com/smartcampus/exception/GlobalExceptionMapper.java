package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 - Global Safety Net.
 * Catches ANY uncaught Throwable and returns HTTP 500.
 *
 * From a cybersecurity standpoint, exposing raw Java stack traces is dangerous:
 *  - They reveal internal class names, package structure, and library versions,
 *    which an attacker can use to identify known CVEs.
 *  - They can expose file system paths, configuration, or logic flaws.
 *  - They confirm which lines of code are reachable, aiding further exploits.
 * This mapper replaces all of that with a generic, safe error message.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable throwable) {
        // Log the full stack trace server-side only
        LOGGER.log(Level.SEVERE, "Unexpected server error", throwable);

        // Return a safe, generic response to the client
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Please contact the administrator."
                ))
                .build();
    }
}
