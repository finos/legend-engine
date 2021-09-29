// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.test.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class TestExecutionResultObjectMapperFactory
{
    @Test
    public void doesNotAutocloseJsonArraysAndObjects() throws IOException
    {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = ExecutionResultObjectMapperFactory.getNewObjectMapper().getFactory().createGenerator(writer);
        generator.writeStartArray();
        generator.writeStartObject();
        generator.close();

        Assert.assertEquals("Generator should not autoclose JSON array and object", "[{", writer.toString());
    }
}
