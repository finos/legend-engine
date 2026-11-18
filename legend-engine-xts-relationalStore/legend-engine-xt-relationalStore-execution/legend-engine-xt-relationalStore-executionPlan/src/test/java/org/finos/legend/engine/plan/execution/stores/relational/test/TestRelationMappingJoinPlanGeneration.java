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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;

public class TestRelationMappingJoinPlanGeneration
{
    private static final String JOIN_CONDITION = "{p, f | $p.FIRMID == $f.ID}";
    private static final String STEP_AHEAD_OF_JOIN_UNSUPPORTED = " cannot be applied to $src ahead of navigationJoin/filterJoin: only extend, filter, select, rename and sort are supported, as they keep every source row exactly once.";

    private static final String FIXTURE = "###Pure\n" +
            "Class my::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  value: %s;\n" +
            "}\n" +
            "Class my::Address\n" +
            "{\n" +
            "  city: String[1];\n" +
            "}\n" +
            "function my::personRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], FIRSTNAME:String[1], FIRMID:Integer[1], ADDRESSID:Integer[1])>[1]\n" +
            "{\n" +
            "  #>{my::DB.PERSON}#\n" +
            "}\n" +
            "function my::firmRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>[1]\n" +
            "{\n" +
            "  #>{my::DB.FIRM}#\n" +
            "}\n" +
            "function my::addressRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], CITY:String[1])>[1]\n" +
            "{\n" +
            "  #>{my::DB.ADDRESS}#\n" +
            "}\n" +
            "function my::query(): Any[*]\n" +
            "{\n" +
            "  my::Person.all()->project([x | $x.value], ['value'])\n" +
            "}\n" +
            "###Relational\n" +
            "Database my::DB\n" +
            "(\n" +
            "  Table PERSON (ID INTEGER PRIMARY KEY, FIRSTNAME VARCHAR(100) NOT NULL, FIRMID INTEGER NOT NULL, ADDRESSID INTEGER NOT NULL)\n" +
            "  Table FIRM (ID INTEGER PRIMARY KEY, LEGALNAME VARCHAR(100) NOT NULL, ADDRESSID INTEGER NOT NULL)\n" +
            "  Table ADDRESS (ID INTEGER PRIMARY KEY, CITY VARCHAR(100) NOT NULL)\n" +
            ")\n" +
            "###Mapping\n" +
            "Mapping my::testMapping\n" +
            "(\n" +
            "  *my::Person[person]: Relation\n" +
            "  {\n" +
            "    ~func my::personRel():Relation<Any>[1]\n" +
            "    ~primaryKey: [ID]\n" +
            "    firstName: FIRSTNAME,\n" +
            "    value: %s\n" +
            "  }\n" +
            ")\n" +
            "###Runtime\n" +
            "Runtime my::testRuntime\n" +
            "{\n" +
            "  mappings: [my::testMapping];\n" +
            "  connections:\n" +
            "  [\n" +
            "    my::DB:\n" +
            "    [\n" +
            "      c1: #{ RelationalDatabaseConnection { type: H2; specification: LocalH2 {}; auth: DefaultH2; } }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    private static void plans(String valueType, String value)
    {
        generatePlan(valueType, value);
    }

    private static void rejected(String valueType, String value, String expectedMessage)
    {
        try
        {
            generatePlan(valueType, value);
        }
        catch (Exception e)
        {
            String messages = allMessages(e);
            Assert.assertTrue("Expected message containing: " + expectedMessage + "\nGot: " + messages, messages.contains(expectedMessage));
            return;
        }
        Assert.fail("Expected plan generation to fail with: " + expectedMessage);
    }

    private static String allMessages(Exception e)
    {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = e; current != null; current = current.getCause())
        {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private static void generatePlan(String valueType, String value)
    {
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(String.format(FIXTURE, valueType, value));
        PureModel pureModel = Compiler.compile(data, null, Identity.getAnonymousIdentity().getName());
        Function query = data.getElementsOfType(Function.class).stream().filter(f -> f.name.startsWith("query__")).findFirst().get();
        Assert.assertNotNull(PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(query.body, Collections.emptyList(), new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("my::testMapping"),
                pureModel.getRuntime("my::testRuntime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers));
    }

    @Test
    public void joinAfterEverySupportedStepPlans()
    {
        plans("String[0..1]", "$src->extend(~K: x | $x.FIRMID + 1)->filter(x | $x.K > 0)->select(~[ID, K])->rename(~K, ~FIRMKEY)->sort(~FIRMKEY->ascending())->navigationJoin(my::firmRel(), {p, f | $p.FIRMKEY == $f.ID}).LEGALNAME");
    }

    @Test
    public void stepLambdaBindingItsOwnRowAndSrcNamesPlans()
    {
        plans("String[0..1]", "$src->filter(row | $row.FIRMID > 0)->navigationJoin(my::firmRel(), " + JOIN_CONDITION + ").LEGALNAME");
        plans("String[0..1]", "$src->filter(src | $src.FIRMID > 0)->navigationJoin(my::firmRel(), " + JOIN_CONDITION + ").LEGALNAME");
    }

    @Test
    public void srcBoundByALambdaOfAJoinTargetPlans()
    {
        plans("String[0..1]", "$src->navigationJoin(my::firmRel()->filter(src | $src.ID > 0), " + JOIN_CONDITION + ").LEGALNAME");
    }

    @Test
    public void filterJoinAndChainedJoinsPlan()
    {
        plans("String[0..1]", "$src->filterJoin(my::firmRel(), " + JOIN_CONDITION + ").FIRSTNAME");
        plans("String[0..1]", "$src->navigationJoin(my::firmRel(), " + JOIN_CONDITION + ")->navigationJoin(my::addressRel(), {f, a | $f.ADDRESSID == $a.ID}).CITY");
    }

    @Test
    public void unsupportedStepAheadOfAJoinRejected()
    {
        rejected("String[0..1]", "$src->limit(1)->filterJoin(my::firmRel(), " + JOIN_CONDITION + ").FIRSTNAME", "'limit'" + STEP_AHEAD_OF_JOIN_UNSUPPORTED);
        rejected("String[0..1]", "$src->filter(r | $r.FIRMID > 0)->limit(1)->navigationJoin(my::firmRel(), " + JOIN_CONDITION + ").LEGALNAME", "'limit'" + STEP_AHEAD_OF_JOIN_UNSUPPORTED);
        rejected("String[0..1]", "$src->join(my::addressRel()->rename(~ID, ~AID), meta::pure::functions::relation::JoinKind.INNER, {p, a | $p.ADDRESSID == $a.AID})->navigationJoin(my::firmRel(), " + JOIN_CONDITION + ").LEGALNAME",
                "'join'" + STEP_AHEAD_OF_JOIN_UNSUPPORTED);
    }
}
