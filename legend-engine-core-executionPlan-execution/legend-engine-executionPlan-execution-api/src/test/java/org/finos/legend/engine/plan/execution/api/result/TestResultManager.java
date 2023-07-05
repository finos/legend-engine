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

package org.finos.legend.engine.plan.execution.api.result;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class TestResultManager
{
    @Test
    public void testDefaultErrorCode() throws Exception
    {
        Response response = ResultManager.manageResult(Lists.mutable.empty(), new ErrorResult(999, "some error"), SerializationFormat.PURE, LoggingEventType.SERVICE_ERROR);
        assertEquals(500, response.getStatus());
        ResultManager.ErrorMessage errorMessage = (ResultManager.ErrorMessage) response.getEntity();
        // error code of 999 in the input result is ignored and replaced by a magic number 20 - This is legacy behavior that is preserved as is
        assertEquals(20, errorMessage.code);
    }

    @Test
    public void testNonDefaultErrorCode() throws Exception
    {
        Response response = ResultManager.manageResultWithCustomErrorCode(Lists.mutable.empty(), new ErrorResult(999, "some error"), SerializationFormat.PURE, LoggingEventType.SERVICE_ERROR);
        assertEquals(500, response.getStatus());
        ResultManager.ErrorMessage errorMessage = (ResultManager.ErrorMessage) response.getEntity();
        assertEquals(999, errorMessage.code);
    }
}