//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.flatdata.validation;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.driver.spi.PropertyType;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RecordTypeMultiplicity;
import org.finos.legend.engine.external.format.flatdata.grammar.toPure.FlatDataSchemaComposer;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlatDataValidation
{
    private static final List<FlatDataDriverDescription> flatDataDriverDescriptions = FlatDataDriverDescription.loadAll();

    public static FlatDataValidationResult validate(FlatData store)
    {
        List<FlatDataDefect> defects = new ArrayList<>();
        store.sections.forEach(s -> defects.addAll(validateSection(store, s)));
        return defects.isEmpty() ? FlatDataValidationResult.valid() : FlatDataValidationResult.invalid(defects);
    }

    private static List<FlatDataDefect> validateSection(FlatData store, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();

        FlatDataDriverDescription description = flatDataDriverDescriptions.stream().filter(d -> d.getId().equals(section.driverId)).findFirst().orElse(null);
        if (description == null)
        {
            defects.add(new FlatDataDefect(store, section, "Invalid driver ID '" + section.driverId + "' specified"));
        }
        else
        {
            ListIterate.forEach(description.getSectionProperties(), propertyDesc -> defects.addAll(checkGroupsAndMandatoryProperties(propertyDesc, null, store, section)));

            section.sectionProperties.stream()
                    .collect(Collectors.groupingBy(p -> p.name))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey)
                    .forEach(name -> defects.add(new FlatDataDefect(store, section, "Duplicate property '" + name + "'")));

            ListIterate.forEach(section.sectionProperties, prop ->
            {
                List<PropertyDescription> propertyDescs = description.getSectionProperties();
                PropertyDescription propertyDescription = null;
                for (String name : prop.name.split("\\."))
                {
                    propertyDescription = propertyDescs.stream().filter(d -> d.getName().equals(name)).findFirst().orElse(null);
                    if (propertyDescription == null)
                    {
                        break;
                    }
                    propertyDescs = propertyDescription.getChildren();
                }

                if (propertyDescription == null)
                {
                    defects.add(new FlatDataDefect(store, section, "Invalid property '" + prop.name + "'"));
                }
                else
                {
                    boolean isValidValue = false;
                    int minValues = (int) propertyDescription.getMinOccurrences();
                    int maxValues = propertyDescription.getMaxOccurrences() == null ? Integer.MAX_VALUE : propertyDescription.getMaxOccurrences().intValue();
                    if (propertyDescription.getType() == PropertyType.String)
                    {
                        isValidValue = prop.values.size() >= minValues && prop.values.size() <= maxValues
                                && prop.values.stream().allMatch(String.class::isInstance);
                    }
                    else if (propertyDescription.getType() == PropertyType.Integer)
                    {
                        isValidValue = prop.values.size() >= minValues && prop.values.size() <= maxValues
                                && prop.values.stream().allMatch(Long.class::isInstance);
                    }
                    else if (propertyDescription.getType() == PropertyType.Boolean)
                    {
                        isValidValue = prop.values.size() == 1 && prop.values.get(0) instanceof Boolean;
                    }

                    if (!isValidValue)
                    {
                        defects.add(new FlatDataDefect(store, section, "Invalid " + prop.name + ": " + FlatDataSchemaComposer.convertValues(prop.values)));
                    }
                }
            });

            defects.addAll(validateRecordType(description, store, section));
        }

        if (description instanceof FlatDataValidator)
        {
            defects.addAll((((FlatDataValidator) description)).validate(store, section));
        }

        return defects;
    }

    private static List<FlatDataDefect> checkGroupsAndMandatoryProperties(PropertyDescription propertyDesc, String prefix, FlatData store, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();

        String path = prefix == null ? propertyDesc.getName() : prefix + '.' + propertyDesc.getName();

        if (propertyDesc.getType() == PropertyType.Group || propertyDesc.getType() == PropertyType.ExclusiveGroup)
        {
            long matches = section.sectionProperties.stream().filter(p -> p.name.startsWith(path)).count();
            if (propertyDesc.getMinOccurrences() != 0 && matches == 0)
            {
                defects.add(new FlatDataDefect(store, section, propertyDesc.getName() + " not specified"));
            }
            else
            {
                if (propertyDesc.getType() == PropertyType.ExclusiveGroup)
                {
                    long nextLevels = section.sectionProperties.stream()
                            .filter(p -> p.name.startsWith(path) && p.name.length() > path.length() + 1)
                            .map(p -> p.name.substring(path.length() + 1))
                            .map(n -> n.split("\\.")[0])
                            .distinct()
                            .count();

                    if (nextLevels != 1)
                    {
                        defects.add(new FlatDataDefect(store, section, propertyDesc.getName() + " can only have one subvalue"));
                    }
                }

                for (PropertyDescription child : propertyDesc.getChildren())
                {
                    defects.addAll(checkGroupsAndMandatoryProperties(child, path, store, section));
                }
            }
        }
        else
        {
            if (propertyDesc.getMinOccurrences() > 0 && section.sectionProperties.stream().noneMatch(p -> p.name.equals(path)))
            {
                defects.add(new FlatDataDefect(store, section, propertyDesc.getName() + " not specified"));
            }
        }

        return defects;
    }

    private static List<FlatDataDefect> validateRecordType(FlatDataDriverDescription description, FlatData store, FlatDataSection section)
    {
        if (section.recordType == null)
        {
            return description.getRecordTypeMultiplicity() == RecordTypeMultiplicity.MANDATORY
                    ? Collections.singletonList(new FlatDataDefect(store, section, "Must specify a record type"))
                    : Collections.emptyList();
        }
        else
        {
            List<FlatDataDefect> defects = new ArrayList<>();

            if (description.getRecordTypeMultiplicity() == RecordTypeMultiplicity.NONE)
            {
                defects.add(new FlatDataDefect(store, section, "Must not specify a record type"));
            }

            ListIterate.forEach(section.recordType.fields, field ->
            {
                if (description.isSelfDescribing() && field.address != null)
                {
                    defects.add(new FlatDataDefect(store, section, "Address should not be specified for " + field.label));
                }
                else if (!description.isSelfDescribing() && field.address == null)
                {
                    defects.add(new FlatDataDefect(store, section, "Address must be specified for " + field.label));
                }
            });

            return defects;
        }
    }
}