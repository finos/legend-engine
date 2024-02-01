// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.shared.core.extension.LegendExtension;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.lang.reflect.Method;
import java.util.List;

public interface LegendPureCoreExtension extends LegendExtension
{
    default String functionFile()
    {
        return null;
    }

    default String functionSignature()
    {
        return null;
    }

    default RichIterable<? extends Root_meta_pure_extension_Extension> extraPureCoreExtensions(ExecutionSupport es)
    {
        if (functionFile() == null && functionSignature() == null)
        {
            throw new RuntimeException("This block can only be used if functionFile and functionSignature are specified!");
        }
        try
        {
            Class<?> cl = ((CompiledExecutionSupport) es).getClassLoader().loadClass("org.finos.legend.pure.generated." + functionFile().replace("/", "_").replace(".pure", ""));
            Method m = cl.getMethod("Root_" + functionSignature().replace("::", "_"), ExecutionSupport.class);
            Object res = m.invoke(null, es);
            return (res instanceof List) ?
                    (RichIterable<? extends Root_meta_pure_extension_Extension>) res :
                    org.eclipse.collections.impl.factory.Lists.mutable.with((Root_meta_pure_extension_Extension) m.invoke(null, es));
        }
        catch (Exception e)
        {
            // Silent at build time
            return Lists.mutable.empty();
        }
    }
}
