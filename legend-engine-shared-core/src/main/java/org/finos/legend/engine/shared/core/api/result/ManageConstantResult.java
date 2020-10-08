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

package org.finos.legend.engine.shared.core.api.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;

import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class ManageConstantResult
{
    public static Response manageResult(Subject subject, Object value)
    {
        return manageResult(subject, value, ObjectMapperFactory.getNewStandardObjectMapper());
    }

    public static Response manageResult(Subject subject, Object value, ObjectMapper objectMapper)
    {
        try
        {
            return Response.ok(objectMapper.writeValueAsString(value)).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (IOException exception)
        {
            return ExceptionTool.exceptionManager(exception, LoggingEventType.EXECUTE_CONSTANT_RESPONSE_ERROR, subject);
        }
    }
}
