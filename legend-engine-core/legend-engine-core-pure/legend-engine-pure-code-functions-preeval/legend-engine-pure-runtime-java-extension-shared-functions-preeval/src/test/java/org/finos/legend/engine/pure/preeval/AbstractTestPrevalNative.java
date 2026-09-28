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
