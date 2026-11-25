// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.language.pure.compiler.test.fromGrammar.EmbeddedPureCompilerExtension;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.embedded.extensions.NewValueSpecificationForTest;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;

public class TestClassInstancePrerequisites
{
    private static final String DEEPER_TARGET =
            "Class pack::Deep\n" +
            "{\n" +
            "}\n" +
            "\n" +
            "function pack::target():String[1]\n" +
            "{\n" +
            "  let deep = pack::Deep;\n" +
            "  'target';\n" +
            "}\n";

    @Test
    public void testUnresolvableIslandReferenceIsReportedByTheIsland()
    {
        EngineException e = Assertions.assertThrows(EngineException.class, () -> compile(
                "function pack::reader():String[1]\n" +
                "{\n" +
                "  #Test{body:pack::missing}#\n" +
                "}\n", false));
        Assertions.assertTrue(e.getMessage().contains("Can't find function 'pack::missing'"), e.getMessage());
    }

    @Test
    public void testContextAwareAndContextFreeHooksAreCombinedAndTheirPathsAreRecorded()
    {
        PureModel pureModel = compile(DEEPER_TARGET, false);
        MutableSet<PackageableElementPointer> prerequisites = Sets.mutable.empty();
        ValueSpecificationPrerequisiteElementsPassBuilder builder = new ValueSpecificationPrerequisiteElementsPassBuilder(pureModel.getContext(), prerequisites);

        new ClassInstance("Test", new NewValueSpecificationForTest("body:pack::target"), null).accept(builder);

        Assertions.assertEquals(Sets.mutable.with("pack::target__String_1_", EmbeddedPureCompilerExtension.CONTEXT_FREE_HOOK_MARKER), prerequisites.collect(p -> p.path));
        Assertions.assertEquals(PackageableElementType.FUNCTION, prerequisites.detect(p -> "pack::target__String_1_".equals(p.path)).type);
        Assertions.assertEquals(Sets.mutable.with("pack::target__String_1_", EmbeddedPureCompilerExtension.CONTEXT_FREE_HOOK_MARKER), builder.getIslandPrerequisitePaths());
    }

    private static PureModel compile(String code, boolean parallel)
    {
        if (!parallel)
        {
            return new PureModel(PureGrammarParser.newInstance().parseModel(code), null, DeploymentMode.TEST);
        }
        ForkJoinPool pool = new ForkJoinPool(4);
        try
        {
            return new PureModel(PureGrammarParser.newInstance().parseModel(code), null, Thread.currentThread().getContextClassLoader(), DeploymentMode.TEST, PureModelProcessParameter.newBuilder().withForkJoinPool(pool).build(), null);
        }
        finally
        {
            pool.shutdown();
        }
    }
}
