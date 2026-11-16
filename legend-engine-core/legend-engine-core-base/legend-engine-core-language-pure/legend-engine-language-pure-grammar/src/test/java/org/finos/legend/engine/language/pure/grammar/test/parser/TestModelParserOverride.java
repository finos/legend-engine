// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.ModelParserOverride;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParserOverride;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * An extension can take over the parsing of whole models, and delegate to the engine's own parser, which still parses
 * each section with its section type's parser, section parser overrides included.
 */
public class TestModelParserOverride
{
    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final String MODEL = "Class a::A\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "###Data\n" +
            "Data a::D\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{}';\n" +
            "  }#\n" +
            "}\n";

    @Test
    public void testOverrideDelegatesToTheEngineParser() throws JsonProcessingException
    {
        Overriding overriding = new Overriding();
        PureGrammarParser overridden = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), overriding));
        PureGrammarParser plain = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser()));
        Assert.assertEquals(MAPPER.writeValueAsString(plain.parseModel(MODEL, "a.pure", 2, 3, true)), MAPPER.writeValueAsString(overridden.parseModel(MODEL, "a.pure", 2, 3, true)));
        Assert.assertEquals(MAPPER.writeValueAsString(plain.parseModel(MODEL, false)), MAPPER.writeValueAsString(overridden.parseModel(MODEL, false)));
        Assert.assertEquals(2, overriding.modelsParsed);
    }

    @Test
    public void testTheEngineParserKeepsSectionParserOverrides()
    {
        Overriding overriding = new Overriding();
        DataCounting dataCounting = new DataCounting();
        PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), overriding, dataCounting)).parseModel(MODEL);
        Assert.assertEquals(1, overriding.modelsParsed);
        Assert.assertEquals(1, dataCounting.sectionsParsed);
    }

    @Test
    public void testOneModelParserOverride()
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> PureGrammarParserExtensions.fromExtensions(new Overriding(), new Overriding()));
        Assert.assertEquals("Conflicting model parser overrides: 2", e.getMessage());
    }

    /**
     * Takes over whole models, counts them, and delegates them to the parser it replaces.
     */
    private static class Overriding implements PureGrammarParserExtension
    {
        private int modelsParsed;

        @Override
        public Iterable<? extends ModelParserOverride> getModelParserOverrides()
        {
            return Collections.singletonList(replaced ->
            {
                Assert.assertNotNull(replaced);
                return (code, sourceId, lineOffset, columnOffset, returnSourceInfo) ->
                {
                    Overriding.this.modelsParsed++;
                    return replaced.parseModel(code, sourceId, lineOffset, columnOffset, returnSourceInfo);
                };
            });
        }
    }

    /**
     * Takes over the `Data` section type, counts its sections, and delegates them to the parser it replaces.
     */
    private static class DataCounting implements PureGrammarParserExtension
    {
        private int sectionsParsed;

        @Override
        public Iterable<? extends SectionParserOverride> getSectionParserOverrides()
        {
            return Collections.singletonList(new SectionParserOverride()
            {
                @Override
                public String getSectionTypeName()
                {
                    return "Data";
                }

                @Override
                public SectionParser override(SectionParser replaced)
                {
                    return SectionParser.newParser("Data", (code, elementConsumer, context) ->
                    {
                        DataCounting.this.sectionsParsed++;
                        return replaced.parse(code, elementConsumer, context);
                    });
                }
            });
        }
    }
}
