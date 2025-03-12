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

package org.finos.legend.engine.protocol.pure.v1.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProduct;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class TestDataProductProtocol
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testBasicProtocol() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("dataProduct_TestData.json")), PureModelContextData.class);
        Assert.assertEquals(1, context.getElementsOfType(DataProduct.class).size());
        Assert.assertEquals(1, context.getElements().size());
    }

    @Test
    public void testBasicProtocolForExecutableTemplate() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("dataProduct_TestDataWithExecutableTemplate.json")), PureModelContextData.class);
        Assert.assertEquals(1, context.getElementsOfType(DataProduct.class).size());
        Assert.assertEquals(1, context.getElements().size());
    }

    @Test
    public void testProtocolBackwardCompatibility() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("dataProduct_DeprecatedModel.json")), PureModelContextData.class);
        Assert.assertEquals(1, context.getElementsOfType(DataProduct.class).size());
        Assert.assertEquals(1, context.getElements().size());
    }
}
