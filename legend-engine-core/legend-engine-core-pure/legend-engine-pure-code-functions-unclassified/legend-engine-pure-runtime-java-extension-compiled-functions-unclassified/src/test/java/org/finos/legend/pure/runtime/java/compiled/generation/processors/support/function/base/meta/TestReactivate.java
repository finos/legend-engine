// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.meta;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.meta.AbstractTestReactivate;
import org.finos.legend.pure.m3.tools.ThrowableTools;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompileException;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestReactivate extends AbstractTestReactivate
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testVariableScopeFail()
    {
        Exception e = Assert.assertThrows(Exception.class, this::compileAndExecuteVariableScopeFailure);
        Throwable cause = ThrowableTools.findRootThrowable(e);
        Assert.assertEquals(PureJavaCompileException.class, cause.getClass());

        String expected = Pattern.quote("1 error compiling /" + JavaPackageAndImportBuilder.rootPackageFolder() + "/DynaClass.java\n" +
                "/" + JavaPackageAndImportBuilder.rootPackageFolder() + "/DynaClass.java:") + "\\d*" + Pattern.quote(": error: cannot find symbol\n" +
                "       return (long)CompiledSupport.plus(Lists.mutable.<java.lang.Long>with(_a,3l));\n" +
                "                                                                            ^\n" +
                "  symbol:   variable _a\n" +
                "  location: class " + JavaPackageAndImportBuilder.rootPackage() + ".DynaClass\n");
        Pattern expectedPattern = Pattern.compile(expected);
        Matcher matcher = expectedPattern.matcher(cause.getMessage());
        Assert.assertTrue("Failed to find pattern in message:\n" + cause.getMessage(), matcher.find());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
