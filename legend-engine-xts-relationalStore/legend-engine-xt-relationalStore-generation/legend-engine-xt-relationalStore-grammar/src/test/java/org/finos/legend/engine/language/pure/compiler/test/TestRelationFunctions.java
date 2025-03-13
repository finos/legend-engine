// Copyright 2023 Goldman Sachs
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

import net.javacrumbs.jsonunit.JsonAssert;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Test;

public class TestRelationFunctions extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void testFilter()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->filter(i|$i.id == 'ok')\n" +
                        "}"
        );
    }

    @Test
    public void lambdaRelationReturnType() throws Exception
    {
        Pair<PureModelContextData, PureModel> pureModelPair = test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n"
        );

        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|#>{a::A.tb}#->select()");
        GenericType genericType = Compiler.getLambdaReturnGenericType(lambda, pureModelPair.getTwo());
        String actualValue = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(genericType);
        JsonAssert.assertJsonEquals(
                "{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"meta::pure::store::RelationStoreAccessor\"},\"typeArguments\":[{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Integer\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"id\"}]},\"typeArguments\":[],\"typeVariableValues\":[]}],\"typeVariableValues\":[]}",
                actualValue);
    }

    @Test
    public void testFilterError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->filter(i|$i.ide == 'ok')\n" +
                        "}", "COMPILATION error at [7:30-32]: The column 'ide' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testConcatenate()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->concatenate(#>{a::A.tb}#)\n" +
                        "}"
        );
    }

    @Test
    public void testConcatenateError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "Database a::B (Table tb(otherCol Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->concatenate(#>{a::B.tb}#)\n" +
                        "}", "COMPILATION error at [8:18-28]: The two relations are incompatible and can't be concatenated (id:Integer) and (otherCol:Integer)"
        );
    }

    @Test
    public void testRename()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id,~id2)\n" +
                        "}"
        );
    }

    @Test
    public void testRenameError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~idw,~id2)\n" +
                        "}", "COMPILATION error at [7:26-28]: The column 'idw' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testRenameCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id, ~newId)->filter(i|$i.newId == 'ok')\n" +
                        "}"
        );
    }

    @Test
    public void testRenameComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->rename(~id, ~newId)->filter(i|$i.id == 'ok')\n" +
                        "}",
                "COMPILATION error at [7:51-52]: The column 'id' can't be found in the relation (newId:Integer)"
        );
    }

    @Test
    public void testExtend()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)\n" +
                        "}"
        );
    }

    @Test
    public void testExtendErrorDupColumns()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~id:x|$x.id->toOne() + 1)\n" +
                        "}",
                "COMPILATION error at [7:18-23]: The relation contains duplicates: [id]"
        );
    }

    @Test
    public void testExtendErrorWrongColumn()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~id:x|$x.ide->toOne() + 1)\n" +
                        "}",
                "COMPILATION error at [7:34-36]: The column 'ide' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testExtendCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)->filter(i|$i.nid < 3)\n" +
                        "}"
        );
    }

    @Test
    public void testExtendComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne() + 1)->filter(i|$i.neid == 'ok')\n" +
                        "}",
                "COMPILATION error at [7:65-68]: The column 'neid' can't be found in the relation (id:Integer, nid:Integer)"
        );
    }

    @Test
    public void testExtendAggregation()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne()+1:y|$y->sum())\n" +
                        "}"
        );
    }

    @Test
    public void testExtendAggregationArray()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~[nid:x|$x.id->toOne()+1:y|$y->sum()])\n" +
                        "}"
        );
    }

    @Test
    public void testExtendAggregationError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(~nid:x|$x.id->toOne()+1:y|$y->joinStrings(','))\n" +
                        "}",
                "COMPILATION error at [7:55-65]: Can't find a match for function 'joinStrings(Integer[*],String[1])'"
        );
    }

    @Test
    public void testExtendAggregationWithWindowPartition()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id), ~nid:{p,w,r|$r.id}:y|$y->sum())\n" +
                        "}"
        );
    }

    @Test
    public void testExtendAggregationWithWindowPartitionError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~ide), ~nid:{p,w,r|$r.id}:y|$y->sum())\n" +
                        "}", "COMPILATION error at [7:31-33]: The column 'ide' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testExtendAggregationWithWindowSort()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->ascending()), ~nid:{p,w,r|$r.id}:y|$y->sum())\n" +
                        "}"
        );
    }

    @Test
    public void testExtendAggregationWithWindowSortError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~ids->ascending()), ~nid:{p,w,r|$r.id}:y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:31-33]: The column 'ids' can't be found in the relation (id:Integer)"
        );
    }


    @Test
    public void testExtendAggregationWithWindowLambdaError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id), ~nid:{p,w,r|$r.ide}:y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:51-53]: The column 'ide' can't be found in the relation (id:Integer)");
    }


    @Test
    public void testExtendWithWindowCumulativeDistribution()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->cumulativeDistribution($w,$r)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowNtile()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->ntile($r,2)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowRank()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->rank($w,$r)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowDenseRank()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->denseRank($w,$r)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowPercentRank()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->percentRank($w,$r)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowRowNumber()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->rowNumber($r)})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowLag()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->lag($r).id})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowLead()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->lead($r).id})\n" +
                        "}");
    }

    @Test
    public void testExtendWithWindowNth()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->nth($w,$r,1).id})\n" +
                        "}");
    }

    @Test
    public void testExtendWithFirst()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->first($w,$r).id})\n" +
                        "}");
    }

    @Test
    public void testExtendWithLast()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->extend(over(~id->descending()), ~nid:{p,w,r|$p->last($w,$r).id})\n" +
                        "}");
    }

    @Test
    public void testSort()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(ascending(~id))\n" +
                        "}"
        );
    }

    @Test
    public void testSortErrorWrongColumn()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(ascending(~id2))\n" +
                        "}",
                "COMPILATION error at [7:34-36]: The column 'id2' can't be found in the relation (id:Integer)"
        );
    }

    @Test
    public void testSortErrorWrongParameter()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(~id2)\n" +
                        "}",
                "COMPILATION error at [7:18-21]: Can't find a match for function 'sort(RelationStoreAccessor<(id:Integer)>[1],ColSpec<(id2:NULL)>[1])"
        );
    }

    @Test
    public void testSortErrorWrongParameterUnknownFunc()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort(~id->desc())\n" +
                        "}",
                "COMPILATION error at [7:28-31]: Can't find a match for function 'desc(ColSpec<(id:Integer)>[1])'"
        );
    }

    @Test
    public void testSortArray()
    {
        test(
                "###Relational\n" +
                        "Database a::A (Table tb(id Integer, other VARCHAR(200)))\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->sort([ascending(~id), descending(~other)])\n" +
                        "}"
        );
    }


    @Test
    public void testJoin()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.id == $b.id2})\n" +
                        "}"
        );
    }

    @Test
    public void testJoinErrorWrongCols()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.xid == $b.id2})\n" +
                        "}",
                "COMPILATION error at [7:95-97]: The column 'xid' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testJoinErrorWrongCols2()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        "   Table tb2(id2 Integer, errr VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->join(#>{a::A.tb2}#, meta::pure::functions::relation::JoinKind.INNER, {a,b|$a.id == $b.eid2})\n" +
                        "}",
                "COMPILATION error at [7:104-107]: The column 'eid2' can't be found in the relation (id2:Integer, errr:String)"
        );
    }

    @Test
    public void testGroupBy()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())\n" +
                        "}"
        );
    }

    @Test
    public void testGroupByErrorCol()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[oteher], ~new : x|$x.id : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:28-33]: The column 'oteher' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testGroupByErrorAggMap()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.ied : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:48-50]: The column 'ied' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testGroupByErrorAggReduce()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.other : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:62-64]: Can't find a match for function 'sum(String[*])"
        );
    }

    @Test
    public void testGroupByCompose()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())->filter(x|$x.new > 1)\n" +
                        "}"
        );
    }

    @Test
    public void testGroupByComposeError()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->groupBy(~[other], ~new : x|$x.id : y|$y->sum())->filter(x|$x.newE > 1)\n" +
                        "}",
                "COMPILATION error at [7:79-82]: The column 'newE' can't be found in the relation (other:String, new:Integer)"
        );
    }

    @Test
    public void testPivot()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())\n" +
                        "}"
        );
    }

    // TODO: @akphi casting syntax should be supported in engine as it works in Pure
//    @Test
//    public void testPivotCompose()
//    {
//        test(
//                "###Relational\n" +
//                        "Database a::A (" +
//                        "   Table tb(id Integer, other VARCHAR(200))" +
//                        ")\n" +
//                        "\n" +
//                        "###Pure\n" +
//                        "function test::f():Any[*]\n" +
//                        "{\n" +
//                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())->cast(@meta::pure::metamodel::relation::Relation<(new:Integer)>)\n" +
//                        "}"
//        );
//    }

    @Test
    public void testErrorPivotComposeWithoutCasting()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.id : y|$y->sum())->select(~many)\n" +
                        "}",
                "COMPILATION error at [7:73-76]: The column 'many' can't be found in the relation ()"
        );
    }

    @Test
    public void testPivotErrorCol()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[oteher], ~new : x|$x.id : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:26-31]: The column 'oteher' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testPivotErrorAggMap()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.ied : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:46-48]: The column 'ied' can't be found in the relation (id:Integer, other:String)"
        );
    }

    @Test
    public void testPivotErrorAggReduce()
    {
        test(
                "###Relational\n" +
                        "Database a::A (" +
                        "   Table tb(id Integer, other VARCHAR(200))" +
                        ")\n" +
                        "\n" +
                        "###Pure\n" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   #>{a::A.tb}#->pivot(~[other], ~new : x|$x.other : y|$y->sum())\n" +
                        "}",
                "COMPILATION error at [7:60-62]: Can't find a match for function 'sum(String[*])"
        );
    }

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Relational\n" +
                "Database anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }
}
