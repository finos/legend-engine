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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestCast;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;

import javax.lang.model.SourceVersion;

public class TestCastCompiled extends AbstractTestCast
{
    @BeforeClass
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), AbstractTestCast.getCodeStorage(), JavaModelFactoryRegistryLoader.loader(), getExtra());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Override
    protected void checkInvalidCastWithTypeParametersTopLevelException(PureExecutionException e)
    {
        AbstractPureTestWithCoreCompiled.assertPureException(PureExecutionException.class, "Unexpected error executing function", "fromString.pure", 1, 1, 1, 10, 4, 1, e);
    }

    @Override
    protected void checkInvalidCastWithTypeParametersRootException(PureExecutionException e)
    {
        Exception root = findRootException(e);
        Assert.assertTrue(root instanceof ClassCastException);
        String message = root.getMessage();
        if (SourceVersion.latest().compareTo(SourceVersion.RELEASE_8) > 0)
        {
            String expectedStart = "class " + JavaPackageAndImportBuilder.rootPackage() + ".Root_X_Impl cannot be cast to class " + JavaPackageAndImportBuilder.rootPackage() + ".Root_Y";
            if (!message.startsWith(expectedStart))
            {
                Assert.assertEquals("should start with expected", expectedStart, message);
            }
        }
        else
        {
            String expectedMessage = JavaPackageAndImportBuilder.rootPackage() + ".Root_X_Impl incompatible with " + JavaPackageAndImportBuilder.rootPackage() + ".Root_Y";
            Assert.assertEquals(expectedMessage, message);
        }
    }

    @Override
    protected void checkPrimitiveConcreteOneTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkPrimitiveConcreteManyTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkNonPrimitiveConcreteOneTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkNonPrimitiveConcreteManyTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkEnumToStringCastTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkPrimitiveNonConcreteOneTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkPrimitiveNonConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: Integer cannot be cast to String", "/test/cast.pure", 51, 10);
    }

    @Override
    protected void checkNonPrimitiveNonConcreteOneTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkNonPrimitiveNonConcreteOneRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 61, 12);
    }

    @Override
    protected void checkPrimitiveNonConcreteManyTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkPrimitiveNonConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: String cannot be cast to Number", "/test/cast.pure", 56, 13);
    }

    @Override
    protected void checkNonPrimitiveNonConcreteManyTopLevelException(PureExecutionException e)
    {
        Assert.assertSame(e, findRootException(e));
    }

    @Override
    protected void checkNonPrimitiveNonConcreteManyRootException(PureExecutionException e)
    {
        checkException(findRootException(e), "Cast exception: X cannot be cast to Y", "/test/cast.pure", 61, 12);
    }

    @Override
    protected void checkStringToEnumCastTopLevelException(PureExecutionException e)
    {
        checkException(e, "Cast exception: String cannot be cast to Enum", "fromString.pure", 3, 17);
    }
}
