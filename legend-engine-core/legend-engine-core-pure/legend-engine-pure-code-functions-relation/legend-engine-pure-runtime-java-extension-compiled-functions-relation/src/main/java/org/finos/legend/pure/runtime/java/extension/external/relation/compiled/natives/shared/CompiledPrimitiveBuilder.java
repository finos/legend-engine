// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame;

public class CompiledPrimitiveBuilder implements Frame.PrimitiveBuilder
{
    @Override
    public CoreInstance build(String val)
    {
        return new ValCoreInstance(val, "String");
    }

    @Override
    public CoreInstance build(int val)
    {
        return new ValCoreInstance(String.valueOf(val), "Integer");
    }
}