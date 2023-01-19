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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.external.language.java.runtime.compiler.SourceCodeHelper;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertTrue(result.isSuccess());
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
        String expectedMessage1 = sourceCode.getName() + ":14:13" + separator + "cannot assign a value to final variable name";
        String expectedMessage2 = sourceCode.getName() + ":14:21" + separator + "incompatible types: java.lang.Integer cannot be converted to java.lang.String";
        Assert.assertEquals(Arrays.asList(expectedMessage1, expectedMessage2), result.getErrorMessages());
    }

    @Test
    public void testExecuteSuccess()
    {
        String validClassName = SourceCodeHelper.VALID_CLASS;
        JavaFileObject sourceCode = SourceCodeHelper.loadSourceCode(validClassName);
        CompilationResult compResult = CompileAndExecuteJava.compile(Collections.singletonList(sourceCode), Collections.emptyList());

        ExecutionResult execResult = CompileAndExecuteJava.execute(compResult, validClassName, "succeed");
        Assert.assertTrue(execResult.isSuccess());
        Assert.assertEquals("the quick brown fox jumps over the lazy dog", execResult.getResult());
        Assert.assertNull(execResult.getError());
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
