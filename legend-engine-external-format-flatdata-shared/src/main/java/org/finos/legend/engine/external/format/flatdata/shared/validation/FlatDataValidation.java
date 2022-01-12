package org.finos.legend.engine.external.format.flatdata.shared.validation;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataGrammarHelper;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RecordTypeMultiplicity;

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
        store.getSections().forEach(s -> defects.addAll(validateSection(store, s)));
        return defects.isEmpty() ? FlatDataValidationResult.valid() : FlatDataValidationResult.invalid(defects);
    }

    private static List<FlatDataDefect> validateSection(FlatData store, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();

        FlatDataDriverDescription description = flatDataDriverDescriptions.stream().filter(d -> d.getId().equals(section.getDriverId())).findFirst().orElse(null);
        if (description == null)
        {
            defects.add(new FlatDataDefect(store, section, "Invalid driver ID '" + section.getDriverId() + "' specified"));
        }
        else
        {
            ListIterate.forEach(description.getSectionProperties(), propertyDesc -> defects.addAll(checkGroupsAndMandatoryProperties(propertyDesc, null, store, section)));

            section.getSectionProperties().stream()
                    .collect(Collectors.groupingBy(FlatDataProperty::getName))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .map(e -> e.getKey())
                    .forEach(name -> defects.add(new FlatDataDefect(store, section, "Duplicate property '" + name + "'")));

            ListIterate.forEach(section.getSectionProperties(), prop ->
            {
                List<PropertyDescription> propertyDescs = description.getSectionProperties();
                PropertyDescription propertyDescription = null;
                for (String name : prop.getName().split("\\."))
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
                    defects.add(new FlatDataDefect(store, section, "Invalid property '" + prop.getName() + "'"));
                }
                else
                {
                    boolean isValidValue = false;
                    int minValues = (int) propertyDescription.getMinOccurrences();
                    int maxValues = propertyDescription.getMaxOccurrences() == null ? Integer.MAX_VALUE : propertyDescription.getMaxOccurrences().intValue();
                    if (propertyDescription.getType() == PropertyType.String)
                    {
                        isValidValue = prop.getValues().size() >= minValues && prop.getValues().size() <= maxValues
                                && prop.getValues().stream().allMatch(String.class::isInstance);
                    }
                    else if (propertyDescription.getType() == PropertyType.Integer)
                    {
                        isValidValue = prop.getValues().size() >= minValues && prop.getValues().size() <= maxValues
                                && prop.getValues().stream().allMatch(Long.class::isInstance);
                    }
                    else if (propertyDescription.getType() == PropertyType.Boolean)
                    {
                        isValidValue = prop.getValues().size() == 1 && prop.getValues().get(0) instanceof Boolean;
                    }

                    if (!isValidValue)
                    {
                        defects.add(new FlatDataDefect(store, section, "Invalid " + prop.getName() + ": " + FlatDataGrammarHelper.convertValues(prop.getValues())));
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
            long matches = section.getSectionProperties().stream().filter(p -> p.getName().startsWith(path)).count();
            if (propertyDesc.getMinOccurrences() != 0 && matches == 0)
            {
                defects.add(new FlatDataDefect(store, section, propertyDesc.getName() + " not specified"));
            }
            else
            {
                if (propertyDesc.getType() == PropertyType.ExclusiveGroup)
                {
                    long nextLevels = section.getSectionProperties().stream()
                            .filter(p -> p.getName().startsWith(path) && p.getName().length() > path.length() + 1)
                            .map(p -> p.getName().substring(path.length() + 1))
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
            if (propertyDesc.getMinOccurrences() > 0 && section.getSectionProperties().stream().noneMatch(p -> p.getName().equals(path)))
            {
                defects.add(new FlatDataDefect(store, section, propertyDesc.getName() + " not specified"));
            }
        }

        return defects;
    }

    private static List<FlatDataDefect> validateRecordType(FlatDataDriverDescription description, FlatData store, FlatDataSection section)
    {
        if (section.getRecordType() == null)
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

            ListIterate.forEach(section.getRecordType().getFields(), field ->
            {
                if (description.isSelfDescribing() && field.getAddress() != null)
                {
                    defects.add(new FlatDataDefect(store, section, "Address should not be specified for " + field.getLabel()));
                }
                else if (!description.isSelfDescribing() && field.getAddress() == null)
                {
                    defects.add(new FlatDataDefect(store, section, "Address must be specified for " + field.getLabel()));
                }
            });

            return defects;
        }
    }
}
