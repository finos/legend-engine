//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.external.language.java.runtime.compiler.SourceCodeHelper;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCompileAndExecuteJavaInterpreted extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(new FunctionExecutionInterpreted(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra());
        runtime.loadAndCompileSystem();
    }

    @After
    public void deleteTestSources()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testCompileEmpty()
    {
        compileAndExecute(
                "let emptyResult = compileJava([]->cast(@JavaSource), []);",
                "assert($emptyResult.successful, |'expected successful compilation');",
                "assert($emptyResult.errors->isEmpty(), |$emptyResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));");
    }

    @Test
    public void testCompileAndExecuteEmpty()
    {
        compileAndExecute(
                "let emptyResult = compileAndExecuteJava([]->cast(@JavaSource), [], ^ExecutionConfiguration(class='not.a.real.Cls', method='none'));",
                "assert($emptyResult.compilationResult.successful, |'expected successful compilation');",
                "assert(!$emptyResult.executionResult->toOne().successful, |'expected failed execution');",
                "assert($emptyResult.executionResult.error->toOne()->startsWith('java.lang.ClassNotFoundException: not.a.real.Cls'), |'unexpected error: ' + $emptyResult.executionResult.error->toOne());");
    }

    @Test
    public void testCompileInvalidJava()
    {
        String invalidJavaCode = loadSourceCodeEscaped(SourceCodeHelper.INVALID_CLASS);
        int lastDot = SourceCodeHelper.INVALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.INVALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.INVALID_CLASS.substring(lastDot + 1);
        String lineSep = System.lineSeparator().replace("\n", "\\n").replace("\r", "\\r");
        compileAndExecute(
                "let invalidResult = compileJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + invalidJavaCode + "'), []);",
                "assert(false == $invalidResult.successful, |'expected unsuccessful compilation');",
                "let expectedErrors = ['/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:14:13" + lineSep + "cannot assign a value to final variable name', '/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:14:21" + lineSep + "incompatible types: java.lang.Integer cannot be converted to java.lang.String'];",
                "assert($expectedErrors == $invalidResult.errors, |'expected: ' + $expectedErrors->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']') + '\\nactual: ' + $invalidResult.errors->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']'));");
    }

    @Test
    public void testCompileAndExecuteInvalidJava()
    {
        String invalidJavaCode = loadSourceCodeEscaped(SourceCodeHelper.INVALID_CLASS);
        int lastDot = SourceCodeHelper.INVALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.INVALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.INVALID_CLASS.substring(lastDot + 1);
        String lineSep = System.lineSeparator().replace("\n", "\\n").replace("\r", "\\r");
        compileAndExecute(
                "let invalidResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + invalidJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.INVALID_CLASS + "', method='main'));",
                "assert(false == $invalidResult.compilationResult.successful, |'expected unsuccessful compilation');",
                "let expectedErrors = ['/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:14:13" + lineSep + "cannot assign a value to final variable name', '/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:14:21" + lineSep + "incompatible types: java.lang.Integer cannot be converted to java.lang.String'];",
                "assert($expectedErrors == $invalidResult.compilationResult.errors, |'expected: ' + $expectedErrors->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']') + '\\nactual: ' + $invalidResult.compilationResult.errors->map(x | $x->toRepresentation())->joinStrings('[', ', ', ']'));",
                "assert($invalidResult.executionResult->isEmpty(), |'expected empty execution result, got: ' + $invalidResult.executionResult->toOne()->toString());");
    }


    @Test
    public void testCompileValidJava()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), []);",
                "assert($validResult.successful, |'expected successful compilation');",
                "assert($validResult.errors->isEmpty(), |$validResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeed'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteFailure()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='fail'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert(!$validResult.executionResult->toOne().successful, |'expected unsuccessful execution');",
                "assert(!$validResult.executionResult->toOne().error->isEmpty(), |'expected an execution error');",
                "assert($validResult.executionResult->toOne().error->toOne()->contains('Caused by: java.lang.RuntimeException: Oh no! A failure!'), |'expected an execution error');");
    }

    private void compileAndExecute(String... lines)
    {
        String code = ArrayIterate.makeString(lines, "import meta::external::language::java::compiler::*;\n\nfunction test():Any[*]\n{\n    ", "\n    ", "\n}");
        try
        {
            compileTestSource("fromString.pure", code);
        }
        catch (PureParserException e)
        {
            throw new PureParserException(e.getSourceInformation(), "Error parsing:\n" + code, e);
        }
        catch (PureCompilationException e)
        {
            throw new PureCompilationException(e.getSourceInformation(), "Error compiling:\n" + code, e);
        }

        CoreInstance function = runtime.getFunction("test():Any[*]");
        functionExecution.getConsole().clear();
        try
        {
            functionExecution.start(function, Lists.immutable.empty());
        }
        catch (PureAssertFailException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        CodeRepositorySet repositories = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true))
                .build()
                .subset("core_external_language_java_compiler");

        return PureCodeStorage.createCodeStorage(null, repositories.getRepositories());
    }

    private static String loadSourceCodeEscaped(String className)
    {
        String code = SourceCodeHelper.loadSourceCodeContent(className);
        return code.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replaceAll("\\R", "\\\\n");
    }
}
