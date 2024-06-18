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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyConversion;

import java.util.HashMap;
import java.util.Map;

class MetamodelSerializationOverrides
{
    private static final Map<String, ImmutableList<String>> CLASS_PROPERTIES;

    static
    {
        CLASS_PROPERTIES = new HashMap<>();
        CLASS_PROPERTIES.put(M3Paths.Property, Lists.immutable.of(M3Properties.name, M3Properties.aggregation, M3Properties.multiplicity, M3Properties.genericType, M3Properties.stereotypes, M3Properties.taggedValues));
        CLASS_PROPERTIES.put(M3Paths.GenericType, Lists.immutable.of(M3Properties.typeParameters, M3Properties.rawType, M3Properties.typeArguments));
        CLASS_PROPERTIES.put(M3Paths.Stereotype, Lists.immutable.of(M3Properties.profile, M3Properties.value));
        CLASS_PROPERTIES.put(M3Paths.TaggedValue, Lists.immutable.of(M3Properties.tag, M3Properties.value));
        CLASS_PROPERTIES.put(M3Paths.TypeParameter, Lists.immutable.of(M3Properties.name));
        CLASS_PROPERTIES.put(M3Paths.Generalization, Lists.immutable.of(M3Properties.general));
        CLASS_PROPERTIES.put(M3Paths.QualifiedProperty, Lists.immutable.of(M3Properties.name, M3Properties.multiplicity, M3Properties.parameters, M3Properties.genericType, M3Properties.stereotypes, M3Properties.taggedValues));
        CLASS_PROPERTIES.put(M3Paths.Tag, Lists.immutable.of(M3Properties.profile, M3Properties.value));
        CLASS_PROPERTIES.put(M3Paths.Association, Lists.immutable.of(M3Properties._package, M3Properties.name, M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.stereotypes, M3Properties.taggedValues));
        CLASS_PROPERTIES.put(M3Paths.FunctionDefinition, Lists.immutable.of(M3Properties.name));
        CLASS_PROPERTIES.put(M3Paths.Class, Lists.immutable.of(M3Properties.name, M3Properties.typeParameters, M3Properties.generalizations, M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.stereotypes, M3Properties.taggedValues));
        CLASS_PROPERTIES.put(M3Paths.Enum, Lists.immutable.of(M3Properties.name));
        CLASS_PROPERTIES.put(M3Paths.Enumeration, Lists.immutable.of(M3Properties.name, M3Properties._package, M3Properties.values));
        CLASS_PROPERTIES.put(M3Paths.VariableExpression, Lists.immutable.of(M3Properties.name, M3Properties.genericType, M3Properties.multiplicity));
    }

    private MetamodelSerializationOverrides()
    {
    }

    static Predicate<PropertyConversion<?, ?>> computeMetamodelPropertyFilter(String className)
    {
        final ImmutableList<String> filteredProperties = CLASS_PROPERTIES.get(className);
        return new Predicate<PropertyConversion<?, ?>>()
        {
            @Override
            public boolean accept(PropertyConversion<?, ?> propertyConversion)
            {
                return filteredProperties != null && filteredProperties.contains(propertyConversion.getName());
            }
        };
    }

    static boolean applyMetamodelPropertyFilter(Class classifier)
    {
        return CLASS_PROPERTIES.containsKey(PackageableElement.getUserPathForPackageableElement(classifier));
    }

    static boolean shouldSerializeAsElementToPath(CoreInstance instance)
    {
        return instance instanceof Package || instance instanceof Multiplicity || instance instanceof Profile;
    }

    private static Object serializeMultiplicityAsNumber(Multiplicity multiplicity)
    {
        return org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(multiplicity, false);
    }

    static Object serializePackageableElement(CoreInstance pureObject, boolean serializeMultiplicityAsNumber)
    {
        return serializeMultiplicityAsNumber && pureObject instanceof Multiplicity ? serializeMultiplicityAsNumber((Multiplicity)pureObject) : PackageableElement.getUserPathForPackageableElement(pureObject);
    }
}
