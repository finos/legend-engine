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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

public class TestLambdaPrettyRendering
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testRenderingEmptyCollectionInPrettyRendering()
    {
        testLambda("|Person.all()->project([])",
            "|Person.all()->project(\n" +
                "  []\n" +
                ")", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithProjectWithColInPrettyRendering()
    {
        testLambda("|Person.all()->filter(f|$f.name->startsWith('ok') && (true || 3 == 4))->project([col(p|$p.name, 'ok')])",
                "|Person.all()->filter(\n" +
                    "  f|$f.name->startsWith('ok') &&\n" +
                    "    (true || (3 == 4))\n" +
                    ")->project(\n" +
                    "  [\n" +
                    "    col(\n" +
                    "      p|$p.name,\n" +
                    "      'ok'\n" +
                    "    )\n" +
                    "  ]\n" +
                    ")", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithProjectWithColsInPrettyRendering()
    {
        testLambda("|Person.all()->filter(f|$f.name->startsWith('ok') && (true || 3 == 4))->project([col(p|$p.name, 'ok'), col(p|$p.name, 'ok2')])",
                "|Person.all()->filter(\n" +
                    "  f|$f.name->startsWith('ok') &&\n" +
                    "    (true || (3 == 4))\n" +
                    ")->project(\n" +
                    "  [\n" +
                    "    col(\n" +
                    "      p|$p.name,\n" +
                    "      'ok'\n" +
                    "    ),\n" +
                    "    col(\n" +
                    "      p|$p.name,\n" +
                    "      'ok2'\n" +
                    "    )\n" +
                    "  ]\n" +
                    ")", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithIfInPrettyRendering()
    {
        testLambda("|if($this.id == 'testing',|'test',|'nonTest')",
                "|if(\n" +
                    "  $this.id == 'testing',\n" +
                    "  |'test',\n" +
                    "  |'nonTest'\n" +
                    ")", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByInPrettyRendering()
    {
        testLambda("{|\n" +
                        "   let businessDate = now();\n" +
                        "   model::domain::referenceData::account::FirmAccount.all($businessDate)\n" +
                        "       ->groupBy([x | $x.trader($businessDate).lastName,\n" +
                        "                  x | $x.trader($businessDate).firstName,\n" +
                        "                  x | $x.trader($businessDate).isActive],\n" +
                        "                 [agg(x | $x.trader($businessDate).kerberos, y | $y->uniqueValueOnly())],\n" +
                        "                 ['Trader/Last Name',\n" +
                        "                  'Trader/First Name',\n" +
                        "                  'Trader/Is Active',\n" +
                        "                  'Trader/Kerberos Distinct Value']\n" +
                        "       );\n" +
                        "}",
                "{|\n" +
                    "  let businessDate = now();\n" +
                    "  model::domain::referenceData::account::FirmAccount.all($businessDate)->groupBy(\n" +
                    "    [\n" +
                    "      x|$x.trader($businessDate).lastName,\n" +
                    "      x|$x.trader($businessDate).firstName,\n" +
                    "      x|$x.trader($businessDate).isActive\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      agg(\n" +
                    "        x|$x.trader($businessDate).kerberos,\n" +
                    "        y|$y->uniqueValueOnly()\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      'Trader/Last Name',\n" +
                    "      'Trader/First Name',\n" +
                    "      'Trader/Is Active',\n" +
                    "      'Trader/Kerberos Distinct Value'\n" +
                    "    ]\n" +
                    "  );\n" +
                    "}", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByWithLatestInPrettyRendering()
    {
        testLambda("{|\n" +
                        "   let businessDate = now();\n" +
                        "   model::domain::referenceData::account::FirmAccount.all(%latest)\n" +
                        "       ->groupBy([x | $x.trader(%latest).lastName,\n" +
                        "                  x | $x.trader(%latest).firstName,\n" +
                        "                  x | $x.trader(%latest).isActive],\n" +
                        "                 [agg(x | $x.trader(%latest).kerberos, y | $y->uniqueValueOnly()),agg(x | $x.trader(%latest).age, y | $y->average())],\n" +
                        "                 ['Trader/Last Name',\n" +
                        "                  'Trader/First Name',\n" +
                        "                  'Trader/Is Active',\n" +
                        "                  'Trader/Kerberos Distinct Value']\n" +
                        "       );\n" +
                        "}",
                "{|\n" +
                    "  let businessDate = now();\n" +
                    "  model::domain::referenceData::account::FirmAccount.all(%latest)->groupBy(\n" +
                    "    [\n" +
                    "      x|$x.trader(%latest).lastName,\n" +
                    "      x|$x.trader(%latest).firstName,\n" +
                    "      x|$x.trader(%latest).isActive\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      agg(\n" +
                    "        x|$x.trader(%latest).kerberos,\n" +
                    "        y|$y->uniqueValueOnly()\n" +
                    "      ),\n" +
                    "      agg(\n" +
                    "        x|$x.trader(%latest).age,\n" +
                    "        y|$y->average()\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    [\n" +
                    "      'Trader/Last Name',\n" +
                    "      'Trader/First Name',\n" +
                    "      'Trader/Is Active',\n" +
                    "      'Trader/Kerberos Distinct Value'\n" +
                    "    ]\n" +
                    "  );\n" +
                    "}", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByWithLatestInPrettyHTMLRendering()
    {
        testLambda("{|\n" +
                        "   let businessDate = now();\n" +
                        "   model::domain::referenceData::account::FirmAccount.all(%latest)\n" +
                        "       ->groupBy([x | $x.trader(%latest).lastName,\n" +
                        "                  x | $x.trader(%latest).firstName,\n" +
                        "                  x | $x.trader(%latest).isActive],\n" +
                        "                 [agg(x | $x.trader(%latest).kerberos, y | $y->uniqueValueOnly()),agg(x | $x.trader(%latest).age, y | $y->average())],\n" +
                        "                 ['Trader/Last Name',\n" +
                        "                  'Trader/First Name',\n" +
                        "                  'Trader/Is Active',\n" +
                        "                  'Trader/Kerberos Distinct Value']\n" +
                        "       );\n" +
                        "}",
                "{|</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>let businessDate = <span class='pureGrammar-function'>now</span>();</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-package'>model::domain::referenceData::account::</span><span class='pureGrammar-packageableElement'>FirmAccount</span>.<span class='pureGrammar-function'>all</span>(%latest)<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>groupBy</span>(</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>lastName</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>firstName</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>isActive</span></BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>],</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>agg</span>(</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>kerberos</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>uniqueValueOnly</span>()</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>),</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>agg</span>(</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>age</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>average</span>()</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>)</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>],</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Last Name'</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/First Name'</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Is Active'</span>,</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Kerberos Distinct Value'</span></BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>]</BR>\n" +
                    "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>);</BR>\n" +
                    "}", PureGrammarComposerContext.RenderStyle.PRETTY_HTML);
    }

    @Test
    public void testComplexGroupByLambdaRoundtripInPrettyRendering()
    {
        testLambda("|test::Person.all()->filter(\n" +
            "  f|$f.name->startsWith('ok') &&\n" +
            "    (true || (3 == 4))\n" +
            ")->groupBy(\n" +
            "  [],\n" +
            "  [\n" +
            "    agg(\n" +
            "      x|$x.lastName,\n" +
            "      x|$x->distinct()->someFunc(\n" +
            "        true,\n" +
            "        'someString',\n" +
            "        90\n" +
            "      ) + $x.lastName\n" +
            "    )\n" +
            "  ],\n" +
            "  ['LastName']\n" +
            ")->distinct()->sort(\n" +
            "  [\n" +
            "    asc('LastName')\n" +
            "  ]\n" +
            ")->take(30)", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    private static void testLambda(String text, PureGrammarComposerContext.RenderStyle renderStyle)
    {
        testLambda(text, text, renderStyle);
    }

    private static void testLambda(String text, String formattedText, PureGrammarComposerContext.RenderStyle renderStyle)
    {
        Assert.assertEquals(formattedText, new DomainParser().parseLambda(text, "", true).accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle).build()));
    }
}
