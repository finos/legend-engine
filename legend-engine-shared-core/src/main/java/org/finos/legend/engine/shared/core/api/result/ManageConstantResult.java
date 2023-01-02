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
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class ManageConstantResult
{
    @Deprecated
    public static Response manageResult(Iterable<? extends CommonProfile> pm, Object value)
    {
        return manageResult(pm, value, ObjectMapperFactory.getNewStandardObjectMapper());
    }

    public static Response manageResult(String user, Object value)
    {
        return manageResult(user, value, ObjectMapperFactory.getNewStandardObjectMapper());
    }

    @Deprecated
    public static Response manageResult(Iterable<? extends CommonProfile> pm, Object value, ObjectMapper objectMapper)
    {
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(pm));
        return manageResult(user, value, objectMapper);
    }

    public static Response manageResult(String user, Object value, ObjectMapper objectMapper)
    {
        try
        {
            return Response.ok(objectMapper.writeValueAsString(value)).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (IOException exception)
        {
            return ExceptionTool.exceptionManager(exception, LoggingEventType.EXECUTE_CONSTANT_RESPONSE_ERROR, user);
        }
    }
}
