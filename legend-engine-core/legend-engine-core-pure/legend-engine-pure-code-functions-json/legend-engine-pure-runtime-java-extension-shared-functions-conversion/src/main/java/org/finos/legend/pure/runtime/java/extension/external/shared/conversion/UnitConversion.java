// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.shared.conversion;

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public abstract class UnitConversion<F, T> implements Conversion<F, T>
{
    protected static final String UNIT_KEY_NAME = "unit";
    protected static final String VALUE_KEY_NAME = "value";
    protected static final String UNIT_ID_KEY_NAME = "unitId";
    protected static final String EXPONENT_VALUE_KEY_NAME = "exponentValue";

    protected final CoreInstance type;

    public UnitConversion(CoreInstance type)
    {
        this.type = type;
    }

    @Override
    public String pureTypeAsString()
    {
        return "Unit";
    }
}
