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

package org.finos.legend.engine.protocol.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestLoadModelAndMapping
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testSplit() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("split_v1_15_0.json")), PureModelContextData.class);
        List<PureModelContextData> parts = PureModelContextData.partition(context, 2);
        Assert.assertEquals("v1_15_0", context.getSerializer().version);
        Assert.assertEquals(31, context.getElements().size());

        PureModelContextData part1 = parts.get(0);
        Assert.assertSame(context.getSerializer(), part1.getSerializer());
        Assert.assertSame(context.getOrigin(), part1.getOrigin());
        Assert.assertEquals(16, part1.getElements().size());

        PureModelContextData part2 = parts.get(1);
        Assert.assertSame(context.getSerializer(), part2.getSerializer());
        Assert.assertSame(context.getOrigin(), part2.getOrigin());
        Assert.assertEquals(15, part2.getElements().size());

        List<String> missingElements = context.getElements().stream()
                .filter(e -> !part1.getElements().contains(e) && !part2.getElements().contains(e))
                .map(PackageableElement::getPath)
                .collect(Collectors.toList());
        Assert.assertEquals(Collections.emptyList(), missingElements);
    }

    @Test
    public void testFull() throws Exception
    {
        PureModelContextData context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("full.json")), PureModelContextData.class);
        Assert.assertEquals(94, context.getElementsOfType(Class.class).size());
        Assert.assertEquals(1, context.getElementsOfType(PackageableConnection.class).size());
        Assert.assertEquals(1, context.getElementsOfType(PackageableRuntime.class).size());
    }

    @Test
    public void testLoadMappingWithLegacyEnumValueMapping() throws Exception
    {
        objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("mappingWithLegacyEnumValueMapping.json")), PureModelContextData.class);
    }
}
