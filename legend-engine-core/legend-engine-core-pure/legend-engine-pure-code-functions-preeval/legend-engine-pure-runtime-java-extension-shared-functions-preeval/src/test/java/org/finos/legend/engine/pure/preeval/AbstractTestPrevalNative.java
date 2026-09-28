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

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestPrevalNative extends AbstractPureTestWithCoreCompiled
{
    private static final String HOOKS = "^PrevalHooks("
            + "stopPreeval = {a:Any[*] | !$a->exists(x | $x->instanceOf(meta::pure::metamodel::valuespecification::FunctionExpression))}, "
            + "shouldInline = {f:Function<Any>[1] | false}, "
            + "isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, "
            + "isGetAllFunction = {f:Function<Any>[1] | false}, "
            + "resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []})";
    private static final String INLINING_HOOKS = "^PrevalHooks("
            + "stopPreeval = {a:Any[*] | !$a->exists(x | $x->instanceOf(meta::pure::metamodel::valuespecification::FunctionExpression))}, "
            + "shouldInline = {f:Function<Any>[1] | $f->instanceOf(ConcreteFunctionDefinition)}, "
            + "isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, "
            + "isGetAllFunction = {f:Function<Any>[1] | false}, "
            + "resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []})";
    private static final String AGG_COL_SPEC = "^meta::pure::metamodel::relation::AggColSpec<{Integer[1]->Integer[1]}, {Integer[*]->Integer[1]}, Any>";

    @After
    public void deleteTestSource()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrevalNativeFoldsConstantExpression()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let f = {|1 + 1};",
                "let r = prevalNative($f, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');",
                "let body = $r.value->cast(@LambdaFunction<Any>).expressionSequence->at(0);",
                "assert($body->instanceOf(InstanceValue), |'expected an instance value');",
                "assert($body->cast(@InstanceValue).values->toOne() == 2, |'expected 2');",
                "assert($body.multiplicity == PureOne, |'expected multiplicity PureOne');",
                "assert($f->evaluateAndDeactivate().expressionSequence->at(0)->instanceOf(FunctionExpression), |'the input lambda must not be mutated');");
    }

    @Test
    public void testPrevalNativeSubstitutesAndFoldsInScopeVariables()
    {
        executeTestFunction(
                "let vars = newMap(pair('x', list(3)));",
                "let x = 0;",
                "let fe = {|$x + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $vars, $vars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->cast(@InstanceValue).values->toOne() == 4, |'expected 4');");
    }

    @Test
    public void testPrevalNativeReactivatesToSeveralValues()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|'a,b,c'->split(',')}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.canPreval, |'expected canPreval');",
                "let iv = $r.value->cast(@InstanceValue);",
                "assert($iv.values->size() == 3, |'expected 3 values');",
                "assert(($iv.values->at(0) == 'a') && ($iv.values->at(1) == 'b') && ($iv.values->at(2) == 'c'), |'expected a, b, c');",
                "assert(($iv.multiplicity.lowerBound.value == 3) && ($iv.multiplicity.upperBound.value == 3), |'expected multiplicity [3]');");
    }

    @Test
    public void testPrevalNativeFoldsAggColSpecFunctions()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let spec = " + AGG_COL_SPEC + "(name = 'a', map = {x:Integer[1] | 1 + 1}, reduce = {y:Integer[*] | $y->plus()});",
                "let r = prevalNative($spec, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');",
                "let newSpec = $r.value->cast(@meta::pure::metamodel::relation::AggColSpec<Any, Any, Any>);",
                "assert($newSpec.name == 'a', |'expected name a');",
                "assert($newSpec.map->cast(@LambdaFunction<Any>).expressionSequence->evaluateAndDeactivate()->at(0)->cast(@InstanceValue).values->toOne() == 2, |'expected map folded to 2');",
                "assert($newSpec.reduce == $spec.reduce, |'expected reduce unchanged');",
                "assert($spec.map->cast(@LambdaFunction<Any>).expressionSequence->evaluateAndDeactivate()->at(0)->instanceOf(FunctionExpression), |'the input spec must not be mutated');");
    }

    @Test
    public void testPrevalNativeFoldsAggColSpecArrayElements()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let folding = " + AGG_COL_SPEC + "(name = 'a', map = {x:Integer[1] | 1 + 1}, reduce = {y:Integer[*] | $y->plus()});",
                "let unchanged = " + AGG_COL_SPEC + "(name = 'b', map = {x:Integer[1] | $x}, reduce = {y:Integer[*] | $y->plus()});",
                "let array = ^meta::pure::metamodel::relation::AggColSpecArray<{Integer[1]->Integer[1]}, {Integer[*]->Integer[1]}, Any>(aggSpecs = [$folding, $unchanged]);",
                "let r = prevalNative($array, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.canPreval, |'expected canPreval');",
                "let specs = $r.value->cast(@meta::pure::metamodel::relation::AggColSpecArray<Any, Any, Any>).aggSpecs;",
                "assert($specs->size() == 2, |'expected 2 specs');",
                "assert($specs->at(0).map->cast(@LambdaFunction<Any>).expressionSequence->evaluateAndDeactivate()->at(0)->cast(@InstanceValue).values->toOne() == 2, |'expected first map folded to 2');",
                "assert($specs->at(1) == $unchanged, |'expected second spec unchanged');",
                "assert($array.aggSpecs->at(0) == $folding, |'the input array must not be mutated');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');");
    }

    @Test
    public void testPrevalNativeReportsOpenVariablesOfFunctionExpression()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {p:Integer[1] | $p + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.value == $fe, |'expected the same function expression back');",
                "assert($r.openVars->size() == 1 && $r.openVars->at(0) == 'p', |'expected open variable p');");
    }

    @Test
    public void testUnsupportedValueIsReportedAtTheCallSite()
    {
        compileTestSource("unsupported.pure", "Class test::preeval::Opaque {}\n");
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "let v = ^test::preeval::Opaque();",
                    "let hooks = ^PrevalHooks(stopPreeval = {a:Any[*] | !$a->forAll(x | $x->instanceOf(test::preeval::Opaque))}, shouldInline = {f:Function<Any>[1] | false}, isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, isGetAllFunction = {f:Function<Any>[1] | false}, resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []});",
                    "let vars = newMap(pair('v', list($v)));",
                    "let fe = {|$v}->evaluateAndDeactivate().expressionSequence->at(0);",
                    "assertError(|prevalNative($fe, $vars, $vars, $hooks, noDebug()), {m:String[1], s:SourceInformation[0..1] | assert($m->contains('Unsupported type: test::preeval::Opaque') && $s->isNotEmpty(), |$m)});");
        }
        finally
        {
            runtime.delete("fromString.pure");
            runtime.delete("unsupported.pure");
            runtime.compile();
        }
    }

    @Test
    public void testFloatPrecisionSurvivesInstanceValueRebuild()
    {
        executeTestFunction(
                "let vars = newMap(pair('x', list(2)));",
                "let x = 0;",
                "let iv = {|[1.1234567890123456789012, $x]}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($iv, $vars, $vars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->cast(@InstanceValue).values->at(0) == 1.1234567890123456789012, |'expected exact Float');");
    }

    @Test
    public void testUnsupportedTypeNamesThePureType()
    {
        executeTestFunction(
                "let v = 3;",
                "let hooks = ^PrevalHooks(stopPreeval = {a:Any[*] | $a->forAll(x | !$x->instanceOf(Integer))}, shouldInline = {f:Function<Any>[1] | false}, isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, isGetAllFunction = {f:Function<Any>[1] | false}, resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []});",
                "let vars = newMap(pair('v', list($v)));",
                "let fe = {|$v}->evaluateAndDeactivate().expressionSequence->at(0);",
                "assertError(|prevalNative($fe, $vars, $vars, $hooks, noDebug()), {m:String[1], s:SourceInformation[0..1] | assert($m->contains('Unsupported type: Integer'), |$m)});");
    }

    @Test
    public void testPrevalNativeShortCircuitsIfWithConstantCondition()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let f = {|if(true, |'a', |[]->toOne())};",
                "let r = prevalNative($f, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "let body = $r.value->cast(@LambdaFunction<Any>).expressionSequence->at(0);",
                "assert($body->instanceOf(InstanceValue), |'expected an instance value');",
                "assert($body->cast(@InstanceValue).values->toOne() == 'a', |'expected a');");
    }

    @Test
    public void testPrevalNativeAndKeepsOpenSecondOperandWhenFirstIsTrue()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {b:Boolean[1] | true && $b}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->instanceOf(VariableExpression), |'expected a variable expression');",
                "assert($r.value->cast(@VariableExpression).name == 'b', |'expected variable b');",
                "assert($r.openVars->size() == 1 && $r.openVars->at(0) == 'b', |'expected open variable b');");
    }

    @Test
    public void testPrevalNativeOrShortCircuitsWhenSecondIsTrue()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {b:Boolean[1] | $b || true}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->instanceOf(InstanceValue), |'expected an instance value');",
                "assert($r.value->cast(@InstanceValue).values->toOne() == true, |'expected true');");
    }

    @Test
    public void testInlinesSingleExpressionFunction()
    {
        compileTestSource("inline.pure", "function test::preeval::addOne(i:Integer[1]):Integer[1]\n{\n    $i + 1\n}\n");
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "let fe = {|test::preeval::addOne(2)}->evaluateAndDeactivate().expressionSequence->at(0);",
                    "let r = prevalNative($fe, $emptyVars, $emptyVars, " + INLINING_HOOKS + ", noDebug());",
                    "assert($r.modified, |'expected modified');",
                    "assert($r.value->instanceOf(InstanceValue), |'expected an instance value');",
                    "assert($r.value->cast(@InstanceValue).values->toOne() == 3, |'expected 3');",
                    "let open = {j:Integer[1] | test::preeval::addOne($j)}->evaluateAndDeactivate().expressionSequence->at(0);",
                    "let o = prevalNative($open, $emptyVars, $emptyVars, " + INLINING_HOOKS + ", noDebug());",
                    "assert($o.modified, |'expected open call modified');",
                    "assert($o.value->cast(@FunctionExpression).func.functionName == 'plus', |'expected the inlined body');",
                    "assert($o.openVars->size() == 1 && $o.openVars->at(0) == 'j', |'expected open variable j');");
        }
        finally
        {
            runtime.delete("fromString.pure");
            runtime.delete("inline.pure");
            runtime.compile();
        }
    }

    @Test
    public void testDoesNotInlineRecursivePath()
    {
        compileTestSource("inline.pure", "function test::preeval::loop(i:Integer[1]):Integer[1]\n{\n    test::preeval::loop($i)\n}\n");
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "let fe = {i:Integer[1] | test::preeval::loop($i)}->evaluateAndDeactivate().expressionSequence->at(0);",
                    "let r = prevalNative($fe, $emptyVars, $emptyVars, " + INLINING_HOOKS + ", noDebug());",
                    "assert($r.modified, |'expected modified');",
                    "assert($r.value->instanceOf(FunctionExpression), |'expected a function expression');",
                    "assert($r.value->cast(@FunctionExpression).func == test::preeval::loop_Integer_1__Integer_1_, |'expected the recursive call to remain');",
                    "assert($r.openVars->size() == 1 && $r.openVars->at(0) == 'i', |'expected open variable i');");
        }
        finally
        {
            runtime.delete("fromString.pure");
            runtime.delete("inline.pure");
            runtime.compile();
        }
    }

    @Test
    public void testExpandsEvalOfLiteralLambda()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|{x:Integer[1] | $x + 1}->eval(4)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->instanceOf(InstanceValue), |'expected an instance value');",
                "assert($r.value->cast(@InstanceValue).values->toOne() == 5, |'expected 5');",
                "let open = {y:Integer[1] | {x:Integer[1] | $x + 1}->eval($y)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let o = prevalNative($open, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($o.modified, |'expected open eval modified');",
                "assert($o.value->cast(@FunctionExpression).func.functionName == 'plus', |'expected the lambda body');",
                "assert($o.openVars->size() == 1 && $o.openVars->at(0) == 'y', |'expected open variable y');");
    }

    @Test
    public void testReactivatedNestedCollectionIsFlattened()
    {
        compileTestSource("inline.pure", "function test::preeval::firstFive(s:String[1]):String[1]\n{\n    $s->map(k|$s->substring(0, 5))\n}\n");
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "let f = {|['hello']->map(a|$a->test::preeval::firstFive())};",
                    "let r = prevalNative($f, $emptyVars, $emptyVars, " + INLINING_HOOKS + ", noDebug());",
                    "let body = $r.value->cast(@LambdaFunction<Any>).expressionSequence->at(0)->cast(@InstanceValue);",
                    "assert($body.values->size() == 1, |'expected one value');",
                    "assert($body.values->toOne()->instanceOf(String), |'expected a String value, not a nested collection');",
                    "assert($body.values->toOne() == 'hello', |'expected hello');");
        }
        finally
        {
            runtime.delete("fromString.pure");
            runtime.delete("inline.pure");
            runtime.compile();
        }
    }

    @Test
    public void testRewrittenLambdaEvaluatesItsNewBody()
    {
        compileTestSource("lambda.pure", "function test::preeval::suffixer():LambdaFunction<{->LambdaFunction<{->String[1]}>[1]}>[1]\n{\n    {|let x = 'e'; {|'ag' + $x};}\n}\n");
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "let hooks = ^PrevalHooks(stopPreeval = {a:Any[*] | !$a->exists(x | $x->instanceOf(meta::pure::metamodel::valuespecification::FunctionExpression) && ($x->cast(@meta::pure::metamodel::valuespecification::FunctionExpression).func.functionName != 'letFunction'))}, shouldInline = {f:Function<Any>[1] | false}, isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, isGetAllFunction = {f:Function<Any>[1] | false}, resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []});",
                    "let r = prevalNative(test::preeval::suffixer(), $emptyVars, $emptyVars, $hooks, noDebug());",
                    "assert($r.modified, |'expected modified');",
                    "let rewritten = $r.value->cast(@LambdaFunction<{->LambdaFunction<{->String[1]}>[1]}>);",
                    "assert($rewritten.expressionSequence->size() == 1, |'expected the let to be inlined');",
                    "let nested = $rewritten->eval();",
                    "assert($nested.expressionSequence->at(0)->instanceOf(InstanceValue), |'expected evaluation to return the rewritten nested lambda');",
                    "assert($nested->eval() == 'age', |'expected age');");
        }
        finally
        {
            runtime.delete("fromString.pure");
            runtime.delete("lambda.pure");
            runtime.compile();
        }
    }

    @Test
    public void testUnrollsMapOverInstanceValues()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|[1, 2]->map(x | $x + 1)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "let iv = $r.value->cast(@InstanceValue);",
                "assert(($iv.values->size() == 2) && ($iv.values->at(0) == 2) && ($iv.values->at(1) == 3), |'expected 2, 3');",
                "assert(($iv.multiplicity.lowerBound.value == 2) && ($iv.multiplicity.upperBound.value == 2), |'expected multiplicity [2]');",
                "let open = {y:Integer[1] | [1, 2]->map(x | $x + $y)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let o = prevalNative($open, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($o.modified, |'expected open map modified');",
                "assert($o.value->instanceOf(InstanceValue), |'expected the map to be unrolled');",
                "let bodies = $o.value->cast(@InstanceValue);",
                "assert(($bodies.values->size() == 2) && $bodies.values->forAll(v | $v->instanceOf(FunctionExpression) && ($v->cast(@FunctionExpression).func.functionName == 'plus')), |'expected two unrolled bodies');",
                "assert(($bodies.multiplicity.lowerBound.value == 2) && ($bodies.multiplicity.upperBound.value == 2), |'expected open multiplicity [2]');",
                "assert($o.openVars->size() == 1 && $o.openVars->at(0) == 'y', |'expected open variable y');");
    }

    @Test
    public void testUnrollsFoldOverInstanceValues()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|[1, 2, 3]->fold({x, a | $a + $x}, 0)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->cast(@InstanceValue).values->toOne() == 6, |'expected 6');",
                "let open = {y:Integer[1] | [1, 2]->fold({x, a | $a + $x + $y}, 0)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let o = prevalNative($open, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($o.modified, |'expected open fold modified');",
                "assert($o.value->instanceOf(FunctionExpression) && ($o.value->cast(@FunctionExpression).func.functionName == 'plus'), |'expected the fold to be unrolled');",
                "assert($o.openVars->size() == 1 && $o.openVars->at(0) == 'y', |'expected open variable y');");
    }

    @Test
    public void testConcatenatesExactMultiplicityParameters()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|[1, 2]->concatenate([3])}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "let iv = $r.value->cast(@InstanceValue);",
                "assert(($iv.values->size() == 3) && ($iv.values->at(0) == 1) && ($iv.values->at(1) == 2) && ($iv.values->at(2) == 3), |'expected 1, 2, 3');",
                "let open = {y:Integer[1] | [1, 2]->concatenate($y)}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let o = prevalNative($open, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($o.modified, |'expected open concatenate modified');",
                "assert($o.value->instanceOf(InstanceValue), |'expected the concatenation to be expanded');",
                "let values = $o.value->cast(@InstanceValue).values;",
                "assert(($values->size() == 3) && ($values->at(0) == 1) && ($values->at(1) == 2) && $values->at(2)->instanceOf(VariableExpression), |'expected 1, 2, $y');",
                "assert(($o.value->cast(@InstanceValue).multiplicity.lowerBound.value == 3) && ($o.value->cast(@InstanceValue).multiplicity.upperBound.value == 3), |'expected multiplicity [3]');",
                "assert($o.openVars->size() == 1 && $o.openVars->at(0) == 'y', |'expected open variable y');");
    }

    @Test
    public void testPreevalImplementationIsAKnownValue()
    {
        executeTestFunction(
                "let implementation = preevalImplementation();",
                "assert(($implementation == 'PURE') || ($implementation == 'JAVA') || ($implementation == 'SHADOW'), |'unexpected: ' + $implementation);");
    }

    private void executeTestFunction(String... lines)
    {
        String code = "import meta::pure::functions::preeval::*;\n\nfunction test():Any[*]\n{\n    " + String.join("\n    ", lines) + "\n}\n";
        compileTestSource("fromString.pure", code);
        CoreInstance function = runtime.getFunction("test():Any[*]");
        try
        {
            functionExecution.start(function, Lists.immutable.empty());
        }
        catch (PureAssertFailException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        CodeRepositorySet repositories = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true))
                .build()
                .subset("core_functions_preeval");
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories.getRepositories()));
    }
}
