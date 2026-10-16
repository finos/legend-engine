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
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestModelStoreDataProtocol
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testSourceInformationSurvivesRoundTrip() throws Exception
    {
        // The parser positions `ModelStore #{ … }#` on the whole embedded data.
        ModelStoreData data = new ModelStoreData();
        data.modelData = Collections.emptyList();
        data.sourceInformation = new SourceInformation("source", 4, 3, 7, 4);
        ModelStoreData read = (ModelStoreData) objectMapper.readValue(objectMapper.writeValueAsString(data), EmbeddedData.class);
        Assert.assertNotNull(read.sourceInformation);
        Assert.assertEquals(objectMapper.writeValueAsString(data.sourceInformation), objectMapper.writeValueAsString(read.sourceInformation));
    }
}
