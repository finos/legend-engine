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

package org.finos.legend.engine.plan.execution.stores.deephaven.test.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.DeephavenCommands;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class DeephavenTestCompiledExtension extends BaseCompiledExtension
{
    public DeephavenTestCompiledExtension()
    {
        super("core_deephaven_execution_test",
                () -> Lists.fixedSize.with(
                        new DeephavenStartCommand(),
                        new DeephavenStopCommand()
                ),
                Lists.fixedSize.with(),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static CompiledExtension extension()
    {
        return new DeephavenTestCompiledExtension();
    }

    private static class DeephavenStartCommand extends AbstractNative
    {
        public DeephavenStartCommand()
        {
            super(DeephavenCommands.START_SERVER_FUNCTION);
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            return DeephavenCommands.class.getCanonicalName() + ".startServer(" + transformedParams.makeString(", ") + ")";
        }
    }

    private static class DeephavenStopCommand extends AbstractNative
    {
        public DeephavenStopCommand()
        {
            super(DeephavenCommands.STOP_SERVER_FUNCTION);
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            return DeephavenCommands.class.getCanonicalName() + ".stopServer(" + transformedParams.makeString(", ") + ")";
        }
    }
}
