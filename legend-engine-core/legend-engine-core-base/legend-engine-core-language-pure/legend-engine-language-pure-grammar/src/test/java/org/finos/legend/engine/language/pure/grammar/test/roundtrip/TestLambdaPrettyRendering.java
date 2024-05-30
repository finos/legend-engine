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
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
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
                        ")", RenderStyle.PRETTY);
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
                        ")", RenderStyle.PRETTY);
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
                        ")", RenderStyle.PRETTY);
    }

    @Test
    public void testPrettyRelationLambda()
    {
        testLambda("|Customer.all()->project(~[companyName:x|$x.companyName, 'Company Title':x|$x.companyTitle, contactName:x|$x.contactName, faxNumber:x|$x.faxNumber, id:x|$x.id, telephoneNumber:x|$x.telephoneNumber])",
                "|Customer.all()->project(\n" +
                            "  ~[\n" +
                            "     companyName: x|$x.companyName,\n" +
                            "     'Company Title': x|$x.companyTitle,\n" +
                            "     contactName: x|$x.contactName,\n" +
                            "     faxNumber: x|$x.faxNumber,\n" +
                            "     id: x|$x.id,\n" +
                            "     telephoneNumber: x|$x.telephoneNumber\n" +
                            "   ]\n" +
                            ")", RenderStyle.PRETTY);
    }

    @Test
    public void testQuotedColumnNameInRelationLambda()
    {
        testLambda("|Customer.all()->project(~['Company Name':x|$x.companyName, companyTitle:x|$x.companyTitle, contactName:x|$x.contactName, faxNumber:x|$x.faxNumber, id:x|$x.id, telephoneNumber:x|$x.telephoneNumber])",
                "|Customer.all()->project(\n" +
                        "  ~[\n" +
                        "     'Company Name': x|$x.companyName,\n" +
                        "     companyTitle: x|$x.companyTitle,\n" +
                        "     contactName: x|$x.contactName,\n" +
                        "     faxNumber: x|$x.faxNumber,\n" +
                        "     id: x|$x.id,\n" +
                        "     telephoneNumber: x|$x.telephoneNumber\n" +
                        "   ]\n" +
                        ")", RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithIfInPrettyRendering()
    {
        testLambda("|if($this.id == 'testing',|'test',|'nonTest')",
                "|if(\n" +
                        "  $this.id == 'testing',\n" +
                        "  |'test',\n" +
                        "  |'nonTest'\n" +
                        ")", RenderStyle.PRETTY);
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
                        "}", RenderStyle.PRETTY);
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
                        "}", RenderStyle.PRETTY);
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
                        "}", RenderStyle.PRETTY_HTML);
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
                ")->take(30)", RenderStyle.PRETTY);
    }

    @Test
    public void testOlapGroupByRendering()
    {
        testLambda("var: String[1]|Person.all()->filter(\n" +
                        "  p|$p.firstName ==\n" +
                        "    $var\n" +
                        ")->project(\n" +
                        "  [\n" +
                        "    col(\n" +
                        "      p|$p.firstName,\n" +
                        "      'firstName'\n" +
                        "    ),\n" +
                        "    col(\n" +
                        "      p|$p.lastName,\n" +
                        "      'lastName'\n" +
                        "    ),\n" +
                        "    col(\n" +
                        "      p|$p.lastName->length(),\n" +
                        "      'age'\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")->olapGroupBy(\n" +
                        "  ['firstName'],\n" +
                        "  desc('lastName'),\n" +
                        "  y|$y->rank(),\n" +
                        "  'testCol1'\n" +
                        ")->olapGroupBy(\n" +
                        "  ['firstName'],\n" +
                        "  asc('lastName'),\n" +
                        "  'age'->func(\n" +
                        "    y|$y->sum()\n" +
                        "  ),\n" +
                        "  'testCol3'\n" +
                        ")",
                 RenderStyle.PRETTY);

        testLambda("var: String[1]|Person.all()->filter(\n" +
                        "  p|$p.firstName ==\n" +
                        "    $var\n" +
                        ")->project(\n" +
                        "  [\n" +
                        "    col(\n" +
                        "      p|$p.firstName,\n" +
                        "      'firstName'\n" +
                        "    ),\n" +
                        "    col(\n" +
                        "      p|$p.lastName,\n" +
                        "      'lastName'\n" +
                        "    ),\n" +
                        "    col(\n" +
                        "      p|$p.lastName->length(),\n" +
                        "      'age'\n" +
                        "    )\n" +
                        "  ]\n" +
                        ")->olapGroupBy(\n" +
                        "  ['firstName'],\n" +
                        "  desc('lastName'),\n" +
                        "  y|$y->rank(),\n" +
                        "  'testCol1'\n" +
                        ")->olapGroupBy(\n" +
                        "  ['firstName'],\n" +
                        "  asc('lastName'),\n" +
                        "  'age'->func(\n" +
                        "    y|$y->sum()\n" +
                        "  ),\n" +
                        "  'testCol3'\n" +
                        ")",
                "<span class='pureGrammar-var'>var</span>: <span class='pureGrammar-packageableElement'>String</span>[1]|<span class='pureGrammar-packageableElement'>Person</span>.<span class='pureGrammar-function'>all</span>()<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>filter</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>p</span>|<span class='pureGrammar-var'>$p</span>.<span class=pureGrammar-property>firstName</span> ==</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>$var</span></BR>\n" +
                        ")<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>project</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>col</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>p</span>|<span class='pureGrammar-var'>$p</span>.<span class=pureGrammar-property>firstName</span>,</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'firstName'</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>col</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>p</span>|<span class='pureGrammar-var'>$p</span>.<span class=pureGrammar-property>lastName</span>,</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'lastName'</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>col</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>p</span>|<span class='pureGrammar-var'>$p</span>.<span class=pureGrammar-property>lastName</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>length</span>(),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'age'</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>)</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>]</BR>\n" +
                        ")<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>olapGroupBy</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[<span class='pureGrammar-string'>'firstName'</span>],</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>desc</span>(<span class='pureGrammar-string'>'lastName'</span>),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>rank</span>(),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'testCol1'</span></BR>\n" +
                        ")<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>olapGroupBy</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[<span class='pureGrammar-string'>'firstName'</span>],</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>asc</span>(<span class='pureGrammar-string'>'lastName'</span>),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'age'</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>func</span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>sum</span>()</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>),</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'testCol3'</span></BR>\n" +
                        ")",
                 RenderStyle.PRETTY_HTML);


    }

    @Test
    public void testLambdaWithoutSubTypeGraphFetchTree()
    {
        testLambda("|test::Address.all()->graphFetch(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id\n" +
                "    }\n" +
                "  }#\n" +
                ")", RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithOnlySubTypeGraphFetchTree()
    {
        testLambda("|test::Address.all()->graphFetch(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      ->subType(@test::Street){\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      ->subType(@test::Street){\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")", RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithSubTypeAndPropertyGraphFetchTree()
    {
        testLambda("|test::Address.all()->graphFetch(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")", RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithSubTypeGraphFetchTreeHavingMultipleProperties()
    {
        testLambda("|test::Address.all()->graphFetch(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street,\n" +
                "        Coordinates\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street,\n" +
                "        Coordinates\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")", RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithMultipleSubTypeGraphFetchTree()
    {
        testLambda("|test::Address.all()->graphFetch(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street,\n" +
                "        Coordinates\n" +
                "      },\n" +
                "      ->subType(@test::City){\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    test::Address{\n" +
                "      Id,\n" +
                "      ->subType(@test::Street){\n" +
                "        street,\n" +
                "        Coordinates\n" +
                "      },\n" +
                "      ->subType(@test::City){\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                ")", RenderStyle.PRETTY);
    }

    private static void testLambda(String text, RenderStyle renderStyle)
    {
        testLambda(text, text, renderStyle);
    }

    private static void testLambda(String text, String formattedText, RenderStyle renderStyle)
    {
        Assert.assertEquals(formattedText, new DomainParser().parseLambda(text, "", 0, 0, true).accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle).build()));
    }
}
