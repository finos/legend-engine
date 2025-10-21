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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string;

import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class RegexpReplace extends AbstractNativeFunctionGeneric
{
    public RegexpReplace()
    {
        super("FunctionsGen.regexpReplace", new Class[]{String.class, String.class, String.class, Boolean.class, Object.class}, "regexpReplace_String_1__String_1__String_1__Boolean_1__RegexpParameter_$1_MANY$__String_1_");
    }
}
