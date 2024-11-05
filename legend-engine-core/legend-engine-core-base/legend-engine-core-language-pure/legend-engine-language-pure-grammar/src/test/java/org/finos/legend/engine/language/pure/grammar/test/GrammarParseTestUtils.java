//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.grammar.test;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

import java.io.IOException;
import java.util.Objects;

public class GrammarParseTestUtils
{
    public static PureModelContextData loadPureModelContextFromResource(String resource, Class<?> clazz)
    {
        return loadPureModelContextFromResource(resource, null, clazz);
    }

    public static PureModelContextData loadPureModelContextFromResource(String resource, String extraCode, Class<?> clazz)
    {
        return loadPureModelContextFromResources(FastList.newListWith(resource), extraCode, clazz);
    }

    public static PureModelContextData loadPureModelContextFromResources(Class<?> clazz, String... resources)
    {
        return loadPureModelContextFromResources(FastList.newListWith(resources), null, clazz);
    }

    public static PureModelContextData loadPureModelContextFromResources(Iterable<String> resources, String extraCode, Class<?> clazz)
    {
        String resourceCode = StringUtils.join(IterableIterate.collect(resources, r -> getResource(r, clazz)), "\n");
        String code = extraCode != null ? resourceCode + "\n" + extraCode : resourceCode;

        return PureModelContextData.newBuilder().withPureModelContextData(PureGrammarParser.newInstance().parseModel(code)).build();
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
}