package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Describes a property that can be set on a section of a <tt>FlatData</tt> store.
 * Properties are of a type:
 *
 * * <b>String</b> allows a string value to be defined
 *
 * * <b>Integer</b> allows an integer value to be defined.  Integer here indicates a PURE integer
 *   thus a long value in Java.
 *
 *   <b>Boolean</b> allows a boolean value to be defined.  A boolean property is either set on a section
 *   (true) or absent (false).
 *
 *   <b>Group</b> defines a set of properties (its children).
 *
 *   <b>ExclusiveGroup</b> defines a set of properties (its children) of which at most one may be
 *   selected.
 *
 *   Note that names in <tt>PropertyDescription</tt>s are contextual to their parents whereas in sections of
 *   a <tt>FlatData</tt> store they are qualified.  That is say we define the following:
 *   the following properties:
 *
 *   <pre>
 *       new PropertyDescription.Builder()
 *               .requiredPropertyGroup("range", b->b.
 *                       .requiredIntegerProperty("start")
 *                       .requiredIntegerProperty("end")
 *               )
 *               .build();
 *   </pre>
 *
 *   The properties defined in the <tt>FlatData</tt> section will be:
 *
 *   <pre>
 *       FlatData test::fd
 *       {
 *           section default: DriverId
 *           {
 *               range.start: 5;
 *               range.end: 10;
 *           }
 *       }
 *   </pre>
 */
public final class PropertyDescription
{
    private final String name;
    private final PropertyType type;
    private final long minOccurrences;
    private final Long maxOccurrences;
    private final List<PropertyDescription> children;

    private PropertyDescription(String name, PropertyType type, long minOccurrences, Long maxOccurrences, List<PropertyDescription> children)
    {
        if (!Objects.requireNonNull(name).matches("[A-Za-z0-9_][A-Za-z0-9_$]*"))
        {
            throw new IllegalArgumentException("Name must be a valid identifier");
        }
        if ((type == PropertyType.Group || type == PropertyType.ExclusiveGroup) && (maxOccurrences == null || maxOccurrences > 1L))
        {
            throw new IllegalArgumentException("Groups may not have multiplicity > 1");
        }
        if (maxOccurrences != null && maxOccurrences < minOccurrences)
        {
            throw new IllegalArgumentException("Maximum multiplicity must equal or exceed minimum");
        }

        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.minOccurrences = minOccurrences;
        this.maxOccurrences = maxOccurrences;
        this.children = Collections.unmodifiableList(children);
    }

    public String getName()
    {
        return name;
    }

    public PropertyType getType()
    {
        return type;
    }

    public long getMinOccurrences()
    {
        return minOccurrences;
    }

    public Long getMaxOccurrences()
    {
        return maxOccurrences;
    }

    public List<PropertyDescription> getChildren()
    {
        return children;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof PropertyDescription && name.equals(((PropertyDescription) o).name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return "PropertyDescription{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", minOccurrences=" + minOccurrences +
                ", maxOccurrences=" + maxOccurrences +
                '}';
    }

    /**
     * Used to create lists of {@link PropertyDescription}s.
     */
    public static class Builder
    {
        private final List<PropertyDescription> descriptions;

        public Builder()
        {
            this.descriptions = new ArrayList<>();
        }

        public Builder(List<PropertyDescription> descriptions)
        {
            this.descriptions = new ArrayList<>(descriptions);
        }

        public Builder booleanProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.Boolean, 0L, 1L, Collections.emptyList()));
            return this;
        }

        public Builder optionalStringProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.String, 0L, 1L, Collections.emptyList()));
            return this;
        }

        public Builder requiredStringProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.String, 1L, 1L, Collections.emptyList()));
            return this;
        }

        public Builder optionalRepeatableStringProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.String, 0L, null, Collections.emptyList()));
            return this;
        }

        public Builder requiredRepeatableStringProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.String, 1L, null, Collections.emptyList()));
            return this;
        }

        public Builder optionalIntegerProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.Integer, 0L, 1L, Collections.emptyList()));
            return this;
        }

        public Builder requiredIntegerProperty(String name)
        {
            descriptions.add(new PropertyDescription(name, PropertyType.Integer, 1L, 1L, Collections.emptyList()));
            return this;
        }

        public Builder optionalGroup(String name, Consumer<Builder> groupContent)
        {
            return aGroup(name, PropertyType.Group, false, groupContent);
        }

        public Builder requiredGroup(String name, Consumer<Builder> groupContent)
        {
            return aGroup(name, PropertyType.Group, true, groupContent);
        }

        public Builder optionalExclusiveGroup(String name, Consumer<Builder> groupContent)
        {
            return aGroup(name, PropertyType.ExclusiveGroup, false, groupContent);
        }

        public Builder requiredExclusiveGroup(String name, Consumer<Builder> groupContent)
        {
            return aGroup(name, PropertyType.ExclusiveGroup, true, groupContent);
        }

        private Builder aGroup(String name, PropertyType type, boolean required, Consumer<Builder> groupContent)
        {
            Builder subBuilder = new Builder();
            groupContent.accept(subBuilder);
            List<PropertyDescription> children = subBuilder.build();
            descriptions.add(new PropertyDescription(name, type, required ? 1L : 0L, 1L, children));
            return this;
        }

        public List<PropertyDescription> build()
        {
            return Collections.unmodifiableList(descriptions);
        }
    }
}
