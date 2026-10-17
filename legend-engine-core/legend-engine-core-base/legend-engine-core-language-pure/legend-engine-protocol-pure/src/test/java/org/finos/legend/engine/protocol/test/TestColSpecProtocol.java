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
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

public class TestColSpecProtocol
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testMultiplicitySurvivesRoundTrip() throws Exception
    {
        // `~[a:Integer[0..1]]`: the compiler builds the column's multiplicity from it.
        ColSpec colSpec = new ColSpec();
        colSpec.name = "a";
        colSpec.multiplicity = Multiplicity.ZERO_ONE;
        ColSpec read = objectMapper.readValue(objectMapper.writeValueAsString(colSpec), ColSpec.class);
        Assert.assertEquals(Multiplicity.ZERO_ONE, read.multiplicity);
    }
}
