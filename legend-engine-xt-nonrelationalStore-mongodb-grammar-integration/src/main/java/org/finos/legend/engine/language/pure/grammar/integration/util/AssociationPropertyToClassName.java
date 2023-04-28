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

package org.finos.legend.engine.language.pure.grammar.integration.util;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;

public class AssociationPropertyToClassName
{
    private String propertyName;
    private String classFullPath;
    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance propertyInstance;

    public AssociationPropertyToClassName(String propertyName, String classFullPath, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance propertyInstance)
    {
        this.propertyName = propertyName;
        this.classFullPath = classFullPath;
        this.propertyInstance = propertyInstance;
    }

    public String getClassFullPath()
    {
        return this.classFullPath;
    }

    public String getPropertyName()
    {
        return this.propertyName;
    }

    public PropertyInstance getPropertyInstance()
    {
        return this.propertyInstance;
    }
}
