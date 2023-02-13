// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.pure.api.test.inMemory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

import static org.finos.legend.engine.query.pure.api.test.inMemory.TestExecutionUtility.runTest;
import static org.junit.Assert.assertEquals;

public class TestRelationalManagerOnExecution
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testRelationalTrackStateOnExecution() throws IOException
    {
        StoreExecutableManager.INSTANCE.registerManager();
        ExecuteInput input = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("relationalQueryExecutionInputEnumZeroOne.json")), ExecuteInput.class);
        HttpServletRequest mockRequest = TestExecutionUtility.buildMockRequest();
        Response response = runTest(input, mockRequest);
        Assert.assertEquals(1, StoreExecutableManager.INSTANCE.getExecutables(mockRequest.getSession().getId()).size());
        String json = TestExecutionUtility.responseAsString(response);
        response.close();
        Assert.assertTrue(StoreExecutableManager.INSTANCE.getExecutables(mockRequest.getSession().getId()).isEmpty());
        assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"Inc Type\",\"type\":\"model::IncType\",\"relationalType\":\"VARCHAR(200)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select top 1000 \\\"root\\\".Inc as \\\"Inc Type\\\" from FirmTable as \\\"root\\\" where (\\\"root\\\".Inc = 'LLC')\"}], \"result\" : {\"columns\" : [\"Inc Type\"], \"rows\" : [{\"values\": [\"LLC\"]}]}}", json);
        StoreExecutableManager.INSTANCE.reset();

    }

}
