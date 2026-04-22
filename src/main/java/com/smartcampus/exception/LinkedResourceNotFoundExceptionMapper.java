package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Part 5.2 - Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * 422 is more semantically accurate than 404 here because the HTTP request
 * itself was well-formed and reached the correct endpoint. The problem is that
 * a *reference* inside the JSON payload (roomId) points to a resource that does
 * not exist. 404 implies the *requested URL* was not found; 422 signals that
 * the payload was understood but contains a logically invalid value.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        return Response.status(422) // 422 Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 422,
                        "error", "Unprocessable Entity",
                        "message", ex.getMessage()
                ))
                .build();
    }
}
