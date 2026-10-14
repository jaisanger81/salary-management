package com.acme.salary.api;

import com.acme.salary.service.EmployeeNotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException exception) {
        Throwable cause = exception;
        while (cause instanceof CamelExecutionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof EmployeeNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiModels.ErrorDto(cause.getMessage())).build();
        }
        if (cause instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiModels.ErrorDto(cause.getMessage())).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ApiModels.ErrorDto("The request could not be completed.")).build();
    }
}
