// Copyright 2025 Goldman Sachs
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.api.Autofix;
import org.finos.legend.engine.language.pure.compiler.api.LambdaTdsToRelationInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

public class TestAutofixApi
{
    private static final Autofix autofixApi = new Autofix(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testTransformTdsToRelationLambda() throws JsonProcessingException
    {
        String model = "Class model::Person {\n" +
                "name: String[1];\n" +
                "}\n";
        PureModelContextText text = new PureModelContextText();
        text.code = model;
        Lambda lambda = PureGrammarParser.newInstance().parseLambda("|model::Person.all()->project([x|$x.name],['Name'])", "", 0, 0, false);
        LambdaTdsToRelationInput lambdaTdsToRelationInput = new LambdaTdsToRelationInput();
        lambdaTdsToRelationInput.model = text;
        lambdaTdsToRelationInput.lambda = lambda;
        String stringResult = objectMapper.writeValueAsString(autofixApi.transformTdsToRelationLambda(lambdaTdsToRelationInput, null).getEntity());
        Lambda actualLambda = objectMapper.readValue(stringResult, Lambda.class);
        String actualLambdaString = actualLambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.STANDARD).build());
        String expectedLambdaString = "|model::Person.all()->project(~[Name:x: model::Person[1]|$x.name])";
        Assert.assertEquals(expectedLambdaString, actualLambdaString);
    }
}
