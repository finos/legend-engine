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

package org.finos.legend.engine.sql.compiler.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.sql.metamodel.NamedArgumentExpression;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.sql.compiler.FunctionTableFunctionCompilerExtension;
import org.finos.legend.engine.sql.compiler.StoreTableFunctionCompilerExtension;
import org.finos.legend.engine.sql.compiler.TableFunctionSimpleElementExtractor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class TestSQLExpressionServiceSource
{
    private static final String MODEL =
            "###Relational\n" +
            "Database pack::DB(Table T(a INT, b INT, c VARCHAR(10)))\n" +
            "\n" +
            "###Pure\n" +
            "Class pack::Person\n" +
            "{\n" +
            "  name: String[0..1];\n" +
            "  age: Integer[0..1];\n" +
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping pack::M\n" +
            "(\n" +
            "  pack::Person: Relational\n" +
            "  {\n" +
            "    ~mainTable [pack::DB]T\n" +
            "    name: [pack::DB]T.c,\n" +
            "    age: [pack::DB]T.a\n" +
            "  }\n" +
            ")\n" +
            "\n" +
            "###Connection\n" +
            "RelationalDatabaseConnection pack::Conn\n" +
            "{\n" +
            "  store: pack::DB;\n" +
            "  type: H2;\n" +
            "  specification: LocalH2{};\n" +
            "  auth: DefaultH2;\n" +
            "}\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime pack::Rt\n" +
            "{\n" +
            "  mappings: [pack::M];\n" +
            "  connections:\n" +
            "  [\n" +
            "    pack::DB: [ c: pack::Conn ]\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "###Service\n" +
            service("pack::RelationService", "/relation", "|#>{pack::DB.T}#->select(~[a, b])") +
            service("pack::TdsService", "/tds", "|pack::Person.all()->project([p|$p.name, p|$p.age], ['name', 'age'])") +
            service("pack::ParamService", "/params", "min: Integer[1]|#>{pack::DB.T}#->filter(r|$r.a->toOne() > $min)->select(~[a, b])") +
            "Service pack::MultiService\n" +
            "{\n" +
            "  pattern: '/multi';\n" +
            "  owners: ['owner'];\n" +
            "  documentation: '';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Multi\n" +
            "  {\n" +
            "    query: |#>{pack::DB.T}#->select(~[b, c]);\n" +
            "    key: 'env';\n" +
            "    executions['PROD']:\n" +
            "    {\n" +
            "      mapping: pack::M;\n" +
            "      runtime: pack::Rt;\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testServiceReturningRelation(boolean parallel)
    {
        PureModel pureModel = compile(MODEL + reader("select b from service('/relation')"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceReturningTabularDataSetIsReadAsARelation()
    {
        PureModel pureModel = compile(MODEL + reader("select name, age from service(pattern => '/tds')"), false);
        Assertions.assertEquals(Lists.mutable.with("name:String[0..1]", "age:Integer[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceWithParameter()
    {
        PureModel pureModel = compile(MODEL + reader("select a from service('/params', min => 1)"), false);
        Assertions.assertEquals(Lists.mutable.with("a:Int[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceWithMultipleExecutionsUsesTheKeyedExecution()
    {
        PureModel pureModel = compile(MODEL + reader("select c from service('/multi', env => 'PROD')"), false);
        Assertions.assertEquals(Lists.mutable.with("c:Varchar[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testUnknownServicePattern()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(MODEL + reader("select * from service('/missing')"), false));
        Assertions.assertTrue(e.getMessage().contains("Can't find a service with pattern '/missing'"), e.getMessage());
    }

    @Test
    public void testTwoServicesWithThePatternAreAnError()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(MODEL +
                "###Service\n" +
                service("pack::DuplicateB", "/duplicate", "|#>{pack::DB.T}#->select(~[a])") +
                service("pack::DuplicateA", "/duplicate", "|#>{pack::DB.T}#->select(~[a])") +
                reader("select * from service('/duplicate')"), false));
        Assertions.assertTrue(e.getMessage().contains("Found 2 services with pattern '/duplicate': pack::DuplicateA, pack::DuplicateB"), e.getMessage());
    }

    @Test
    public void testServiceWithEmbeddedRuntimeIsAnError()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(MODEL +
                "###Service\n" +
                "Service pack::EmbeddedRuntimeService\n" +
                "{\n" +
                "  pattern: '/embedded';\n" +
                "  owners: ['owner'];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |#>{pack::DB.T}#->select(~[a]);\n" +
                "    mapping: pack::M;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        pack::DB: [ c: pack::Conn ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "}\n" +
                reader("select * from service('/embedded')"), false));
        Assertions.assertTrue(e.getMessage().contains("service '/embedded' uses an embedded runtime, which service sources do not support; use a PackageableRuntime"), e.getMessage());
    }

    @Test
    public void testServiceCallingTheFunctionThatReadsItIsACycle()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(MODEL +
                "###Service\n" +
                service("pack::CyclicService", "/cyclic", "|pack::cyclicReader()") +
                "###Pure\n" +
                "function pack::cyclicReader():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                "{\n" +
                "  #SQL{select * from service('/cyclic')}#\n" +
                "}\n", false));
        Assertions.assertTrue(e.getMessage().contains("Detected a circular dependency in element prerequisites graph!"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testFuncSourceReadingAFunctionThatReadsAService(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                serviceReadingFunction("pack::middle", "select b from service('/relation')") +
                reader("select b from func('pack::middle__Relation_1_')"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testFunctionsSharingAServiceAreReadTogether(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                serviceReadingFunction("pack::left", "select b from service('/relation')") +
                serviceReadingFunction("pack::right", "select b from service('/relation')") +
                reader("select b from func('pack::left__Relation_1_') union all select b from func('pack::right__Relation_1_')"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testServiceWhoseQueryCallsAFunctionReadingAnotherService(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                serviceReadingFunction("pack::middle", "select b from service('/relation')") +
                "###Service\n" +
                service("pack::ViaFunctionService", "/viaFunction", "|pack::middle()") +
                reader("select b from service('/viaFunction')"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testManyReadersOfOneServiceCompiledInParallelAllSeeItsColumns()
    {
        StringBuilder readers = new StringBuilder("###Pure\n");
        for (int i = 0; i < 16; i++)
        {
            readers.append("function pack::reader").append(i).append("():meta::pure::metamodel::relation::Relation<Any>[1]\n{\n  #SQL{select b from service('/relation')}#\n}\n");
        }
        for (int round = 0; round < 5; round++)
        {
            PureModel pureModel = compile(MODEL + readers, true);
            for (int i = 0; i < 16; i++)
            {
                Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), columns(pureModel, "pack::reader" + i + "__Relation_1_"), "round " + round + ", reader " + i);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 5, 8, 13, 21, 34})
    public void testServiceSourceDoesNotDependOnTheOrderOfTheModelElements(long seed)
    {
        PureModelContextData parsed = PureGrammarParser.newInstance().parseModel(MODEL +
                serviceReadingFunction("pack::middle", "select b from service('/relation')") +
                reader("select b from func('pack::middle__Relation_1_')"));
        List<PackageableElement> elements = new ArrayList<>(parsed.getElements());
        Collections.shuffle(elements, new Random(seed));

        PureModel pureModel = new PureModel(PureModelContextData.newBuilder().withElements(elements).build(), null, DeploymentMode.TEST);

        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testPartialCompilationReportsTheUnknownServiceAndCompilesTheOtherReaders()
    {
        PureModelContextData parsed = PureGrammarParser.newInstance().parseModel(MODEL +
                reader("select b from service('/relation')") +
                "function pack::brokenReader():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                "{\n" +
                "  #SQL{select * from service('/missing')}#\n" +
                "}\n");

        PureModel pureModel = new PureModel(parsed, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.TEST, PureModelProcessParameter.newBuilder().withEnablePartialCompilation(true).build(), null);

        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
        Assertions.assertEquals(1, pureModel.getEngineExceptions().size(), pureModel.getEngineExceptions().toString());
        Assertions.assertTrue(pureModel.getEngineExceptions().get(0).getMessage().contains("Can't find a service with pattern '/missing'"), pureModel.getEngineExceptions().get(0).getMessage());
    }

    private static final String DECLARED_STRING_FUNCTION =
            "###Pure\n" +
            "function pack::declared():meta::pure::metamodel::relation::Relation<(c:String[0..1])>[1]\n" +
            "{\n" +
            "  #>{pack::DB.T}#->select(~[c])\n" +
            "}\n";

    @Test
    public void testFuncSourceIsTypedByTheSignatureOfItsTarget()
    {
        PureModel pureModel = compile(MODEL + DECLARED_STRING_FUNCTION + reader("select c from func('pack::declared__Relation_1_')"), false);
        Assertions.assertEquals(Lists.mutable.with("c:String[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceWhoseQueryCallsAFunctionIsTypedByThatFunctionsSignature()
    {
        PureModel pureModel = compile(MODEL + DECLARED_STRING_FUNCTION +
                "###Service\n" +
                service("pack::DeclaredService", "/declared", "|pack::declared()") +
                reader("select c from service('/declared')"), false);
        Assertions.assertEquals(Lists.mutable.with("c:String[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceWhoseQueryReadsATableIsTypedByTheTable()
    {
        PureModel pureModel = compile(MODEL +
                "###Service\n" +
                service("pack::TableService", "/table", "|#>{pack::DB.T}#->select(~[c])") +
                reader("select c from service('/table')"), false);
        Assertions.assertEquals(Lists.mutable.with("c:Varchar[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testPartialCompilationReportsAReaderOfABrokenServiceAgainstThatService()
    {
        PureModelContextData parsed = PureGrammarParser.newInstance().parseModel(MODEL +
                "###Service\n" +
                service("pack::BrokenService", "/broken", "|pack::doesNotExist()") +
                reader("select * from service('/broken')"));

        PureModel pureModel = new PureModel(parsed, null, Thread.currentThread().getContextClassLoader(), DeploymentMode.TEST, PureModelProcessParameter.newBuilder().withEnablePartialCompilation(true).build(), null);

        Assertions.assertTrue(pureModel.getEngineExceptions().anySatisfy(e -> e.getMessage().contains("service '/broken' (pack::BrokenService) has no compiled execution; fix the errors reported for that service")), pureModel.getEngineExceptions().toString());
    }

    private static final String FUNC_TARGETS =
            "###Pure\n" +
            "function pack::open():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
            "{\n" +
            "  #>{pack::DB.T}#->select(~[b])\n" +
            "}\n" +
            "function pack::typed():meta::pure::metamodel::relation::Relation<(b:Int[0..1])>[1]\n" +
            "{\n" +
            "  #>{pack::DB.T}#->select(~[b])\n" +
            "}\n";

    @Test
    public void testFuncSourcesDeclareNoPrerequisites()
    {
        CompileContext context = compile(MODEL + FUNC_TARGETS, false).getContext();
        FunctionTableFunctionCompilerExtension extension = new FunctionTableFunctionCompilerExtension();

        Assertions.assertEquals(Collections.emptySet(), extension.extractPrerequisiteElements(tableFunction("select * from func('pack::open__Relation_1_')"), context));
        Assertions.assertEquals(Collections.emptySet(), extension.extractPrerequisiteElements(tableFunction("select * from func('pack::typed__Relation_1_')"), context));
    }

    @Test
    public void testSubclassesOfTheSimpleExtractorDeclareNoPrerequisitesUnlessTheyOptIn()
    {
        TableFunctionSimpleElementExtractor<ConcreteFunctionDefinition> extractor = new TableFunctionSimpleElementExtractor<ConcreteFunctionDefinition>(ConcreteFunctionDefinition.class, "func")
        {
        };

        Assertions.assertEquals(Collections.emptySet(), extractor.extractPrerequisiteElements(tableFunction("select * from func('pack::open__Relation_1_')"), compile(MODEL + FUNC_TARGETS, false).getContext()));
    }

    @Test
    public void testStorePrerequisitesIgnoreAnElementPathOverrideThatThrows()
    {
        StoreTableFunctionCompilerExtension extension = new StoreTableFunctionCompilerExtension()
        {
            @Override
            protected String extractElementPath(TableFunction tableFunction)
            {
                throw new IllegalStateException("unsupported arguments");
            }
        };

        Assertions.assertEquals(Collections.emptySet(), extension.extractPrerequisiteElements(tableFunction("select * from tb('pack::DB.T')"), compile(MODEL, false).getContext()));
    }

    @Test
    public void testStorePrerequisiteUsesAnElementPathOverrideForANamedArgument()
    {
        StoreTableFunctionCompilerExtension extension = new StoreTableFunctionCompilerExtension()
        {
            @Override
            protected String extractElementPath(TableFunction tableFunction)
            {
                return ((StringLiteral) ((NamedArgumentExpression) tableFunction.functionCall.arguments.get(0)).expression).value;
            }
        };

        Assertions.assertEquals(Sets.mutable.with("pack::DB"), extension.extractPrerequisiteElements(tableFunction("select * from tb(path => 'pack::DB.T')"), compile(MODEL, false).getContext()).stream().map(p -> p.path).collect(Collectors.toSet()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testFuncSourceWithATypedTargetDependingBackOnTheReaderIsNotACycle(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                "###Pure\n" +
                "Class pack::Row\n" +
                "{\n" +
                "  b: Integer[0..1];\n" +
                "}\n" +
                "function pack::src():meta::pure::metamodel::relation::Relation<(b:Int[0..1])>[1]\n" +
                "{\n" +
                "  #>{pack::DB.T}#->select(~[b])->from(pack::Rt2)\n" +
                "}\n" +
                reader("select * from func('pack::src__Relation_1_')") +
                "###Mapping\n" +
                "Mapping pack::M2\n" +
                "(\n" +
                "  *pack::Row: Relation\n" +
                "  {\n" +
                "    ~func pack::reader():Relation<Any>[1]\n" +
                "    b: b\n" +
                "  }\n" +
                ")\n" +
                "###Runtime\n" +
                "Runtime pack::Rt2\n" +
                "{\n" +
                "  mappings: [pack::M2];\n" +
                "  connections:\n" +
                "  [\n" +
                "    pack::DB: [ c2: pack::Conn ]\n" +
                "  ];\n" +
                "}\n", parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testServiceSourceInARelationMappingSrcIsCompiledAfterTheService(boolean parallel)
    {
        Assertions.assertDoesNotThrow(() -> compile(MODEL +
                "###Pure\n" +
                "Class pack::C\n" +
                "{\n" +
                "  a: Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping pack::M2\n" +
                "(\n" +
                "  *pack::C: Relation\n" +
                "  {\n" +
                "    ~src #SQL{select a from service('/relation')}#\n" +
                "    a: a\n" +
                "  }\n" +
                ")\n", parallel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testServiceSourceInASubqueryArgumentOfAnotherSource(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                "###Pure\n" +
                "function pack::g2(r:meta::pure::metamodel::relation::Relation<(b:Integer[0..1])>[1]):meta::pure::metamodel::relation::Relation<(b:Integer[0..1])>[1]\n" +
                "{\n" +
                "  #SQL{select * from var('r')}#\n" +
                "}\n" +
                reader("select b from func('pack::g2_Relation_1__Relation_1_', r => (select b from service('/relation')))"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Integer[0..1]"), readerColumns(pureModel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testServiceSourceInAnOrderBySubquery(boolean parallel)
    {
        PureModel pureModel = compile(MODEL +
                "###Service\n" +
                service("pack::OtherService", "/other", "|#>{pack::DB.T}#->select(~[b])") +
                reader("select b from service('/relation') order by case when b in (select b from service('/other')) then 0 else 1 end"), parallel);
        Assertions.assertEquals(Lists.mutable.with("b:Int[0..1]"), readerColumns(pureModel));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRelationMappingSrcReferringToItsOwnRuntimeIsNotACycle(boolean parallel)
    {
        Assertions.assertDoesNotThrow(() -> compile(MODEL +
                "###Pure\n" +
                "Class pack::C\n" +
                "{\n" +
                "  a: Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping pack::M2\n" +
                "(\n" +
                "  *pack::C: Relation\n" +
                "  {\n" +
                "    ~src #>{pack::DB.T}#->select(~[a])->from(pack::Rt2)\n" +
                "    a: a\n" +
                "  }\n" +
                ")\n" +
                "###Runtime\n" +
                "Runtime pack::Rt2\n" +
                "{\n" +
                "  mappings: [pack::M, pack::M2];\n" +
                "  connections:\n" +
                "  [\n" +
                "    pack::DB: [ c2: pack::Conn ]\n" +
                "  ];\n" +
                "}\n", parallel));
    }

    @Test
    public void testServiceSourceInTheSrcOfAMappingInTheServicesOwnMappingIsACycle()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(MODEL.replace(
                "    age: [pack::DB]T.a\n" +
                "  }\n" +
                ")\n",
                "    age: [pack::DB]T.a\n" +
                "  }\n" +
                "  *pack::C: Relation\n" +
                "  {\n" +
                "    ~src #SQL{select a from service('/relation')}#\n" +
                "    a: a\n" +
                "  }\n" +
                ")\n") +
                "###Pure\n" +
                "Class pack::C\n" +
                "{\n" +
                "  a: Integer[0..1];\n" +
                "}\n", false));
        Assertions.assertTrue(e.getMessage().contains("Detected a circular dependency in element prerequisites graph!"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "select case (select max(a) from service('/other')) when 1 then 'x' else 'y' end as r from service('/relation')",
            "select a, array[(select max(a) from service('/other'))] as arr from service('/relation')"
    })
    public void testServiceSourceInACaseOperandOrArrayLiteral(String sql)
    {
        Assertions.assertDoesNotThrow(() -> compile(MODEL +
                "###Service\n" +
                service("pack::OtherService", "/other", "|#>{pack::DB.T}#->select(~[a])") +
                reader(sql), false));
    }

    @Test
    public void testMultiExecutionKeyIsAlsoBoundToAServiceParameterOfThatName()
    {
        PureModel pureModel = compile(MODEL +
                "###Service\n" +
                "Service pack::KeyedService\n" +
                "{\n" +
                "  pattern: '/keyed';\n" +
                "  owners: ['owner'];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: env: String[1]|#>{pack::DB.T}#->filter(r|$r.c == $env)->select(~[c]);\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: pack::M;\n" +
                "      runtime: pack::Rt;\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                reader("select c from service('/keyed', env => 'PROD')"), false);
        Assertions.assertEquals(Lists.mutable.with("c:Varchar[0..1]"), readerColumns(pureModel));
    }

    @Test
    public void testServiceArgumentIsTypedByTheDeclaredParameter()
    {
        PureModel pureModel = compile(MODEL +
                "###Service\n" +
                service("pack::ScaledService", "/scaled", "factor: Float[1]|#>{pack::DB.T}#->extend(~f:x|$factor)->select(~[f])") +
                reader("select f from service('/scaled', factor => 2)"), false);
        Assertions.assertEquals(Lists.mutable.with("f:Float[1]"), readerColumns(pureModel));
    }

    private static TableFunction tableFunction(String sql)
    {
        Query query = (Query) SQLGrammarParser.newInstance().parseStatement(sql);
        return (TableFunction) ((QuerySpecification) query.queryBody).from.get(0);
    }

    private static String service(String path, String pattern, String query)
    {
        return "Service " + path + "\n" +
                "{\n" +
                "  pattern: '" + pattern + "';\n" +
                "  owners: ['owner'];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: " + query + ";\n" +
                "    mapping: pack::M;\n" +
                "    runtime: pack::Rt;\n" +
                "  }\n" +
                "}\n";
    }

    private static String reader(String sql)
    {
        return "###Pure\n" +
                "function pack::reader():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                "{\n" +
                "  #SQL{" + sql + "}#\n" +
                "}\n";
    }

    private static String serviceReadingFunction(String path, String sql)
    {
        return "###Pure\n" +
                "function " + path + "():meta::pure::metamodel::relation::Relation<(b:Int[0..1])>[1]\n" +
                "{\n" +
                "  #SQL{" + sql + "}#\n" +
                "}\n";
    }

    private static ListIterable<String> readerColumns(PureModel pureModel)
    {
        return columns(pureModel, "pack::reader__Relation_1_");
    }

    private static ListIterable<String> columns(PureModel pureModel, String function)
    {
        ConcreteFunctionDefinition<?> reader = (ConcreteFunctionDefinition<?>) pureModel.getPackageableElement(function);
        RelationType<?> relationType = (RelationType<?>) reader._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType();
        return relationType._columns().collect(TestSQLExpressionServiceSource::describe).toList();
    }

    private static String describe(Column<?, ?> column)
    {
        return column._name() + ":" + column._classifierGenericType()._typeArguments().getLast()._rawType()._name() + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(column._classifierGenericType()._multiplicityArguments().getFirst());
    }

    private static PureModel compile(String code, boolean parallel)
    {
        if (!parallel)
        {
            return new PureModel(PureGrammarParser.newInstance().parseModel(code), null, DeploymentMode.TEST);
        }
        ForkJoinPool pool = new ForkJoinPool(4);
        try
        {
            return new PureModel(PureGrammarParser.newInstance().parseModel(code), null, Thread.currentThread().getContextClassLoader(), DeploymentMode.TEST, PureModelProcessParameter.newBuilder().withForkJoinPool(pool).build(), null);
        }
        finally
        {
            pool.shutdown();
        }
    }
}
