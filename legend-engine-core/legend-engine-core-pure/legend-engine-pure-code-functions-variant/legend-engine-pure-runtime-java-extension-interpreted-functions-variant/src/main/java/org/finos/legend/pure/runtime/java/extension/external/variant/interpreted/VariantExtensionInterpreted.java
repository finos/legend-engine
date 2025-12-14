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

package org.finos.legend.pure.runtime.java.extension.external.variant.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives.To;
import org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives.ToJson;
import org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives.ToMany;
import org.finos.legend.pure.runtime.java.extension.external.variant.interpreted.natives.ToVariant;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;

public class VariantExtensionInterpreted extends BaseInterpretedExtension
{
    public VariantExtensionInterpreted()
    {
        super(Lists.fixedSize.of(
                        Tuples.pair("fromJson_String_1__Variant_1_", FromJson::new),
                        Tuples.pair("toJson_Variant_1__String_1_", ToJson::new),
                        Tuples.pair("to_Variant_$0_1$__T_1__T_$0_1$_", To::new),
                        Tuples.pair("to_Variant_$0_1$__T_1__String_1__Pair_MANY__T_$0_1$_", To::new),
                        Tuples.pair("toMany_Variant_$0_1$__T_1__T_MANY_", ToMany::new),
                        Tuples.pair("toMany_Variant_$0_1$__T_1__String_1__Pair_MANY__T_MANY_", ToMany::new),
                        Tuples.pair("toVariant_Any_MANY__Variant_1_", ToVariant::new)
                )
        );
    }
}
