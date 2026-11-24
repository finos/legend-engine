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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class TestSQLExpressionCompileOrdering
{
    // Within a dependency level the third pass runs in hash order of the element paths (or concurrently), so each pair
    // is chosen so that both orders occur across the set; func(...) adds no ordering edge, so every order must work.
    private static final ImmutableList<String[]> NAME_PAIRS = Lists.immutable.with(
            new String[]{"a", "b"}, new String[]{"b", "a"}, new String[]{"src", "user"}, new String[]{"user", "src"},
            new String[]{"x1", "x2"}, new String[]{"x2", "x1"}, new String[]{"zz", "aa"}, new String[]{"aa", "zz"},
            new String[]{"m", "n"}, new String[]{"n", "m"}, new String[]{"p", "q"}, new String[]{"q", "p"});

    private static final String DATABASE =
            "###Relational\n" +
            "Database pack::DB(Table T(a INT, b INT))\n" +
            "###Pure\n";

    private static Stream<Arguments> namePairsSequentialAndParallel()
    {
        return NAME_PAIRS.asLazy().flatCollect(pair -> Lists.mutable.with(Arguments.of(pair[0], pair[1], false), Arguments.of(pair[0], pair[1], true))).toList().stream();
    }

    @ParameterizedTest
    @MethodSource("namePairsSequentialAndParallel")
    public void testFuncSourceAtTheSameLevelAsItsTargetIsTypedByTheTargetsSignature(String source, String user, boolean parallel)
    {
        PureModel pureModel = compile(sourceFunction(source, "Relation<(a:Integer, b:Integer)>", "#SQL{select a, b from csv('a,b\n1,2\n3,4')}#") + userFunction(user, source), parallel);
        Assertions.assertEquals(Lists.mutable.with("b"), relationColumns(pureModel, user));
    }

    @ParameterizedTest
    @MethodSource("namePairsSequentialAndParallel")
    public void testFuncSourceOfADeeperTargetIsTypedByTheTargetsSignature(String source, String user, boolean parallel)
    {
        PureModel pureModel = compile(DATABASE + sourceFunction(source, "Relation<(a:Int[0..1], b:Int[0..1])>", "#>{pack::DB.T}#->select(~[a, b])") + userFunction(user, source), parallel);
        Assertions.assertEquals(Lists.mutable.with("b"), relationColumns(pureModel, user));
    }

    private static String sourceFunction(String name, String returnType, String body)
    {
        return "function pack::" + name + "():" + returnType + "[1]\n" +
                "{\n" +
                "  " + body + "\n" +
                "}\n\n";
    }

    private static String userFunction(String name, String source)
    {
        return "function pack::" + name + "():Relation<Any>[1]\n" +
                "{\n" +
                "  #SQL{select b from func('pack::" + source + "__Relation_1_')}#\n" +
                "}\n";
    }

    private static ListIterable<String> relationColumns(PureModel pureModel, String function)
    {
        ConcreteFunctionDefinition<?> definition = (ConcreteFunctionDefinition<?>) pureModel.getPackageableElement("pack::" + function + "__Relation_1_");
        RelationType<?> relationType = (RelationType<?>) definition._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType();
        return relationType._columns().collect(c -> c._name()).toList();
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
