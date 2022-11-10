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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.PropertyView;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class TestDiagramProtocol
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testBasicProtocol() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("modelWithDiagram.json")), PureModelContextData.class);
        Assert.assertEquals(1, context.getElementsOfType(Diagram.class).size());
        Assert.assertEquals(1, context.getElements().size());
    }

    @Test
    public void testOldPropertyPointerProtocol() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("modelWithDiagram.json")), PureModelContextData.class);
        Assert.assertEquals(1, context.getElementsOfType(Diagram.class).size());
        Diagram diagram = (Diagram) context.getElementsOfType(Diagram.class).get(0);
        Assert.assertEquals(2, diagram.propertyViews.size());
        PropertyView propertyView = diagram.propertyViews.get(0);
        Assert.assertNotNull(propertyView.property.owner);
    }
}
