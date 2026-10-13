// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.application.query.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

public class ApplicationQueryException extends RuntimeException
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Response.Status statusCode;

    public ApplicationQueryException(String message, Response.Status statusCode)
    {
        super(message);
        this.statusCode = statusCode;
    }

    public Response.Status getStatus()
    {
        return this.statusCode;
    }

    public Response toResponse()
    {
        return Response.status(this.statusCode).type(MediaType.APPLICATION_JSON_TYPE).entity(this.toJson()).build();
    }

    /**
     * The message as JSON, escaped, so that a message quoting a name with a quote or a backslash stays valid JSON
     */
    String toJson()
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(Collections.singletonMap("message", this.getMessage()));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
