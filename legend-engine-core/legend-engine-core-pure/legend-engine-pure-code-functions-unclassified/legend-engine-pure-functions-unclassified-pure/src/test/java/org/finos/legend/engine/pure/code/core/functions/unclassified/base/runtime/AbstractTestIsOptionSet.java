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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.runtime;

import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestIsOptionSet extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testOptionThatIsSetOn()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::core::runtime::isOptionSet('TestSetOn');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("true", ValueSpecification.getValue(result, processorSupport).getName());
    }

    @Test
    public void testOptionThatIsSetOff()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::core::runtime::isOptionSet('TestSetOff');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("false", ValueSpecification.getValue(result, processorSupport).getName());
    }

    @Test
    public void testOptionThatIsNotSet()
    {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::core::runtime::isOptionSet('TestUnset');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("false", ValueSpecification.getValue(result, processorSupport).getName());
    }

    protected static RuntimeOptions getOptions()
    {
        ObjectBooleanMap<String> testOptions = ObjectBooleanMaps.mutable.<String>empty()
                .withKeyValue("TestSetOn", true)
                .withKeyValue("TestSetOff", false);
        return name -> testOptions.getIfAbsent(name, false);
    }
}
