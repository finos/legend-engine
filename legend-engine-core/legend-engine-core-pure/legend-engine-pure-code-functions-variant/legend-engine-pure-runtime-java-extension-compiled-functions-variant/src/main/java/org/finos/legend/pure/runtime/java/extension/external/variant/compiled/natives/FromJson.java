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
//

package org.finos.legend.pure.runtime.java.extension.external.variant.compiled.natives;

import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;

public class FromJson extends AbstractNativeFunctionGeneric
{
    public FromJson()
    {
        super(FromJson.class.getCanonicalName() + ".fromJson", new Class[]{String.class, ExecutionSupport.class}, false, true, false, "fromJson_String_1__Variant_1_");
    }

    public static VariantInstanceImpl fromJson(String json, ExecutionSupport es)
    {
        return VariantInstanceImpl.newVariant(json, ((CompiledExecutionSupport) es).getProcessorSupport());
    }
}
