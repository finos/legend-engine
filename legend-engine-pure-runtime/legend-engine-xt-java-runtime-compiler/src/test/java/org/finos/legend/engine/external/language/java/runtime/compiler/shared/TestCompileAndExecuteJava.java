// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.external.language.java.runtime.compiler.SourceCodeHelper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class TestCompileAndExecuteJava
{
    @Test
    public void testCompileSuccess() throws ClassNotFoundException
    {
        Assert.assertThrows(ClassNotFoundException.class, () -> Thread.currentThread().getContextClassLoader().loadClass(SourceCodeHelper.VALID_CLASS));

        JavaFileObject sourceCode = SourceCodeHelper.loadSourceCode(SourceCodeHelper.VALID_CLASS);
        CompilationResult result = CompileAndExecuteJava.compile(Collections.singletonList(sourceCode), Collections.emptyList());
        Assert.assertTrue(result.getErrorMessages().makeString("\n"), result.isSuccess());
        Assert.assertEquals(Collections.emptyList(), result.getDiagnostics());
        Assert.assertEquals(Collections.emptyList(), result.getErrorMessages());
        ClassLoader classLoader = result.getClassLoader();
        Assert.assertNotNull(classLoader);
        Class<?> cls = classLoader.loadClass(SourceCodeHelper.VALID_CLASS);
        Assert.assertEquals(SourceCodeHelper.VALID_CLASS, cls.getName());

        Assert.assertThrows(ClassNotFoundException.class, () -> Thread.currentThread().getContextClassLoader().loadClass(SourceCodeHelper.VALID_CLASS));
    }

    @Test
    public void testCompileError()
    {
        JavaFileObject sourceCode = SourceCodeHelper.loadSourceCode(SourceCodeHelper.INVALID_CLASS);
        CompilationResult result = CompileAndExecuteJava.compile(Collections.singletonList(sourceCode), Collections.emptyList());
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getClassLoader());

        List<Diagnostic<? extends JavaFileObject>> diagnostics = Lists.mutable.withAll(result.getDiagnostics());
        Assert.assertEquals(Collections.singleton(Diagnostic.Kind.ERROR), Iterate.collect(diagnostics, Diagnostic::getKind, EnumSet.noneOf(Diagnostic.Kind.class)));
        Assert.assertEquals(Collections.singleton(sourceCode), Iterate.collect(diagnostics, Diagnostic::getSource, Sets.mutable.empty()));

        String separator = System.lineSeparator();
        String expectedMessage1 = sourceCode.getName() + ":28:13" + separator + "cannot assign a value to final variable name";
        String expectedMessage2 = sourceCode.getName() + ":28:21" + separator + "incompatible types: java.lang.Integer cannot be converted to java.lang.String";
        Assert.assertEquals(Arrays.asList(expectedMessage1, expectedMessage2), result.getErrorMessages());
    }

    @Test
    public void testExecuteSuccess()
    {
        String validClassName = SourceCodeHelper.VALID_CLASS;
        JavaFileObject sourceCode = SourceCodeHelper.loadSourceCode(validClassName);
        CompilationResult compResult = CompileAndExecuteJava.compile(Collections.singletonList(sourceCode), Collections.emptyList());

        testExecuteSuccess(null, compResult, validClassName, "succeed");
        testExecuteSuccess("the quick brown fox jumps over the lazy dog", compResult, validClassName, "succeedReturnString");
        testExecuteSuccess(4, compResult, validClassName, "succeedReturnInt");
        testExecuteSuccess(new String[]{"the quick", "brown fox", "jumps over", "the lazy dog"}, compResult, validClassName, "succeedReturnArray");
        testExecuteSuccess(new int[]{9, 9, 10, 12}, compResult, validClassName, "succeedReturnIntArray");
        testExecuteSuccess(Lists.mutable.with("the quick", "brown fox", "jumps over", "the lazy dog"), compResult, validClassName, "succeedReturnList");
        MutableMap<Character, List<Integer>> expectedMap = Maps.mutable.<Character, List<Integer>>empty()
                .withKeyValue('a', Lists.mutable.with(36))
                .withKeyValue('b', Lists.mutable.with(10))
                .withKeyValue('c', Lists.mutable.with(7))
                .withKeyValue('d', Lists.mutable.with(40))
                .withKeyValue('e', Lists.mutable.with(2, 28, 33))
                .withKeyValue('f', Lists.mutable.with(16))
                .withKeyValue('g', Lists.mutable.with(42))
                .withKeyValue('h', Lists.mutable.with(1, 32))
                .withKeyValue('i', Lists.mutable.with(6))
                .withKeyValue('j', Lists.mutable.with(20))
                .withKeyValue('k', Lists.mutable.with(8))
                .withKeyValue('l', Lists.mutable.with(35))
                .withKeyValue('m', Lists.mutable.with(22))
                .withKeyValue('n', Lists.mutable.with(14))
                .withKeyValue('o', Lists.mutable.with(12, 17, 26, 41))
                .withKeyValue('p', Lists.mutable.with(23))
                .withKeyValue('q', Lists.mutable.with(4))
                .withKeyValue('r', Lists.mutable.with(11, 29))
                .withKeyValue('s', Lists.mutable.with(24))
                .withKeyValue('t', Lists.mutable.with(0, 31))
                .withKeyValue('u', Lists.mutable.with(5, 21))
                .withKeyValue('v', Lists.mutable.with(27))
                .withKeyValue('w', Lists.mutable.with(13))
                .withKeyValue('x', Lists.mutable.with(18))
                .withKeyValue('y', Lists.mutable.with(38))
                .withKeyValue('z', Lists.mutable.with(37));
        testExecuteSuccess(expectedMap, compResult, validClassName, "succeedReturnMap");
    }

    private void testExecuteSuccess(Object expectedResult, CompilationResult compResult, String className, String methodName)
    {
        String failureMessage = className + "." + methodName;
        ExecutionResult execResult = CompileAndExecuteJava.execute(compResult, className, methodName);
        Assert.assertTrue(failureMessage, execResult.isSuccess());
        Assert.assertNull(failureMessage, execResult.getError());
        Object actualResult = execResult.getResult();
        if ((expectedResult == null) || !expectedResult.getClass().isArray() || !actualResult.getClass().isArray())
        {
            Assert.assertEquals(failureMessage, expectedResult, actualResult);
        }
        else
        {
            Assert.assertEquals(failureMessage, arrayToList(expectedResult), arrayToList(actualResult));
        }
    }

    private MutableList<Object> arrayToList(Object array)
    {
        int length = Array.getLength(array);
        MutableList<Object> result = Lists.mutable.ofInitialCapacity(length);
        for (int i = 0; i < length; i++)
        {
            result.add(Array.get(array, i));
        }
        return result;
    }

    @Test
    public void testExecuteError()
    {
        JavaFileObject sourceCode = SourceCodeHelper.loadSourceCode(SourceCodeHelper.VALID_CLASS);
        CompilationResult compResult = CompileAndExecuteJava.compile(Collections.singletonList(sourceCode), Collections.emptyList());

        // Unknown class
        ExecutionResult unknownClassResult = CompileAndExecuteJava.execute(compResult, "not.a.known.Class", "fail");
        Assert.assertFalse(unknownClassResult.isSuccess());
        Assert.assertNull(unknownClassResult.getResult());

        Throwable unknownClassError = unknownClassResult.getError();
        Assert.assertNotNull(unknownClassError);
        Assert.assertEquals(ClassNotFoundException.class, unknownClassError.getClass());
        Assert.assertEquals("not.a.known.Class", unknownClassError.getMessage());

        // Unknown method
        ExecutionResult unknownMethodResult = CompileAndExecuteJava.execute(compResult, SourceCodeHelper.VALID_CLASS, "notAMethod");
        Assert.assertFalse(unknownMethodResult.isSuccess());
        Assert.assertNull(unknownMethodResult.getResult());

        Throwable unknownMethodError = unknownMethodResult.getError();
        Assert.assertNotNull(unknownMethodError);
        Assert.assertEquals(NoSuchMethodException.class, unknownMethodError.getClass());
        Assert.assertEquals(SourceCodeHelper.VALID_CLASS + ".notAMethod()", unknownMethodError.getMessage());

        // Runtime error
        ExecutionResult execFailResult = CompileAndExecuteJava.execute(compResult, SourceCodeHelper.VALID_CLASS, "fail");
        Assert.assertFalse(execFailResult.isSuccess());
        Assert.assertNull(execFailResult.getResult());

        Throwable execError = execFailResult.getError();
        Assert.assertNotNull(execError);
        Assert.assertEquals(InvocationTargetException.class, execError.getClass());

        Throwable execErrorCause = execError.getCause();
        Assert.assertNotNull(execErrorCause);
        Assert.assertEquals(RuntimeException.class, execErrorCause.getClass());
        Assert.assertNull(execErrorCause.getCause());
        Assert.assertEquals("Oh no! A failure!", execErrorCause.getMessage());
    }
}
