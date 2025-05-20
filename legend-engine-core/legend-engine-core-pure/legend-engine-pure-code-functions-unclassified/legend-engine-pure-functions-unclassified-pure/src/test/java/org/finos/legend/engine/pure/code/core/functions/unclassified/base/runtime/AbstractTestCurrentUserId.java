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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractTestCurrentUserId extends AbstractPureTestWithCoreCompiled
{
    @Before
    public abstract void setupCurrentUser();

    @Test
    public void testCurrentUserId()
    {
        CoreInstance result = this.execute("test():String[1]");
        Assert.assertEquals(this.getTestUserId(), ValueSpecification.getValue(result, this.processorSupport).getName());
    }

    @Test
    public void testCurrentUserIdEval()
    {
        CoreInstance result = this.execute("test_eval():String[1]");
        Assert.assertEquals(this.getTestUserId(), ValueSpecification.getValue(result, this.processorSupport).getName());
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "fromString.pure",
                "import test::model::*;\n" +
                "\n" +
                "function test():String[1]\n" +
                "{\n" +
                "    meta::core::runtime::currentUserId();\n" +
                "}\n" +
                "\n" +
                "function test_eval():String[1]\n" +
                "{\n" +
                "    meta::core::runtime::currentUserId__String_1_->eval();\n" +
                "}");
    }

    protected String getTestUserId()
    {
        return "someUserId";
    }
}
