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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

public class TestLambdaRoundtrip
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testLambdaWithBodyWithNonStringTokenOnly()
    {
        testLambda("|a::X");
    }

    @Test
    public void testLambdaWithParameter()
    {
        testLambda("s: String[1]|'ok'");
    }

    @Test
    public void testLambdaWithTwoParameters()
    {
        testLambda("{a,b|'ok'}");
    }

    @Test
    public void testLambdaWithBodyWithString()
    {
        testLambda("|'ok'");
    }

    @Test
    public void testLambdaWithBodyWithStringMany()
    {
        testLambda("|['ok', 'ok2']");
    }

    @Test
    public void testLambdaWithBodyWithInteger()
    {
        testLambda("|1");
    }

    @Test
    public void testLambdaWithBodyWithIntegerMany()
    {
        testLambda("|[1, 2]");
    }

    @Test
    public void testLambdaWithBodyWithBoolean()
    {
        testLambda("|true");
    }

    @Test
    public void testLambdaWithBodyWithBooleanMany()
    {
        testLambda("|[true, false]");
    }

    @Test
    public void testLambdaWithBodyWithFloat()
    {
        testLambda("|1.23");
    }

    @Test
    public void testLambdaWithBodyWithFloatMany()
    {
        testLambda("|[1.23, 1.54]");
    }

    @Test
    public void testLambdaWithBodyWithStrictDate()
    {
        testLambda("|%2010-10-20");
    }

    @Test
    public void testLambdaWithBodyWithStrictDateMany()
    {
        testLambda("|[%2010-10-20, %2010-10-22]");
    }

    @Test
    public void testLambdaWithBodyWithDateTime()
    {
        testLambda("|%2010-10-20T20:20:20.000");
    }

    @Test
    public void testLambdaWithBodyWithDateTimeMany()
    {
        testLambda("|[%2010-10-20T20:20:20.000, %2010-10-22T00:00:00.000]");
    }

    @Test
    public void testLambdaWithBodyWithStrictTime()
    {
        testLambda("|%22:22:22.1111");
    }

    @Test
    public void testLambdaWithBodyWithStrictTimeMany()
    {
        testLambda("|[%22:22:22.1, %09:20:21, %8:08:08]");
    }

    @Test
    public void testLambdaWithGetAll()
    {
        testLambda("|Person.all()->filter(p|true)");
    }

    @Test
    public void testLambdaWithFilterWithTypeInParam()
    {
        testLambda("|Person.all()->filter(p: Person[1]|true)");
    }

    @Test
    public void testLambdaWithFilterWithEqualExpression()
    {
        testLambda("|Person.all()->filter(p|$p.name == 'ok')");
    }

    @Test
    public void testLambdaWithFilterWithGreaterThanExpression()
    {
        testLambda("|Person.all()->filter(p|$p.age > 10)");
    }

    @Test
    public void testLambdaWithFilterWithGreaterThanEqualsExpression()
    {
        testLambda("|Person.all()->filter(p|$p.age >= 10)");
    }

    @Test
    public void testLambdaWithFilterWithLessThanExpression()
    {
        testLambda("|Person.all()->filter(p|$p.age < 10)");
    }

    @Test
    public void testLambdaWithFilterWithLessThanEqualsExpression()
    {
        testLambda("|Person.all()->filter(p|$p.age <= 10)");
    }

    @Test
    public void testLambdaWithFilterWithFunctionExpression()
    {
        testLambda("|Person.all()->filter(p|$p.name->startsWith('ok'))");
    }

    @Test
    public void testLambdaWithMapWithPlusFunctionExpression()
    {
        testLambda("|Person.all()->map(p|$p.name + 'testLambda')");
    }

    @Test
    public void testLambdaWithMultiPlus()
    {
        testLambda("|'ok' + 'b' + 'c'");
    }

    @Test
    public void testLambdaWithMultiTimes()
    {
        testLambda("|'ok' * 'b' * 'c'");
    }

    @Test
    public void testLambdaWithNot()
    {
        testLambda("|not(true && false)");
    }

    @Test
    public void testLambdaWithNotEquals()
    {
        testLambda("|not(true == false)");
    }

    @Test
    public void testLambdaWithAnd()
    {
        testLambda("|(1 < 2) && (2 < 3)");
    }

    @Test
    public void testLambdaWithMultiMinus()
    {
        testLambda("|'ok' - 'b' - 'c'");
    }

    @Test
    public void testLambdaWithMapWithMinusFunctionExpression()
    {
        testLambda("|Person.all().age->map(p|$p - 10)");
    }

    @Test
    public void testLambdaWithMapWithTimesFunctionExpression()
    {
        testLambda("|Person.all().age->map(p|$p * 10)");
    }

    @Test
    public void testLambdaWithMapWithDivFunctionExpression()
    {
        testLambda("|Person.all().age->map(p|$p / 10)");
    }

    @Test
    public void testLambdaWithLambdaWithEnumWithPackage()
    {
        testLambda("|a::b::MyEnum.ok");
    }

    @Test
    public void testLambdaWithEnum()
    {
        testLambda("|MyEnum.ok");
    }

    @Test
    public void testLambdaWithProjectWithCol()
    {
        testLambda("|Person.all()->filter(f|$f.name->startsWith('ok'))->project([col(p|$p.name, 'ok')])");
    }

    @Test
    public void testLambdaWithAndUsedAsACollectionFunction()
    {
        testLambda("|[true, false, false]->and()");
    }

    @Test
    public void testLambdaWithAndFromVariable()
    {
        testLambda("|$i->and()");
    }

    @Test
    public void testLambdaWithPlusFromVariable()
    {
        testLambda("|$i->plus()");
    }

    @Test
    public void testGraphFetch()
    {
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId,lei,eti}}#)");
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId,'ok':lei,eti}}#)");
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId{other,and{more}},lei,eti}}#)");
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId(true),lei,eti}}#)");
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId(true),lei($var),eti('yo'),opi(1.23)}}#)");
        testLambda("|MifidCDTModel.all()->graphFetch(#{MifidCDTModel{cPrimeId(true),lei->subType(@SomeClass),eti('yo'),opi(1.23)->subType(@SomeOtherClass){and{more}}}}#)");
    }

    @Test
    public void testDslNavigationPath()
    {
        testLambda("|$this.employees.lastName->sortBy(#/model::Person/lastName#)->joinStrings('')");
        testLambda("|#/Person/nameWithPrefixAndSuffix('a', 'b')#");
        testLambda("|#/Person/nameWithPrefixAndSuffix('a', ['a', 'b'])#");
        testLambda("|#/Person/nameWithPrefixAndSuffix([], ['a', 'b'])#");
        testLambda("|#/Person/nameWithPrefixAndSuffix('a', [1, 2])#");
    }

    @Test
    public void testLambdaWithProjectWithColInPrettyRendering()
    {
        testLambdaWithFormat("|Person.all()->filter(f|$f.name->startsWith('ok') && (true || 3 == 4))->project([col(p|$p.name, 'ok')])",
                "|Person.all()\n" +
                        "   ->filter\n" +
                        "    (\n" +
                        "      f|$f.name->startsWith('ok') &&\n" +
                        "         (true ||\n" +
                        "         (3 == 4))\n" +
                        "    )\n" +
                        "   ->project\n" +
                        "    (\n" +
                        "      [\n" +
                        "        col(p|$p.name, 'ok')\n" +
                        "      ]\n" +
                        "    )", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithProjectWithColsInPrettyRendering()
    {
        testLambdaWithFormat("|Person.all()->filter(f|$f.name->startsWith('ok') && (true || 3 == 4))->project([col(p|$p.name, 'ok'), col(p|$p.name, 'ok2')])",
                "|Person.all()\n" +
                        "   ->filter\n" +
                        "    (\n" +
                        "      f|$f.name->startsWith('ok') &&\n" +
                        "         (true ||\n" +
                        "         (3 == 4))\n" +
                        "    )\n" +
                        "   ->project\n" +
                        "    (\n" +
                        "      [\n" +
                        "        col(p|$p.name, 'ok'), \n" +
                        "        col(p|$p.name, 'ok2')\n" +
                        "      ]\n" +
                        "    )", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithIfInPrettyRendering()
    {
        testLambdaWithFormat("|if($this.id == 'testing',|'test',|'nonTest')",
                "|if($this.id == 'testing', \n" +
                        "   |'test', \n" +
                        "   |'nonTest'\n" +
                        ")", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByInPrettyRendering()
    {
        testLambdaWithFormat("{|\n" +
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
                        "  model::domain::referenceData::account::FirmAccount.all($businessDate)\n" +
                        "     ->groupBy\n" +
                        "      (\n" +
                        "        [\n" +
                        "          x|$x.trader($businessDate).lastName, \n" +
                        "          x|$x.trader($businessDate).firstName, \n" +
                        "          x|$x.trader($businessDate).isActive\n" +
                        "        ], \n" +
                        "        [\n" +
                        "          agg(x|$x.trader($businessDate).kerberos, y|$y->uniqueValueOnly())\n" +
                        "        ], \n" +
                        "        [\n" +
                        "          'Trader/Last Name', \n" +
                        "          'Trader/First Name', \n" +
                        "          'Trader/Is Active', \n" +
                        "          'Trader/Kerberos Distinct Value'\n" +
                        "        ]\n" +
                        "      );\n" +
                        "}", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByWithLatestInPrettyRendering()
    {
        testLambdaWithFormat("{|\n" +
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
                        "  model::domain::referenceData::account::FirmAccount.all(%latest)\n" +
                        "     ->groupBy\n" +
                        "      (\n" +
                        "        [\n" +
                        "          x|$x.trader(%latest).lastName, \n" +
                        "          x|$x.trader(%latest).firstName, \n" +
                        "          x|$x.trader(%latest).isActive\n" +
                        "        ], \n" +
                        "        [\n" +
                        "          agg(x|$x.trader(%latest).kerberos, y|$y->uniqueValueOnly()), \n" +
                        "          agg(x|$x.trader(%latest).age, y|$y->average())\n" +
                        "        ], \n" +
                        "        [\n" +
                        "          'Trader/Last Name', \n" +
                        "          'Trader/First Name', \n" +
                        "          'Trader/Is Active', \n" +
                        "          'Trader/Kerberos Distinct Value'\n" +
                        "        ]\n" +
                        "      );\n" +
                        "}", PureGrammarComposerContext.RenderStyle.PRETTY);
    }

    @Test
    public void testLambdaWithGroupByWithLatestInPrettyHTMLRendering()
    {
        testLambdaWithFormat("{|\n" +
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
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-package'>model::domain::referenceData::account::</span><span class='pureGrammar-packageableElement'>FirmAccount</span>.<span class='pureGrammar-function'>all</span>(%latest)</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>groupBy</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>(</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>lastName</span>, </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>firstName</span>, </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>isActive</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>], </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>agg</span>(<span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>kerberos</span>, <span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>uniqueValueOnly</span>()), </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>agg</span>(<span class='pureGrammar-var'>x</span>|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>trader</span>(%latest).<span class=pureGrammar-property>age</span>, <span class='pureGrammar-var'>y</span>|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>average</span>())</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>], </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Last Name'</span>, </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/First Name'</span>, </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Is Active'</span>, </BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Trader/Kerberos Distinct Value'</span></BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>]</BR>\n" +
                        "<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>);</BR>\n" +
                        "}", PureGrammarComposerContext.RenderStyle.PRETTY_HTML);
    }

    @Test
    public void testAppliedFunctionWithParametersUsingInfixOperations()
    {
        testLambda("|'data1' + toString('data1'->someTransformation::fn() + 1)");
    }

    @Test
    public void testFunctionNameWithRichIdentifier()
    {
        testLambda("|'data1'->someTransformation::fn() + 1");
        testLambda("|'data1'->someTransformation::fn::'special function 1'() + 1");
    }

    @Test
    public void testLambdaWithCast()
    {
        testLambda("src: OldClass[1]|$src->cast(@newClass)");
    }

    @Test
    public void testLambdaWithQuotedPropertyAccess()
    {
        testLambda("|$this.'1@3'->isEmpty()");
    }

    private void testLambda(String string)
    {
        Lambda postJSON_lambda;
        try
        {
            Lambda lambda = new DomainParser().parseLambda(string, "");
            String json = objectMapper.writeValueAsString(lambda);
            postJSON_lambda = objectMapper.readValue(json, Lambda.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(string, postJSON_lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()));
    }

    private void testLambdaWithFormat(String string, String toCompare, PureGrammarComposerContext.RenderStyle renderStyle)
    {
        Assert.assertEquals(toCompare, new DomainParser().parseLambda(string, "").accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(renderStyle).build()));
    }
}
