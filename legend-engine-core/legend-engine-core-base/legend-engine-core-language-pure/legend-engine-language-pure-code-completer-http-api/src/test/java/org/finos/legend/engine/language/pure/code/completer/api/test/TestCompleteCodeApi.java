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

package org.finos.legend.engine.language.pure.code.completer.api.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.code.completer.api.CompleteCode;
import org.finos.legend.engine.language.pure.code.completer.api.CompleteCodeInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.function.Consumer;

public class TestCompleteCodeApi
{
    private static final CompleteCode COMPLETE_CODE_API = new CompleteCode(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testCompleteCodeAPI() throws IOException
    {
        getCompletionsAndAssert("", "", (r) -> Assert.assertEquals("{\"completions\":[]}", r));
        getCompletionsAndAssert("", "1->", (r) -> Assert.assertTrue(r.contains("{\"completion\":\"abs(\",\"display\":\"abs\"}")));
        getCompletionsAndAssert("Class x::A{name:String[1];}", "x::A.all()->filter(y|$y.name1->", (r) -> Assert.assertEquals("{\"completions\":[],\"exception\":\"Can't find property 'name1' in class 'x::A'\"}", r));
    }

    @Test
    public void testCompleteCodeAPIErrorMessageWithOffset() throws IOException
    {
        Response response = COMPLETE_CODE_API.completeCode(null, buildCompleteCodeInput(buildPureModelContextText(""), "1->", 1), null, null);
        Assert.assertEquals(500, response.getStatus());
        Assert.assertTrue("", OBJECT_MAPPER.writeValueAsString(response.getEntity()).contains("Code completion with offset not yet supported (offset should be either -1 or null)"));
    }

    private void getCompletionsAndAssert(String modelCode, String codeBlock, Consumer<String> assertFunction) throws IOException
    {
        assertWithPureModelContextText(modelCode, codeBlock, assertFunction);
        assertWithPureModelContextData(modelCode, codeBlock, assertFunction);
    }

    private void assertWithPureModelContextText(String modelCode, String codeBlock, Consumer<String> assertFunction) throws IOException
    {
        Response response = COMPLETE_CODE_API.completeCode(null, buildCompleteCodeInput(buildPureModelContextText(modelCode), codeBlock, null), null, null);
        Assert.assertEquals(200, response.getStatus());
        assertFunction.accept(OBJECT_MAPPER.writeValueAsString(response.getEntity()));
    }

    private void assertWithPureModelContextData(String modelCode, String codeBlock, Consumer<String> assertFunction) throws IOException
    {
        Response response = COMPLETE_CODE_API.completeCode(null, buildCompleteCodeInput(buildPureModelContextData(modelCode), codeBlock, null), null, null);
        Assert.assertEquals(200, response.getStatus());
        assertFunction.accept(OBJECT_MAPPER.writeValueAsString(response.getEntity()));
    }

    private CompleteCodeInput buildCompleteCodeInput(PureModelContext model, String codeBlock, Integer offset)
    {
        CompleteCodeInput input = new CompleteCodeInput();
        input.model = model;
        input.codeBlock = codeBlock;
        input.offset = offset;
        return input;
    }

    private PureModelContextText buildPureModelContextText(String code)
    {
        PureModelContextText text = new PureModelContextText();
        text.code = code;
        return text;
    }

    private PureModelContextData buildPureModelContextData(String code)
    {
        return PureGrammarParser.newInstance().parseModel(code);
    }
}
