package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 — Global Safety Net.
 *
 * Catches ALL uncaught Throwable (including NullPointerException,
 * IndexOutOfBoundsException, etc.) and returns a sanitised HTTP 500.
 *
 * CRITICAL: Never expose the stack trace to the client. Log it server-side only.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable throwable) {
        // Log the full stack trace server-side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", throwable);

        // Return a generic, non-leaking message to the client
        ErrorResponse error = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please contact the system administrator."
        );
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
