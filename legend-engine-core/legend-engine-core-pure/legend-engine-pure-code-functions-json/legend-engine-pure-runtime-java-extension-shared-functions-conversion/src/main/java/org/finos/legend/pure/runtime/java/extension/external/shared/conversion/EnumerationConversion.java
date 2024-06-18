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

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.HashMap;
import java.util.Map;

public abstract class EnumerationConversion<F, T> implements Conversion<F, T>
{
    protected final Enumeration enumeration;
    protected Map<String, CoreInstance> valueMap;

    public EnumerationConversion(Enumeration enumeration)
    {
        this.enumeration = enumeration;
        this.generateEnumValueMap();
    }

    protected String enumerationName()
    {
        return PackageableElement.getUserPathForPackageableElement(this.enumeration);
    }

    private void generateEnumValueMap()
    {
        this.valueMap = new HashMap<>();
        for (Object eachValue : this.enumeration._values())
        {
            this.valueMap.put(((CoreInstance)eachValue).getName(), (CoreInstance)eachValue);
        }
    }

    @Override
    public String pureTypeAsString()
    {
        return PackageableElement.getUserPathForPackageableElement(this.enumeration);
    }
}
