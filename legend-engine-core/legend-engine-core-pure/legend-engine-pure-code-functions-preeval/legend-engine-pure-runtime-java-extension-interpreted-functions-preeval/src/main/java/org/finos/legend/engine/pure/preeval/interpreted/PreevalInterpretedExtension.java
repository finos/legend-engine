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

package org.finos.legend.engine.pure.preeval.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.preeval.interpreted.natives.PreevalImplementationNative;
import org.finos.legend.engine.pure.preeval.interpreted.natives.PrevalNative;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class PreevalInterpretedExtension extends BaseInterpretedExtension
{
    public PreevalInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("prevalNative_Any_1__Map_1__Map_1__PrevalHooks_1__DebugContext_1__PrevalResult_1_", PrevalNative::new),
                Tuples.pair("preevalImplementation__String_1_", PreevalImplementationNative::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new PreevalInterpretedExtension();
    }
}
