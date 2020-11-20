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

package org.finos.legend.engine.protocol.pure.v1.model.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Domain;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PureModelContextData extends PureModelContext
{
    public final Protocol serializer;
    public final PureModelContextPointer origin;
    private final MutableList<PackageableElement> elements;

    private PureModelContextData(Protocol serializer, PureModelContextPointer origin, MutableList<PackageableElement> elements)
    {
        this.serializer = serializer;
        this.origin = origin;
        this.elements = elements;
    }

    public Protocol getSerializer()
    {
        return this.serializer;
    }

    public PureModelContextPointer getOrigin()
    {
        return this.origin;
    }

    public List<PackageableElement> getElements()
    {
        return this.elements.asUnmodifiable();
    }

    @JsonIgnore
    public <T extends PackageableElement> List<T> getElementsOfType(java.lang.Class<T> cls)
    {
        return this.elements.selectInstancesOf(cls);
    }

    public static PureModelContextData newPureModelContextData(Protocol serializer, PureModelContextPointer origin, Iterable<? extends PackageableElement> elements)
    {
        return new PureModelContextData(serializer, origin, Lists.mutable.ofAll(elements));
    }

    @JsonCreator
    public static PureModelContextData newPureModelContextData(
            @JsonProperty("serializer") Protocol serializer,
            @JsonProperty("origin") PureModelContextPointer origin,
            @JsonProperty("elements") Iterable<? extends PackageableElement> elements,
            @Deprecated @JsonProperty("domain") Domain domain,
            @Deprecated @JsonProperty("sectionIndices") Collection<? extends PackageableElement> sectionIndices,
            @Deprecated @JsonProperty("stores") Collection<? extends PackageableElement> stores,
            @Deprecated @JsonProperty("mappings") Collection<? extends PackageableElement> mappings,
            @Deprecated @JsonProperty("services") Collection<? extends PackageableElement> services,
            @Deprecated @JsonProperty("cacheables") Collection<? extends PackageableElement> cacheables,
            @Deprecated @JsonProperty("caches") Collection<? extends PackageableElement> caches,
            @Deprecated @JsonProperty("pipelines") Collection<? extends PackageableElement> pipelines,
            @Deprecated @JsonProperty("flattenSpecifications") Collection<? extends PackageableElement> flattenSpecifications,
            @Deprecated @JsonProperty("diagrams") Collection<? extends PackageableElement> diagrams,
            @Deprecated @JsonProperty("dataStoreSpecifications") Collection<? extends PackageableElement> dataStoreSpecifications,
            @Deprecated @JsonProperty("texts") Collection<? extends PackageableElement> texts,
            @Deprecated @JsonProperty("runtimes") Collection<? extends PackageableElement> runtimes,
            @Deprecated @JsonProperty("connections") Collection<? extends PackageableElement> connections,
            @Deprecated @JsonProperty("fileGenerations") Collection<? extends PackageableElement> fileGenerations,
            @Deprecated @JsonProperty("generationSpecifications") Collection<? extends PackageableElement> generationSpecifications,
            @Deprecated @JsonProperty("serializableModelSpecifications") Collection<? extends PackageableElement> serializableModelSpecifications)
    {
        MutableList<PackageableElement> allElements = (elements == null) ? Lists.mutable.empty() : Lists.mutable.withAll(elements);
        if (domain != null)
        {
            allElements.addAll(domain.classes);
            allElements.addAll(domain.associations);
            allElements.addAll(domain.enums);
            allElements.addAll(domain.profiles);
            allElements.addAll(domain.functions);
            allElements.addAll(domain.measures);
        }
        Optional.ofNullable(sectionIndices).ifPresent(allElements::addAll);
        Optional.ofNullable(stores).ifPresent(allElements::addAll);
        Optional.ofNullable(mappings).ifPresent(allElements::addAll);
        Optional.ofNullable(services).ifPresent(allElements::addAll);
        Optional.ofNullable(cacheables).ifPresent(allElements::addAll);
        Optional.ofNullable(caches).ifPresent(allElements::addAll);
        Optional.ofNullable(pipelines).ifPresent(allElements::addAll);
        Optional.ofNullable(flattenSpecifications).ifPresent(allElements::addAll);
        Optional.ofNullable(diagrams).ifPresent(allElements::addAll);
        Optional.ofNullable(dataStoreSpecifications).ifPresent(allElements::addAll);
        Optional.ofNullable(texts).ifPresent(allElements::addAll);
        Optional.ofNullable(runtimes).ifPresent(allElements::addAll);
        Optional.ofNullable(connections).ifPresent(allElements::addAll);
        Optional.ofNullable(fileGenerations).ifPresent(allElements::addAll);
        Optional.ofNullable(generationSpecifications).ifPresent(allElements::addAll);
        Optional.ofNullable(serializableModelSpecifications).ifPresent(allElements::addAll);
        return newPureModelContextData(serializer, origin, allElements);
    }

    public PureModelContextData shallowCopy()
    {
        return new PureModelContextData(this.serializer, this.origin, this.elements.toSortedList(PureModelContextData::compareByPackageAndName));
    }

    @JsonIgnore
    @Deprecated
    public Stream<PackageableElement> streamAllElements()
    {
        // NOTE: right now this does not include `sectionIndex` since those are not considered as standard element models
        return this.elements.stream().filter(e -> !(e instanceof SectionIndex));
    }

    @JsonIgnore
    @Deprecated
    public List<PackageableElement> getAllElements()
    {
        return streamAllElements().collect(Collectors.toList());
    }

    @JsonIgnore
    public PureModelContextData combine(PureModelContextData data)
    {
        return combine(this, data);
    }

    public static PureModelContextData combine(PureModelContextData data1, PureModelContextData data2)
    {
        return combine(data1, data2, (PureModelContextData[]) null);
    }

    public static PureModelContextData combine(PureModelContextData data1, PureModelContextData data2, PureModelContextData... moreData)
    {
        Builder builder = new Builder().withPureModelContextData(data1).withPureModelContextData(data2);
        if (moreData != null)
        {
            ArrayIterate.forEach(moreData, builder::addPureModelContextData);
        }
        return builder.distinct().sorted().build();
    }

    @JsonIgnore
    public static List<PureModelContextData> partition(PureModelContextData inputModel, int parts)
    {
        List<PureModelContextData> result = Lists.mutable.ofInitialCapacity(parts);
        int partitionSize = (inputModel.elements.size() / parts) + 1;
        for (int i = 0; i < parts; i++)
        {
            result.add(new PureModelContextData(inputModel.serializer, inputModel.origin, Lists.mutable.ofInitialCapacity(partitionSize)));
        }
        inputModel.elements.forEachWithIndex((e, i) -> result.get(i % parts).elements.add(e));
        return result;
    }

    @JsonIgnore
    private static final HashingStrategy<PackageableElement> ELEMENT_PATH_HASH = new HashingStrategy<PackageableElement>()
    {
        @Override
        public int computeHashCode(PackageableElement element)
        {
            int hashCode = Objects.hashCode(element.name);
            if (!isPackageEmpty(element._package))
            {
                hashCode += 31 * element._package.hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(PackageableElement el1, PackageableElement el2)
        {
            return packagesEqual(el1._package, el2._package) && Objects.equals(el1.name, el2.name);
        }
    };

    private static int compareByPackageAndName(PackageableElement element1, PackageableElement element2)
    {
        return comparePackageAndName(element1._package, element1.name, element2._package, element2.name);
    }

    private static int comparePackageAndName(String _package1, String name1, String _package2, String name2)
    {
        int cmp = comparePackages(_package1, _package2);
        return (cmp == 0) ? name1.compareTo(name2) : cmp;
    }

    private static boolean packagesEqual(String _package1, String _package2)
    {
        return isPackageEmpty(_package1) ? isPackageEmpty(_package2) : _package1.equals(_package2);
    }

    private static int comparePackages(String _package1, String _package2)
    {
        return isPackageEmpty(_package1) ? (isPackageEmpty(_package2) ? 0 : -1) : (isPackageEmpty(_package2) ? 1 : _package1.compareTo(_package2));
    }

    private static boolean isPackageEmpty(String _package)
    {
        return (_package == null) || _package.isEmpty();
    }

    public static class Builder
    {
        private Protocol serializer;
        private PureModelContextPointer origin;
        private final MutableList<PackageableElement> elements = Lists.mutable.empty();

        public Builder()
        {
        }

        public void setSerializer(Protocol serializer)
        {
            this.serializer = serializer;
        }

        public Builder withSerializer(Protocol serializer)
        {
            setSerializer(serializer);
            return this;
        }

        public void setOrigin(PureModelContextPointer origin)
        {
            this.origin = origin;
        }

        public Builder withOrigin(PureModelContextPointer origin)
        {
            setOrigin(origin);
            return this;
        }

        public boolean addElement(PackageableElement element)
        {
            return this.elements.add(element);
        }

        public boolean addElements(Iterable<? extends PackageableElement> elements)
        {
            return this.elements.addAllIterable(elements);
        }

        public Builder withElement(PackageableElement element)
        {
            addElement(element);
            return this;
        }

        public Builder withElements(Iterable<? extends PackageableElement> elements)
        {
            addElements(elements);
            return this;
        }

        public void addPureModelContextData(PureModelContextData pureModelContextData)
        {
            if (this.serializer == null)
            {
                this.serializer = pureModelContextData.serializer;
            }
            if (this.origin == null)
            {
                this.origin = pureModelContextData.origin;
            }
            this.elements.addAll(pureModelContextData.elements);
        }

        public Builder withPureModelContextData(PureModelContextData pureModelContextData)
        {
            addPureModelContextData(pureModelContextData);
            return this;
        }

        public boolean removeDuplicates()
        {
            MutableSet<PackageableElement> set = UnifiedSetWithHashingStrategy.newSet(ELEMENT_PATH_HASH, this.elements.size());
            return this.elements.removeIf(e -> !set.add(e));
        }

        public Builder distinct()
        {
            removeDuplicates();
            return this;
        }

        public void sort()
        {
            this.elements.sort(PureModelContextData::compareByPackageAndName);
        }

        public Builder sorted()
        {
            sort();
            return this;
        }

        public PureModelContextData build()
        {
            return new PureModelContextData(this.serializer, this.origin, this.elements.toList());
        }
    }
}
