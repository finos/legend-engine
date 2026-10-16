// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.protocol.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.UnknownClassInstanceTypeException;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

public class TestClassInstanceProtocol
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testUnknownTypeFailsWithItsType()
    {
        assertUnknownType("{\"_type\":\"classInstance\",\"type\":\"noSuchType\",\"value\":{}}", ValueSpecification.class);
    }

    @Test
    public void testNestedUnknownTypeFailsWithItsType()
    {
        // Within an object, Jackson reports the failure wrapped, with its path.
        assertUnknownType("{\"_type\":\"lambda\",\"parameters\":[],\"body\":[{\"_type\":\"func\",\"function\":\"f\",\"parameters\":[{\"_type\":\"classInstance\",\"type\":\"noSuchType\",\"value\":{}}]}]}", LambdaFunction.class);
    }

    private static void assertUnknownType(String json, Class<?> type)
    {
        try
        {
            objectMapper.readValue(json, type);
            Assert.fail("expected an unknown class instance type");
        }
        catch (Exception e)
        {
            Throwable cause = e;
            while (cause != null && !(cause instanceof UnknownClassInstanceTypeException))
            {
                cause = cause.getCause();
            }
            Assert.assertNotNull(e.toString(), cause);
            Assert.assertEquals("noSuchType", ((UnknownClassInstanceTypeException) cause).getType());
            Assert.assertEquals("Can't parse the ClassInstance value for type 'noSuchType'", cause.getMessage());
        }
    }
}
