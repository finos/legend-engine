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

package org.finos.legend.engine.server.core.exceptionMappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public class JsonInformationExceptionMapper implements ExceptionMapper<JsonProcessingException>
{
    @Override
    public Response toResponse(JsonProcessingException exception)
    {
        Map<String, Object> entity = Maps.mutable.empty();
        entity.put("code", 500);
        entity.put("message", exception.getOriginalMessage());
        if (exception.getLocation() != null)
        {
            entity.put("location", String.format("line %d, column %d", exception.getLocation().getLineNr(), exception.getLocation().getColumnNr()));
        }
        return ExceptionTool.exceptionManager(exception, LoggingEventType.JSON_PARSING_ERROR, (String)null);
    }
}
