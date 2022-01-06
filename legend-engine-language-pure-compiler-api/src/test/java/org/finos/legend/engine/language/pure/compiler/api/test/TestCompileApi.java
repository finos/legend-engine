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

package org.finos.legend.engine.language.pure.compiler.api.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.api.Compile;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestCompileApi
{
    private static final Compile compileApi = new Compile(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testEnumerationMappingWithMixedFormatSourceValues()
    {
        testWithProtocolPath("faultyEnumerationMappingWithMixedFormatSourceValues.json", FastList.newListWith("Mixed formats for enum value mapping source values"));
    }

    @Test
    public void testResolutionOfAutoImportsWhenNoSectionInfoIsProvided()
    {
        testWithProtocolPath("enumerationWithSystemProfileButNoSection.json");
    }

    public void testWithProtocolPath(String protocolPath)
    {
        testWithProtocolPath(protocolPath, null);
    }

    public void testWithProtocolPath(String protocolPath, List<String> compilationResultTextFragments)
    {
        String jsonString = new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(protocolPath), "Can't find resource '" + protocolPath + "'"), "UTF-8").useDelimiter("\\A").next();
        testWithJson(jsonString, compilationResultTextFragments);
    }

    // NOTE: since if compilation failed we throw an EngineException which inherits many properties from the general Exception
    // comparing the JSON is not a good option, so we have to search fragment of the error response string instead
    // We can fix this method when we properly serialize the error response
    public void testWithJson(String pureModelContextDataJsonStr, List<String> compilationResultTextFragments)
    {
        String actual;
        try
        {
            PureModelContextData pureModelContextData = objectMapper.readValue(pureModelContextDataJsonStr, PureModelContextData.class);
            Object response = compileApi.compile(pureModelContextData, null, null).getEntity();
            actual = response instanceof EngineException ? objectMapper.writeValueAsString(response) : response.toString();
            // NOTE when we call `toString` we most likely call it on `EngineException` which will return something that does not really make sense
            // and it's not a JSON object (since the verbose stack-trace info is also included), so it's hard to just print out everything
            if (compilationResultTextFragments != null)
            {
                compilationResultTextFragments.forEach(fragment -> assertTrue(actual.contains(fragment)));
            }
            else
            {
                assertEquals("{\"message\":\"OK\"}", actual);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
