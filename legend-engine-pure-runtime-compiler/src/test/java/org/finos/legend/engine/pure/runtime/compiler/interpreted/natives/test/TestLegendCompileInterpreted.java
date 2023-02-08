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

package org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.pure.runtime.compiler.Tools;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.InterpretedMetadata;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.engine.pure.runtime.compiler.test.LegendCompileTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumerationInstance;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLegendCompileInterpreted extends LegendCompileTest
{
    @BeforeClass
    public static void setUp()
    {
        Pair<FunctionExecution, PureRuntime> res = Tools.setUpInterpreted();
        functionExecution = res.getOne();
        runtime = res.getTwo();
    }

    @Test
    public void testInterpretedMetadataGetEnum()
    {
        InterpretedMetadata interpretedMetadata = new InterpretedMetadata(new LegendCompileMixedProcessorSupport(runtime.getContext(), runtime.getModelRepository(), functionExecution.getProcessorSupport()));
        CoreInstance enumValue = interpretedMetadata.getEnum("meta::pure::functions::hash::HashType", "MD5");
        Assert.assertTrue(enumValue.getClassifier() instanceof EnumerationInstance);
        Assert.assertEquals("MD5", Instance.getValueForMetaPropertyToOneResolved(enumValue, "name", functionExecution.getProcessorSupport()).getName());
    }
}
