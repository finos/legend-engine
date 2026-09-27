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

    @After
    public void deleteTestSource()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPrevalNativeReturnsInputUnmodified()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let f = {|1 + 1};",
                "let r = prevalNative($f, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');",
                "assert($r.value->instanceOf(LambdaFunction), |'expected the input lambda back');",
                "assert($r.value == $f, |'expected the same lambda back');");
    }

    @Test
    public void testPrevalNativeReturnsFunctionExpressionUnmodified()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {|1 + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.canPreval, |'expected canPreval');",
                "assert($r.openVars->isEmpty(), |'expected no open variables');",
                "assert($r.value == $fe, |'expected the same function expression back');");
    }

    @Test
    public void testPrevalNativeReportsOpenVariablesOfFunctionExpression()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {p:Integer[1] | $p + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.value == $fe, |'expected the same function expression back');",
                "assert($r.openVars->size() == 1 && $r.openVars->at(0) == 'p', |'expected open variable p');");
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
