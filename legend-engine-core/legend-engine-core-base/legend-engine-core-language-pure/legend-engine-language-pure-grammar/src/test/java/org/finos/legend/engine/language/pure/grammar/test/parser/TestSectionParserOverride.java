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
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParserOverride;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * An extension can take over a section type, whether another extension or the engine itself parses it, and delegate
 * to the parser it replaces.
 */
public class TestSectionParserOverride
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
    public void testOverrideOfABuiltInSection() throws JsonProcessingException
    {
        // The engine parses `Pure` itself: the override still gets a parser to delegate to.
        assertDelegates("Pure");
    }

    @Test
    public void testOverrideOfAnExtensionSection() throws JsonProcessingException
    {
        // `Data` comes from an extension.
        assertDelegates("Data");
    }

    @Test
    public void testOneOverridePerSectionType()
    {
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> PureGrammarParserExtensions.fromExtensions(new Overriding("Pure"), new Overriding("Pure")));
        Assert.assertEquals("Conflicting overrides for section type: Pure", e.getMessage());
    }

    private static void assertDelegates(String sectionType) throws JsonProcessingException
    {
        Overriding overriding = new Overriding(sectionType);
        PureModelContextData overridden = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser(), overriding)).parseModel(MODEL);
        PureModelContextData plain = PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(new CorePureGrammarParser())).parseModel(MODEL);
        Assert.assertEquals(1, overriding.sectionsParsed);
        Assert.assertEquals(MAPPER.writeValueAsString(plain), MAPPER.writeValueAsString(overridden));
    }

    /**
     * Takes over one section type, counts the sections it parses, and delegates them to the parser it replaces.
     */
    private static class Overriding implements PureGrammarParserExtension
    {
        private final String sectionType;
        private int sectionsParsed;

        Overriding(String sectionType)
        {
            this.sectionType = sectionType;
        }

        @Override
        public Iterable<? extends SectionParserOverride> getSectionParserOverrides()
        {
            return Collections.singletonList(new SectionParserOverride()
            {
                @Override
                public String getSectionTypeName()
                {
                    return Overriding.this.sectionType;
                }

                @Override
                public SectionParser override(SectionParser replaced)
                {
                    Assert.assertNotNull(replaced);
                    return SectionParser.newParser(Overriding.this.sectionType, (code, elementConsumer, context) ->
                    {
                        Overriding.this.sectionsParsed++;
                        return replaced.parse(code, elementConsumer, context);
                    });
                }
            });
        }
    }
}
