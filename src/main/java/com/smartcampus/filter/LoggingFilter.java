package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Observability Filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter so a
 * single class handles both inbound and outbound logging.
 *
 * Why filters instead of Logger.info() in every resource method?
 *  - Cross-cutting concern: logging applies to every endpoint. Duplicating
 *    Logger.info() in dozens of methods violates DRY and creates maintenance burden.
 *  - Centralised control: change log format or add correlation IDs in one place.
 *  - Cleaner resource classes: business logic stays uncluttered by infrastructure code.
 *  - Works even when exceptions bypass resource methods entirely.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST]  Method=%s  URI=%s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] Method=%s  URI=%s  Status=%d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()
        ));
    }
}
