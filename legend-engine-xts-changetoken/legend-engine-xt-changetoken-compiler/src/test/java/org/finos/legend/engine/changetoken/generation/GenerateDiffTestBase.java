// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateDiffTestBase
{
    private static final ObjectMapper mapper = new ObjectMapper();

    protected String diff(String newEntities, String newDependencies, String oldEntities, String oldDependencies, String versions, String propertyRenames, String classRenames, String defaultValues)
    {
        return new GenerateDiff(pure(newEntities), pure(newDependencies), pure(oldEntities), pure(oldDependencies)).execute(versions, (Map) parse(propertyRenames), (Map) parse(classRenames), (Map) parse(defaultValues));
    }

    protected void expect(String actual, String expected)
    {
        Assert.assertEquals(parse(expected), parse(actual));
    }

    private List<PackageableElement> pure(String model)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(model);
        return contextData.streamAllElements().collect(Collectors.toList());
    }

    private Map<String, Object> parse(String value)
    {
        try
        {
            return mapper.readValue(value, Map.class);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
