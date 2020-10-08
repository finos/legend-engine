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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PureModelContextData extends PureModelContext
{
    public Protocol serializer;
    public PureModelContextPointer origin;
    public Domain domain;
    public List<PackageableElement> sectionIndices = Lists.mutable.empty();
    public List<PackageableElement> stores = Lists.mutable.empty();
    public List<PackageableElement> mappings = Lists.mutable.empty();
    public List<PackageableElement> services = Lists.mutable.empty();
    public List<PackageableElement> cacheables = Lists.mutable.empty();
    public List<PackageableElement> caches = Lists.mutable.empty();
    public List<PackageableElement> pipelines = Lists.mutable.empty();
    public List<PackageableElement> flattenSpecifications = Lists.mutable.empty();
    public List<PackageableElement> diagrams = Lists.mutable.empty();
    public List<PackageableElement> dataStoreSpecifications = Lists.mutable.empty();
    public List<PackageableElement> texts = Lists.mutable.empty();
    public List<PackageableElement> runtimes = Lists.mutable.empty();
    public List<PackageableElement> connections = Lists.mutable.empty();
    public List<PackageableElement> fileGenerations = Lists.mutable.empty();
    public List<PackageableElement> generationSpecifications = Lists.mutable.empty();
    public List<PackageableElement> serializableModelSpecifications = Lists.mutable.empty();

    // ------------------------------------------------------------------------------------------------
    // Since we are modularizing PURE protocol, adding a new space for each packageable element types
    // is really not that beneficial anymore, make sure to discuss carefully with the team about this
    // or to use a common space `elements` to store.
    //-------------------------------------------------------------------------------------------------

    public PureModelContextData shallowCopy()
    {
        PureModelContextData res = new PureModelContextData();
        res.serializer = this.serializer;
        res.origin = this.origin;
        res.domain = this.domain;
        res.sectionIndices = sort(this.sectionIndices);
        res.stores = sort(this.stores);
        res.mappings = sort(this.mappings);
        res.services = sort(this.services);
        res.cacheables = sort(this.cacheables);
        res.caches = sort(this.caches);
        res.pipelines = sort(this.pipelines);
        res.flattenSpecifications = sort(this.flattenSpecifications);
        res.dataStoreSpecifications = sort(this.dataStoreSpecifications);
        res.diagrams = sort(this.diagrams);
        res.texts = sort(this.texts);
        res.runtimes = sort(this.runtimes);
        res.connections = sort(this.connections);
        res.fileGenerations = sort(this.fileGenerations);
        res.generationSpecifications = sort(this.generationSpecifications);
        res.serializableModelSpecifications = sort(this.serializableModelSpecifications);
        return res;
    }

    @JsonIgnore
    public Stream<PackageableElement> streamAllElements()
    {
        // NOTE: right now this does not include `sectionIndex` since those are not considered as standard element models
        return Stream.of(
                this.domain == null ? null : this.domain.profiles,
                this.domain == null ? null : this.domain.enums,
                this.domain == null ? null : this.domain.classes,
                this.domain == null ? null : this.domain.associations,
                this.domain == null ? null : this.domain.functions,
                this.domain == null ? null : this.domain.measures,
                this.stores,
                this.mappings,
                this.diagrams,
                this.flattenSpecifications,
                this.dataStoreSpecifications,
                this.texts,
                this.services,
                this.cacheables,
                this.caches,
                this.pipelines,
                this.runtimes,
                this.connections,
                this.fileGenerations,
                this.generationSpecifications,
                this.serializableModelSpecifications
        ).filter(Objects::nonNull).flatMap(Collection::stream);
    }

    @JsonIgnore
    public List<PackageableElement> getAllElements()
    {
        return this.streamAllElements().collect(Collectors.toList());
    }

    private static <T extends PackageableElement> List<T> sort(List<T> elements)
    {
        elements.sort(Comparator.comparing(element -> (element._package + "::" + element.name)));
        return elements;
    }

    @JsonIgnore
    public boolean isUnit()
    {
        long numberOfNonDomainElements = Stream.of(
                this.stores,
                this.mappings,
                this.diagrams,
                this.flattenSpecifications,
                this.dataStoreSpecifications,
                this.texts,
                this.services,
                this.cacheables,
                this.caches,
                this.pipelines,
                this.runtimes,
                this.connections,
                this.fileGenerations,
                this.generationSpecifications,
                this.serializableModelSpecifications
        ).filter(element -> element != null && !element.isEmpty()).map(List::size).reduce(0, Integer::sum);
        return (this.domain != null && !this.domain.isEmpty() && numberOfNonDomainElements == 0) || ((this.domain == null || this.domain.isEmpty()) && numberOfNonDomainElements == 1);
    }

    public PureModelContextData domainPart()
    {
        PureModelContextData data = new PureModelContextData();
        data.domain = this.domain;
        data.serializer = this.serializer;
        return data;
    }

    public MutableList<PureModelContextData> storeParts()
    {
        return ListIterate.collect(this.stores, store ->
                {
                    PureModelContextData data = new PureModelContextData();
                    data.stores = Lists.mutable.with(store);
                    data.serializer = this.serializer;
                    return data;
                }
        );
    }

    public MutableList<PureModelContextData> mappingParts()
    {
        return ListIterate.collect(this.mappings, mapping ->
                {
                    PureModelContextData data = new PureModelContextData();
                    data.mappings = Lists.mutable.with(mapping);
                    data.serializer = this.serializer;
                    return data;
                }
        );
    }

    public MutableList<PureModelContextData> dataStoreSpecParts()
    {
        return ListIterate.collect(this.dataStoreSpecifications, dataStoreSpecification ->
                {
                    PureModelContextData data = new PureModelContextData();
                    data.dataStoreSpecifications = Lists.mutable.with(dataStoreSpecification);
                    data.serializer = this.serializer;
                    return data;
                }
        );
    }

    public MutableList<PureModelContextData> serviceParts()
    {
        return ListIterate.collect(this.services, service ->
                {
                    PureModelContextData data = new PureModelContextData();
                    data.services = Lists.mutable.with(service);
                    data.serializer = this.serializer;
                    return data;
                }
        );
    }

    @JsonIgnore
    public PureModelContextData combine(PureModelContextData data)
    {
        PureModelContextData result = new PureModelContextData();
        result.domain = this.domain == null ? data.domain : this.domain.combine(data.domain);
        result.sectionIndices = uniqueUnion(this.sectionIndices, data.sectionIndices);
        result.stores = uniqueUnion(this.stores, data.stores);
        result.mappings = uniqueUnion(this.mappings, data.mappings);
        result.services = uniqueUnion(this.services, data.services);
        result.cacheables = uniqueUnion(this.cacheables, data.cacheables);
        result.caches = uniqueUnion(this.caches, data.caches);
        result.pipelines = uniqueUnion(this.pipelines, data.pipelines);
        result.flattenSpecifications = uniqueUnion(this.flattenSpecifications, data.flattenSpecifications);
        result.dataStoreSpecifications = uniqueUnion(this.dataStoreSpecifications, data.dataStoreSpecifications);
        result.diagrams = uniqueUnion(this.diagrams, data.diagrams);
        result.texts = uniqueUnion(this.texts, data.texts);
        result.runtimes = uniqueUnion(this.runtimes, data.runtimes);
        result.connections = uniqueUnion(this.connections, data.connections);
        result.fileGenerations = uniqueUnion(this.fileGenerations, data.fileGenerations);
        result.generationSpecifications = uniqueUnion(this.generationSpecifications, data.generationSpecifications);
        result.serializableModelSpecifications = uniqueUnion(this.serializableModelSpecifications, data.serializableModelSpecifications);
        result.origin = this.origin == null ? data.origin : this.origin.combine(data.origin);
        result.serializer = this.serializer == null ? data.serializer : this.serializer;
        return result;
    }

    @JsonIgnore
    public static <T extends PackageableElement> List<T> uniqueUnion(List<T> s1, List<T> s2)
    {
        MutableSet<T> set = UnifiedSetWithHashingStrategy.newSet(hash);
        set.addAll(s1);
        set.addAll(s2);
        return sort(set.toList());
    }

    public static List<PureModelContextData> partition(PureModelContextData inputModel, int parts)
    {
        List<PureModelContextData> result = Lists.mutable.empty();
        for (int i = 0; i < parts; i++)
        {
            PureModelContextData newModel = new PureModelContextData();
            newModel.serializer = inputModel.serializer;
            newModel.origin = inputModel.origin;
            result.add(newModel);
        }
        List<Domain> partitionedDomain = Domain.partition(inputModel.domain, parts);
        Lists.mutable.withAll(partitionedDomain).forEach(c -> result.get(partitionedDomain.indexOf(c) % parts).domain = c);
        Lists.mutable.withAll(inputModel.sectionIndices).forEach(c -> result.get(inputModel.sectionIndices.indexOf(c) % parts).sectionIndices.add(c));
        Lists.mutable.withAll(inputModel.stores).forEach(c -> result.get(inputModel.stores.indexOf(c) % parts).stores.add(c));
        Lists.mutable.withAll(inputModel.mappings).forEach(c -> result.get(inputModel.mappings.indexOf(c) % parts).mappings.add(c));
        Lists.mutable.withAll(inputModel.services).forEach(c -> result.get(inputModel.services.indexOf(c) % parts).services.add(c));
        Lists.mutable.withAll(inputModel.cacheables).forEach(c -> result.get(inputModel.cacheables.indexOf(c) % parts).cacheables.add(c));
        Lists.mutable.withAll(inputModel.caches).forEach(c -> result.get(inputModel.caches.indexOf(c) % parts).caches.add(c));
        Lists.mutable.withAll(inputModel.pipelines).forEach(c -> result.get(inputModel.pipelines.indexOf(c) % parts).pipelines.add(c));
        Lists.mutable.withAll(inputModel.flattenSpecifications).forEach(c -> result.get(inputModel.flattenSpecifications.indexOf(c) % parts).flattenSpecifications.add(c));
        Lists.mutable.withAll(inputModel.diagrams).forEach(c -> result.get(inputModel.diagrams.indexOf(c) % parts).diagrams.add(c));
        Lists.mutable.withAll(inputModel.dataStoreSpecifications).forEach(c -> result.get(inputModel.dataStoreSpecifications.indexOf(c) % parts).dataStoreSpecifications.add(c));
        Lists.mutable.withAll(inputModel.texts).forEach(c -> result.get(inputModel.texts.indexOf(c) % parts).texts.add(c));
        Lists.mutable.withAll(inputModel.runtimes).forEach(c -> result.get(inputModel.runtimes.indexOf(c) % parts).runtimes.add(c));
        Lists.mutable.withAll(inputModel.connections).forEach(c -> result.get(inputModel.connections.indexOf(c) % parts).connections.add(c));
        Lists.mutable.withAll(inputModel.fileGenerations).forEach(c -> result.get(inputModel.fileGenerations.indexOf(c) % parts).fileGenerations.add(c));
        Lists.mutable.withAll(inputModel.generationSpecifications).forEach(c -> result.get(inputModel.generationSpecifications.indexOf(c) % parts).generationSpecifications.add(c));
        Lists.mutable.withAll(inputModel.serializableModelSpecifications).forEach(c -> result.get(inputModel.serializableModelSpecifications.indexOf(c) % parts).serializableModelSpecifications.add(c));
        return result;
    }


    @JsonIgnore
    private static HashingStrategy<PackageableElement> hash = new HashingStrategy<PackageableElement>()
    {
        @Override
        public int computeHashCode(PackageableElement element)
        {
            return (element._package + "::" + element.name).hashCode();
        }

        @Override
        public boolean equals(PackageableElement el1, PackageableElement el2)
        {
            return el1._package.equals(el2._package) && el1.name.equals(el2.name);
        }
    };
}
