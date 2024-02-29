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

package org.finos.legend.engine.query.graphQL.api.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.collections.impl.*;
import org.eclipse.collections.impl.factory.*;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.query.graphQL.api.format.generation.GraphQLGenerationExtension;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

import static org.finos.legend.pure.generated.core_external_query_graphql_deprecated_tests_generationTests.Root_meta_external_query_graphQL_generation_tests_constants_comparisonExpressions_String_MANY__String_1_;

public class TestGraphQLFileGeneration
{
    @Test
    public void testSimpleGraphQL()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, IdentityFactoryProvider.getInstance().getAnonymousIdentity(), DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            GraphQLGenerationExtension graphQLGenerationExtension = new GraphQLGenerationExtension();
            List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = graphQLGenerationExtension.generateFromElement(fileGeneration, pureModel.getContext());

            Assert.assertEquals(outputs.size(), 5);
            outputs.forEach(o ->
            {
                if (o._fileName().endsWith("scalars.graphql"))
                {
                    Assert.assertEquals(getResourceAsString("scalars.graphql"), o._content());
                }
                else if (o._fileName().endsWith("primitive_comparisons.graphql"))
                {
                    Assert.assertEquals(Root_meta_external_query_graphQL_generation_tests_constants_comparisonExpressions_String_MANY__String_1_(Iterables.iList("Int", "String"), pureModel.getExecutionSupport()), o._content());
                }
                else
                {
                    Assert.assertEquals(getResourceAsString(o._fileName().substring(o._fileName().lastIndexOf('/') + 1)), o._content());
                }
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testScalarGraphQL()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("scalarFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, IdentityFactoryProvider.getInstance().getAnonymousIdentity(), DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            GraphQLGenerationExtension graphQLGenerationExtension = new GraphQLGenerationExtension();
            List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = graphQLGenerationExtension.generateFromElement(fileGeneration, pureModel.getContext());

            Assert.assertEquals(outputs.size(), 3);
            outputs.forEach(o ->
            {
                if (o._fileName().endsWith("scalars.graphql"))
                {
                    Assert.assertEquals(getResourceAsString("scalars.graphql"), o._content());
                }
                else if (o._fileName().endsWith("primitive_comparisons.graphql"))
                {
                    Assert.assertEquals(Root_meta_external_query_graphQL_generation_tests_constants_comparisonExpressions_String_MANY__String_1_(Iterables.iList("BigDecimal","Date", "DateTime"), pureModel.getExecutionSupport()), o._content());
                }
                else
                {
                    Assert.assertEquals(getResourceAsString(o._fileName().substring(o._fileName().lastIndexOf('/') + 1)), o._content());
                }
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        String jsonString = this.getResourceAsString(fileName);
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(jsonString, PureModelContextData.class);
    }

    private String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestGraphQLFileGeneration.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
