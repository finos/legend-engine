//  Copyright 2025 Goldman Sachs
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.Scanner;

public class Processor
{
    @Test
    public void test() throws Exception
    {
        System.out.println(new BigDecimal("4.0").compareTo(new BigDecimal("4")));
        //process(new File(Processor.class.getClassLoader().getResource("122390239DF2390").toURI()));
    }

    public void process(File file) throws FileNotFoundException, JsonProcessingException
    {
        System.out.println(file.getName());
        File[] children = file.listFiles();

        String grammar = null;
        String grammarCompare = null;
        String protocol = null;
        for (File child : (children == null ? new File[0] : children))
        {
            if (child.isDirectory())
            {
                process(child);
            }
            if (child.getName().equals("grammar.pure"))
            {
                grammar = readFile(child);
            }
            if (child.getName().equals("grammar_compare.pure"))
            {
                grammarCompare = readFile(child);
            }
            if (child.getName().equals("protocol.json"))
            {
                protocol = readFile(child);
            }
        }
        if (grammar != null && protocol != null)
        {
            ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            System.out.println(mapper.writeValueAsString(PureGrammarParser.newInstance().parseModel(grammar)));
            PureModelContextData pureModelContextData = mapper.readValue(protocol, PureModelContextData.class);
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.STANDARD).build());
            String rendered = grammarTransformer.renderPureModelContextData(pureModelContextData);
            Assert.assertEquals(grammarCompare == null ? grammar : grammarCompare, rendered);
        }
    }

    private static String readFile(File child) throws FileNotFoundException
    {
        try (Scanner s = new Scanner(new FileReader(child)).useDelimiter("\\A"))
        {
            return s.next();
        }
    }
}
