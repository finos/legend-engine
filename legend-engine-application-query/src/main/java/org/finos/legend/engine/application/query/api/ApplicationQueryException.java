package org.finos.legend.engine.application.query.api;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ApplicationQueryException extends RuntimeException
{
    private final Response.Status statusCode;

    public ApplicationQueryException(String message, Response.Status statusCode)
    {
        super(message);
        this.statusCode = statusCode;
    }

    public Response toResponse()
    {
        return Response.status(this.statusCode).type(MediaType.APPLICATION_JSON_TYPE).entity("{\"message\":" + this.getMessage() + "}").build();
    }
}
