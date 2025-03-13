// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.meta;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestFunctionDescriptorToId extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("testFunc.pure");
        runtime.compile();
    }

    @Test
    public void testInvalidFunctionDescriptorException()
    {
        compileTestSource("testFunc.pure",
                "function test():String[1]\n" +
                        "{\n" +
                        "   meta::pure::functions::meta::functionDescriptorToId('meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]');\n" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():String[1]"));
        assertPureException(PureExecutionException.class, "Invalid function descriptor: meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]", "testFunc.pure", 3, 33, e);
    }
}
