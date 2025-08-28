// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;

import java.io.IOException;
import java.util.Objects;

public class SQLSourceProviderTestUtils
{

    //TODO replace once equals method added in legend
    public static void assertLogicalEquality(Object expected, Object actual)
    {
        try
        {
            ObjectMapper mapper = PureProtocolObjectMapperFactory.getNewObjectMapper();
            Assert.assertEquals(
                    mapper.writeValueAsString(expected),
                    mapper.writeValueAsString(actual));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static PureModelContextData loadPureModelContextFromResource(String resource, Class<?> clazz)
    {
        String model = getResource(resource, clazz);
        return PureModelContextData.newBuilder().withPureModelContextData(PureGrammarParser.newInstance().parseModel(model)).build();
    }

    public static String getResource(String resource, Class<?> clazz)
    {
        try
        {
            return IOUtils.toString(Objects.requireNonNull(clazz.getClassLoader().getResourceAsStream(resource)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T loadFromResources(String resource, TypeReference<T> typeReference, Class<?> clazz)
    {
        String sources = getResource(resource, clazz);
        try
        {
            return PureProtocolObjectMapperFactory.getNewObjectMapper().readValue(sources, typeReference);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}