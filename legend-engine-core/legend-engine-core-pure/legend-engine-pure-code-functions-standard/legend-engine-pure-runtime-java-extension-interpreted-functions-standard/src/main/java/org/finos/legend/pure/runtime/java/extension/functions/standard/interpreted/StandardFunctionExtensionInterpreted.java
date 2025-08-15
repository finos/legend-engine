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

package org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.date.operation.TimeBucket;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.math.trigonometry.CosH;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.math.trigonometry.SinH;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.math.trigonometry.TanH;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitAnd;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitNot;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitOr;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitXor;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitShiftLeft;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.BitShiftRight;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.tbd.HashCode;
import org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted.natives.string.generation.Guid;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;

public class StandardFunctionExtensionInterpreted extends BaseInterpretedExtension
{
    public StandardFunctionExtensionInterpreted()
    {
        super(//Date
                Tuples.pair("timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_", TimeBucket::new),
                Tuples.pair("timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_", TimeBucket::new),

                // Math
                Tuples.pair("cosh_Number_1__Float_1_", CosH::new),
                Tuples.pair("sinh_Number_1__Float_1_", SinH::new),
                Tuples.pair("tanh_Number_1__Float_1_", TanH::new),

                // Bitwise
                Tuples.pair("bitAnd_Integer_1__Integer_1__Integer_1_", BitAnd::new),
                Tuples.pair("bitNot_Integer_1__Integer_1_", BitNot::new),
                Tuples.pair("bitOr_Integer_1__Integer_1__Integer_1_", BitOr::new),
                Tuples.pair("bitXor_Integer_1__Integer_1__Integer_1_", BitXor::new),
                Tuples.pair("bitShiftLeft_Integer_1__Integer_1__Integer_1_", BitShiftLeft::new),
                Tuples.pair("bitShiftRight_Integer_1__Integer_1__Integer_1_", BitShiftRight::new),

                // String
                Tuples.pair("generateGuid__String_1_", Guid::new),

                // Hash
                Tuples.pair("hashCode_Any_MANY__Integer_1_", HashCode::new)
        );
    }

    public static StandardFunctionExtensionInterpreted extension()
    {
        return new StandardFunctionExtensionInterpreted();
    }
}
