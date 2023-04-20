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

package org.finos.legend.engine.external.language.java.runtime.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestCompileAndExecuteJava extends AbstractPureTestWithCoreCompiled
{
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
                "assert($emptyResult.executionResult.error->toOne()->startsWith('java.lang.ClassNotFoundException: not.a.real.Cls'), |'unexpected error: ' + $emptyResult.executionResult.error->toOne());",
                "assert($emptyResult.executionResult.returnValue->isEmpty(), |'unexpected returnValue: ' + $emptyResult.executionResult.returnValue->toOne()->toString());");
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
                "let expectedErrors = ['/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:28:13" + lineSep + "cannot assign a value to final variable name', '/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:28:21" + lineSep + "incompatible types: java.lang.Integer cannot be converted to java.lang.String'];",
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
                "let expectedErrors = ['/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:28:13" + lineSep + "cannot assign a value to final variable name', '/org/finos/legend/engine/external/language/java/runtime/compiler/shared/InvalidJavaClass.java:28:21" + lineSep + "incompatible types: java.lang.Integer cannot be converted to java.lang.String'];",
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
    public void testCompileAndExecuteValidJavaExecuteSuccess_Void()
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
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaNull(), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_String()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnString'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaObject(class='java.lang.String', string='the quick brown fox jumps over the lazy dog'), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_Integer()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnInt'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaObject(class='java.lang.Integer', string='4'), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_StringArray()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnArray'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaArray(componentType='java.lang.String', values=[^JavaObject(class='java.lang.String', string='the quick'), ^JavaObject(class='java.lang.String', string='brown fox'), ^JavaObject(class='java.lang.String', string='jumps over'), ^JavaObject(class='java.lang.String', string='the lazy dog')]), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_IntArray()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnIntArray'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaArray(componentType='int', values=[^JavaPrimitive(type='int', value='9'), ^JavaPrimitive(type='int', value='9'), ^JavaPrimitive(type='int', value='10'), ^JavaPrimitive(type='int', value='12')]), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_StringList()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnList'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaIterable(class='java.util.Arrays$ArrayList', values=[^JavaObject(class='java.lang.String', string='the quick'), ^JavaObject(class='java.lang.String', string='brown fox'), ^JavaObject(class='java.lang.String', string='jumps over'), ^JavaObject(class='java.lang.String', string='the lazy dog')]), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
    }

    @Test
    public void testCompileAndExecuteValidJavaExecuteSuccess_Map()
    {
        String validJavaCode = loadSourceCodeEscaped(SourceCodeHelper.VALID_CLASS);
        int lastDot = SourceCodeHelper.VALID_CLASS.lastIndexOf('.');
        String packageName = SourceCodeHelper.VALID_CLASS.substring(0, lastDot);
        String className = SourceCodeHelper.VALID_CLASS.substring(lastDot + 1);
        compileAndExecute(
                "let validResult = compileAndExecuteJava(^JavaSource(package='" + packageName + "', name='" + className + "', content='" + validJavaCode + "'), [], ^ExecutionConfiguration(class='" + SourceCodeHelper.VALID_CLASS + "', method='succeedReturnMap'));",
                "assert($validResult.compilationResult.successful, |'expected successful compilation');",
                "assert($validResult.compilationResult.errors->isEmpty(), |$validResult.compilationResult.errors->joinStrings('expected no errors, got: \\'', '\\', \\'', '\\''));",
                "assert(!$validResult.executionResult->isEmpty(), |'expected non-empty execution result');",
                "assert($validResult.executionResult->toOne().successful, |'expected successful execution');",
                "assert($validResult.executionResult->toOne().error->isEmpty(), |'expected no execution error, got: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue == ^JavaMap(class='java.util.HashMap', keyValuePairs=[",
                "    pair(^JavaObject(class='java.lang.Character', string='a'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='36')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='b'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='10')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='c'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='7')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='d'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='40')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='e'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='2'), ^JavaObject(class='java.lang.Integer', string='28'), ^JavaObject(class='java.lang.Integer', string='33')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='f'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='16')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='g'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='42')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='h'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='1'), ^JavaObject(class='java.lang.Integer', string='32')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='i'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='6')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='j'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='20')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='k'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='8')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='l'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='35')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='m'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='22')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='n'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='14')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='o'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='12'), ^JavaObject(class='java.lang.Integer', string='17'), ^JavaObject(class='java.lang.Integer', string='26'), ^JavaObject(class='java.lang.Integer', string='41')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='p'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='23')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='q'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='4')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='r'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='11'), ^JavaObject(class='java.lang.Integer', string='29')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='s'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='24')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='t'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='0'), ^JavaObject(class='java.lang.Integer', string='31')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='u'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='5'), ^JavaObject(class='java.lang.Integer', string='21')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='v'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='27')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='w'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='13')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='x'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='18')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='y'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='38')])),",
                "    pair(^JavaObject(class='java.lang.Character', string='z'), ^JavaIterable(class='java.util.ArrayList', values=[^JavaObject(class='java.lang.Integer', string='37')]))",
                "]), |'unexpected return value, got: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
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
                "assert($validResult.executionResult->toOne().error->toOne()->contains('Caused by: java.lang.RuntimeException: Oh no! A failure!'), |'unexpected execution error: ' + $validResult.executionResult->toOne().error->toOne());",
                "assert($validResult.executionResult->toOne().returnValue->isEmpty(), |'unexpected execution return value: ' + $validResult.executionResult->toOne().returnValue->toOne()->toString());");
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

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        CodeRepositorySet repositories = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true))
                .build()
                .subset("core_external_language_java_compiler");

        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories.getRepositories()));
    }

    private static String loadSourceCodeEscaped(String className)
    {
        String code = SourceCodeHelper.loadSourceCodeContent(className);
        return code.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replaceAll("\\R", "\\\\n");
    }
}
