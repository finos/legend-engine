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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyConversion;

class MetamodelSerializationOverrides
{
    private static final ImmutableMap<String, ImmutableSet<String>> CLASS_PROPERTIES = Maps.mutable.<String, ImmutableSet<String>>empty()
            .withKeyValue(M3Paths.Property, Sets.immutable.of(M3Properties.name, M3Properties.aggregation, M3Properties.multiplicity, M3Properties.genericType, M3Properties.stereotypes, M3Properties.taggedValues))
            .withKeyValue(M3Paths.GenericType, Sets.immutable.of(M3Properties.typeParameters, M3Properties.rawType, M3Properties.typeArguments))
            .withKeyValue(M3Paths.Stereotype, Sets.immutable.of(M3Properties.profile, M3Properties.value))
            .withKeyValue(M3Paths.TaggedValue, Sets.immutable.of(M3Properties.tag, M3Properties.value))
            .withKeyValue(M3Paths.TypeParameter, Sets.immutable.of(M3Properties.name))
            .withKeyValue(M3Paths.Generalization, Sets.immutable.of(M3Properties.general))
            .withKeyValue(M3Paths.QualifiedProperty, Sets.immutable.of(M3Properties.name, M3Properties.multiplicity, M3Properties.parameters, M3Properties.genericType, M3Properties.stereotypes, M3Properties.taggedValues))
            .withKeyValue(M3Paths.Tag, Sets.immutable.of(M3Properties.profile, M3Properties.value))
            .withKeyValue(M3Paths.Association, Sets.immutable.of(M3Properties._package, M3Properties.name, M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.stereotypes, M3Properties.taggedValues))
            .withKeyValue(M3Paths.FunctionDefinition, Sets.immutable.of(M3Properties.name))
            .withKeyValue(M3Paths.Class, Sets.immutable.of(M3Properties.name, M3Properties.typeParameters, M3Properties.generalizations, M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.stereotypes, M3Properties.taggedValues))
            .withKeyValue(M3Paths.Enum, Sets.immutable.of(M3Properties.name))
            .withKeyValue(M3Paths.Enumeration, Sets.immutable.of(M3Properties.name, M3Properties._package, M3Properties.values))
            .withKeyValue(M3Paths.VariableExpression, Sets.immutable.of(M3Properties.name, M3Properties.genericType, M3Properties.multiplicity))
            .toImmutable();

    private MetamodelSerializationOverrides()
    {
    }

    static Predicate<PropertyConversion<?, ?>> computeMetamodelPropertyFilter(String className)
    {
        ImmutableSet<String> filteredProperties = CLASS_PROPERTIES.get(className);
        return (filteredProperties == null) ? pc -> true : pc -> filteredProperties.contains(pc.getName());
    }

    static boolean applyMetamodelPropertyFilter(Class<?> classifier)
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
