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

package org.finos.legend.engine.language.pure.compiler.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestCompilationFromGrammar
{
    public abstract static class TestCompilationFromGrammarTestSuite
    {
        protected abstract String getDuplicatedElementTestCode();

        protected abstract String getDuplicatedElementTestExpectedErrorMessage();

        @Test
        public void testDuplicatedElement()
        {
            test(this.getDuplicatedElementTestCode(), this.getDuplicatedElementTestExpectedErrorMessage());
        }

        public static Pair<PureModelContextData, PureModel> test(String str)
        {
            return test(str, null);
        }

        public static Pair<PureModelContextData, PureModel> test(String str, String expectedErrorMsg)
        {
            return test(str, expectedErrorMsg, null);
        }

        public static Pair<PureModelContextData, PureModel> test(String str, String expectedErrorMsg, List<String> expectedWarnings)
        {
            try
            {
                // do a full re-serialization after parsing to make sure the protocol produced is proper
                PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(str);
                if (expectedErrorMsg == null && (expectedWarnings == null || expectedWarnings.isEmpty()))
                {
                    ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                    String json = objectMapper.writeValueAsString(modelData);
                    modelData = objectMapper.readValue(json, PureModelContextData.class);
                }
                PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
                modelData.getElements().parallelStream().forEach(pureModel::getPackageableElement);
                if (expectedErrorMsg != null)
                {
                    Assert.fail("Expected compilation error with message: " + expectedErrorMsg + "; but no error occurred");
                }

                if (expectedWarnings == null)
                {
                    Assert.assertTrue("expected no warnings but found " + pureModel.getWarnings().stream().map(Warning::buildPrettyWarningMessage).collect(Collectors.toList()), pureModel.getWarnings().isEmpty());
                }

                if (expectedWarnings != null)
                {
                    List<String> warnings = pureModel.getWarnings().stream().map(Warning::buildPrettyWarningMessage).sorted().collect(Collectors.toList());
                    Collections.sort(expectedWarnings);
                    Assert.assertEquals(expectedWarnings, warnings);
                }

                return Tuples.pair(modelData, pureModel);
            }
            catch (EngineException e)
            {
                if (expectedErrorMsg == null)
                {
                    throw e;
                }
                Assert.assertNotNull("No source information provided in error", e.getSourceInformation());
                MatcherAssert.assertThat(EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(),
                        e.getErrorType()), CoreMatchers.startsWith(expectedErrorMsg));
                return null;
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public static Pair<PureModelContextData, PureModel> partialCompilationTest(String str)
        {
            return partialCompilationTest(str, null);
        }

        public static Pair<PureModelContextData, PureModel> partialCompilationTest(String str, List<String> expectedEngineExceptions)
        {
            return partialCompilationTest(str, expectedEngineExceptions, null);
        }

        public static Pair<PureModelContextData, PureModel> partialCompilationTest(String str, List<String> expectedEngineExceptions, List<String> expectedWarnings)
        {
            try
            {
                // do a full re-serialization after parsing to make sure the protocol produced is proper
                PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(str);
                if ((expectedEngineExceptions == null || expectedEngineExceptions.isEmpty()) && (expectedWarnings == null || expectedWarnings.isEmpty()))
                {
                    ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                    String json = objectMapper.writeValueAsString(modelData);
                    modelData = objectMapper.readValue(json, PureModelContextData.class);
                }
                PureModelProcessParameter pureModelProcessParameter = PureModelProcessParameter.newBuilder().withEnablePartialCompilation(true).build();
                PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName(), null, pureModelProcessParameter);
                modelData.getElements().parallelStream().forEach(pureModel::getPackageableElement);

                Set<String> engineExceptions = pureModel.getEngineExceptions().stream().map(EngineException::toPretty).collect(Collectors.toSet());
                if (expectedEngineExceptions != null)
                {
                    ImmutableSet<String> expectedEngineExceptionsSet = Sets.immutable.withAll(expectedEngineExceptions);
                    Assert.assertEquals(expectedEngineExceptionsSet, engineExceptions);
                }
                else
                {
                    Assert.assertTrue("expected no engine exceptions but found " + engineExceptions, engineExceptions.isEmpty());
                }

                if (expectedWarnings == null)
                {
                    Assert.assertTrue("expected no warnings but found " + pureModel.getWarnings().stream().map(Warning::buildPrettyWarningMessage).collect(Collectors.toList()), pureModel.getWarnings().isEmpty());
                }

                if (expectedWarnings != null)
                {
                    List<String> warnings = pureModel.getWarnings().stream().map(Warning::buildPrettyWarningMessage).sorted().collect(Collectors.toList());
                    Collections.sort(expectedWarnings);
                    Assert.assertEquals(expectedWarnings, warnings);
                }

                return Tuples.pair(modelData, pureModel);
            }
            catch (EngineException e)
            {
                if (expectedEngineExceptions == null)
                {
                    throw e;
                }
                Assert.assertEquals("There should only be one expected engine exception but was provided " + expectedEngineExceptions.size(), 1, expectedEngineExceptions.size());
                Assert.assertNotNull("No source information provided in error", e.getSourceInformation());
                MatcherAssert.assertThat(EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(),
                        e.getErrorType()), CoreMatchers.startsWith(expectedEngineExceptions.get(0)));
                return null;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testCachingSearchImportsResults()
    {
        CompileContext context = new CompileContext.Builder(new CompileContext.Builder(new PureModel(new PureModelContextData.Builder().build(), null, DeploymentMode.PROD))
                .withImports(Sets.immutable.with("meta::pure::precisePrimitives"))
                .build()).withImports(Sets.immutable.with("meta::pure::precisePrimitives")).build();
        GenericType bigInt = new GenericType(new PackageableType("BigInt"), Lists.fixedSize.empty(), Lists.fixedSize.empty(),
                Lists.fixedSize.of(new Multiplicity(1, 1)));
        long timeBefore = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++)
        {
            context.newGenericType(bigInt);
        }
        long timeAfter = System.currentTimeMillis();
        long timeTaken = timeAfter - timeBefore;
        Assert.assertTrue("With caching in the compiler, it is expected to take no longer than 3000ms, but took: " + timeTaken, timeTaken < 3000);
    }

    @Test
    public void testCompilationFromGrammarWithParsingError()
    {
        TestCompilationFromGrammarTestSuite.test("Class example::MyTest\n" +
                "{\n" +
                "p:String[1];\n" +
                "}\n" +
                "###Pure\n" +
                "importd example::*;\n" +
                "function example::testMatch(test:MyTest[1]): MyTest[1]\n" +
                "{\n" +
                "  $test->match([ a:MyTest[1]|$a ]);\n" +
                "}", "PARSER error at [6:1-7]: Unexpected token 'importd'");
    }

    @Test
    public void testCompilePathVariable()
    {
        TestCompilationFromGrammarTestSuite.test("Class test::Person\n" +
                "{\n" +
                "  firstName:String[1];\n" +
                "  age: Integer[1];\n" +
                "}\n" +
                "\n" +
                "function test::usePath(p:test::Person[1]):Boolean[1]\n" +
                "{\n" +
                "  let path = #/test::Person/firstName#;\n" +
                "  test::Person.all()->project($path)->filter(r|$r.getInteger('age') > 30 && $r.isNotNull('age'))->isNotEmpty();\n" +
                "}", null);
    }

    @Test
    public void testCompileWithDefaultValue()
    {
        TestCompilationFromGrammarTestSuite.test("Class test::Person\n" +
                "{\n" +
                "  firstName:String[1];\n" +
                "  age: Integer[1] = 5;\n" +
                "}\n", null);
    }

    @Test
    public void testCompilePathVariableMultipleAssignments()
    {
        TestCompilationFromGrammarTestSuite.test("Class test::Person\n" +
                "{\n" +
                "  firstName:String[1];\n" +
                "  age: Integer[1];\n" +
                "}\n" +
                "\n" +
                "function test::usePath(p:test::Person[1]):Boolean[1]\n" +
                "{\n" +
                "  let path = #/test::Person/firstName#;\n" +
                "  let x = $path;\n" +
                "  test::Person.all()->project($x)->filter(r|$r.getInteger('age') > 30 && $r.isNotNull('age'))->isNotEmpty();\n" +
                "}", null);
    }

    @Test
    public void testCompileLambdaVariable()
    {
        TestCompilationFromGrammarTestSuite.test("function test::func(): Any[*]\n" +
                "{\n" +
                "  let s = {| 'sample string'}; $s->eval();\n" +
                "} ", null);
    }

    @Test
    public void testCompileEvalUsingPackageableElement()
    {
        TestCompilationFromGrammarTestSuite.test("function test::func(): Any[*]\n" +
                "{\n" +
                "  meta::pure::functions::math::acos_Number_1__Float_1_->meta::pure::functions::lang::eval(0.5);\n" +
                "} ", null);
    }

    @Test
    public void testCompilationFromGrammarWithMergeOperation()
    {
        TestCompilationFromGrammarTestSuite.test("Class  example::SourcePersonWithFirstName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::SourcePersonWithLastName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class example::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +

                "\n" +

                "###Mapping\n" +
                "Mapping  example::MergeModelMappingSourceWithMatch\n" +
                "(\n" +
                "   *example::Person : Operation\n" +
                "           {\n" +
                "             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })\n" +

                "           }\n" +
                "\n" +
                "   example::Person[p1] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithFirstName\n" +
                "               firstName : $src.firstName\n" +
                "            }\n" +
                "\n" +
                "   example::Person[p2] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +
                "   example::Person[p3] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +

                "\n" +
                ")");
    }

    @Test
    public void testCompilationPureDuplicateSetIdError()
    {
        TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "Class simple::Account\n" +
                "{\n" +
                "   id: String[1];   \n" +
                "}\n" +
                "\n" +
                "Class simple::Raw_Account\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class simple::Another extends simple::Account\n" +
                "{  \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping simple::gen1::map\n" +
                "(\n" +
                "   simple::Account[id]: Pure\n" +
                "   {\n" +
                "      ~src simple::Raw_Account\n" +
                "      id: $src.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::gen2::map\n" +
                "(\n" +
                "   simple::Account[id]: Pure\n" +
                "   {\n" +
                "      ~src simple::Raw_Account\n" +
                "      id: $src.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::merged(\n" +
                "   include simple::gen1::map\n" +
                "   include simple::gen2::map\n" +
                "      \n" +
                "   simple::Another extends [id]: Pure\n" +
                "   {      \n" +
                "   }   \n" +
                ")", "COMPILATION error at [39:4-41:4]: Duplicated class mappings found with ID 'id' in mapping 'simple::merged'; parent mapping for duplicated: 'simple::gen1::map', 'simple::gen2::map'");
    }


    @Test
    public void testCompilationAnnualized()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | annualized($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationCme()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | cme($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationCw()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | cw($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationCw_Fm()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | cw_fm($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationCyminus2()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | CYMinus2($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationCyminus3()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | CYMinus3($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationMtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | mtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP12Wa()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p12wa($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP12Wtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p12wtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP4Wa()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p4wa($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP4Wtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p4wtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP52Wtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p52wtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP52Wa()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p52wa($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationP12mtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | p12mtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPma()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pma($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPmtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pmtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPqtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pqtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPriorday()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | priorDay($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPrioryear()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | priorYear($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPw()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pw($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPw_Fm()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pw_fm($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPwa()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pwa($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPwtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pwtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPymtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pymtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPyqtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pyqtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPytd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pytd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPywa()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pywa($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationPywtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | pywtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationQtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | qtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationReportendday()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | reportEndDay($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationWtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | wtd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationYtd()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Employee\n" +
                        "{\n" +
                        "   id       : Integer[1];\n" +
                        "   hireDate : Date[1];\n" +
                        "   hireType : String[1];\n" +
                        "   fteFactor: Float[1];\n" +
                        "   firmName : String[0..1];\n" +
                        "}\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{ test::Employee.all()->groupBy(" +
                        "[p|$p.hireDate]," +
                        "[ agg(p | ytd($p.hireDate, 'NY', %2022-11-16, $p.fteFactor), y | $y->sum()) ]," +
                        "['includedDate',  'calendarAgg'])" +
                        "}");
    }

    @Test
    public void testCompilationDate()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function example::testYtd(): Any[*]\n" +
                        "{" +
                        "   |(%2014 == %2014);" +
                        "}");
    }

    @Test
    public void testRelation()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Report{country : String[1]; city: String[1]; year: Integer[1]; treePlanted: Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Report.all()->project(~[\n" +
                        "        country: x|$x.country,\n" +
                        "        city : x|$x.city,\n" +
                        "        treePlanted: x|$x.treePlanted,\n" +
                        "        year: x|$x.year\n" +
                        "      ])->pivot(~[country, city], ~[total: x|$x.treePlanted : x|$x->sum()])->cast(@meta::pure::metamodel::relation::Relation<(year2:Integer)>)->filter(x|$x.year2 == 2000)\n" +
                        "}"
        );
    }

    @Test
    public void testDateFunctionTypeInference()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::func(param1: Date[1], param2:Date[1]):Any[*]" +
                        "{" +
                        "   max([$param1, $param2])->toString()" +
                        "}","COMPILATION error at [2:86-93]: Can't find a match for function 'toString(Date[0..1])'"
        );
    }

    @Test
    public void testTypeVariables()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Report" +
                        "{" +
                        "   country : meta::pure::precisePrimitives::Varchar(200)[1];" +
                        "}"
        );
    }

    @Test
    public void testTypeVariablesError()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Report" +
                        "{" +
                        "   country : meta::pure::precisePrimitives::Varchar(200,1)[1];" +
                        "}",
                "COMPILATION error at [2:33-77]: Wrong type variables count (2) for type: Varchar(x:Integer)"
        );
    }

    @Test
    public void testTypeVariablesErrorType()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class test::Report" +
                        "{" +
                        "   country : meta::pure::precisePrimitives::Varchar('200')[1];" +
                        "}",
                "COMPILATION error at [2:33-77]: Error for type: Varchar(x:Integer), '200' is not an instance of Integer"
        );
    }

    @Test
    public void testTypeVariablesErrorInFunction()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::a(x:meta::pure::precisePrimitives::Varchar('200')[1]):String[1]" +
                        "{" +
                        "   'x';" +
                        "}",
                "COMPILATION error at [2:17-61]: Error for type: Varchar(x:Integer), '200' is not an instance of Integer"
        );
    }

    @Test
    public void testTypeVariableInRelation()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::a():meta::pure::metamodel::relation::Relation<(x:meta::pure::precisePrimitives::Varchar(200))>[1]" +
                        "{" +
                        "   []->cast(@meta::pure::metamodel::relation::Relation<(x:meta::pure::precisePrimitives::Varchar('200'))>);" +
                        "}",
                "COMPILATION error at [2:169-213]: Error for type: Varchar(x:Integer), '200' is not an instance of Integer"
        );
    }

    @Test
    public void testTypeVariablesFunctionMatching()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::a(x:meta::pure::precisePrimitives::Varchar(200)[1]):Boolean[1]" +
                        "{" +
                        "   true;" +
                        "}" +
                        "function x::call():Boolean[1]" +
                        "{" +
                        "   x::a('www'->cast(@meta::pure::precisePrimitives::Varchar(200)));" +
                        "}"
        );
    }

    @Test
    public void testTypeVariablesFunctionMatchingError()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::a(x:meta::pure::precisePrimitives::Varchar(200)[1]):Boolean[1]" +
                        "{" +
                        "   true;" +
                        "}" +
                        "function x::call():Boolean[1]" +
                        "{" +
                        "   x::a('www'->cast(@meta::pure::precisePrimitives::Varchar(250)));" +
                        "}",
                "COMPILATION error at [2:118-121]: Can't find a match for function 'x::a(Varchar(250)[1])"
        );
    }

    @Test
    public void testTypeVariablesFunctionMatchingNative()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::call():Integer[1]" +
                        "{" +
                        "   1->cast(@meta::pure::precisePrimitives::TinyInt) + 1;" +
                        "}"
        );
    }

    @Test
    public void testTypeVariablesFunctionMatchingNativeError()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function x::call():Integer[1]" +
                        "{" +
                        "   1->cast(@meta::pure::precisePrimitives::TinyInt) + '1';" +
                        "}", "COMPILATION error at [2:83-87]: Can't find a match for function 'plus(Any[2])'"
        );
    }
}
