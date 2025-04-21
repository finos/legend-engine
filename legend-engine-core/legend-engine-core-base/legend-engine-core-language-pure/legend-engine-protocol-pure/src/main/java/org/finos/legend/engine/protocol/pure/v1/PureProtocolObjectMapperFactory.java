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

package org.finos.legend.engine.protocol.pure.v1;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolConverter;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.path.valuespecification.constant.classInstance.Path;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.dsl.store.valuespecification.constant.classInstance.RelationStoreAccessor;

public class PureProtocolObjectMapperFactory
{
    public static <T extends MapperBuilder<?, ?>> T withPureProtocolExtensions(T mapperBuilder)
    {
        return withPureProtocolExtensions(mapperBuilder, (Predicate<? super String>) null);
    }

    public static <T extends MapperBuilder<?, ?>> T withPureProtocolExtensions(T mapperBuilder, Collection<String> excludedSubTypes)
    {
        return withPureProtocolExtensions(mapperBuilder, (excludedSubTypes == null) ? null : excludedSubTypes::contains);
    }

    public static <T extends MapperBuilder<?, ?>> T withPureProtocolExtensions(T mapperBuilder, Predicate<? super String> excludeSubType)
    {
        return withPureProtocolExtensions(mapperBuilder, MapperBuilder::addModule, MapperBuilder::registerSubtypes, excludeSubType);
    }

    public static <T extends ObjectMapper> T withPureProtocolExtensions(T objectMapper)
    {
        return withPureProtocolExtensions(objectMapper, (Predicate<? super String>) null);
    }

    public static <T extends ObjectMapper> T withPureProtocolExtensions(T objectMapper, Collection<String> excludedSubTypes)
    {
        return withPureProtocolExtensions(objectMapper, (excludedSubTypes == null) ? null : excludedSubTypes::contains);
    }

    public static <T extends ObjectMapper> T withPureProtocolExtensions(T objectMapper, Predicate<? super String> excludeSubType)
    {
        return withPureProtocolExtensions(objectMapper, ObjectMapper::registerModule, ObjectMapper::registerSubtypes, excludeSubType);
    }

    public static Map<String, Class<?>> getClassInstanceTypeMappings()
    {
        Map<String, Class<?>> result = Maps.mutable.empty();
        result.put("path", Path.class);
        result.put("rootGraphFetchTree", RootGraphFetchTree.class);
        result.put(">", RelationStoreAccessor.class);
        result.put("colSpec", ColSpec.class);
        result.put("colSpecArray", ColSpecArray.class);
        // Below Not supported by the grammar
        // Move to functions and deprecate
        result.put("listInstance", PureList.class);
        result.put("pair", Pair.class);
        result.put("aggregateValue", AggregateValue.class);
        result.put("tdsAggregateValue", TDSAggregateValue.class);
        result.put("tdsColumnInformation", TDSColumnInformation.class);
        result.put("tdsSortInformation", TDSSortInformation.class);
        result.put("tdsOlapRank", TdsOlapRank.class);
        result.put("tdsOlapAggregation", TdsOlapAggregation.class);
        // Move to VS extension
        result.put("runtimeInstance", RuntimeInstance.class);
        result.put("executionContextInstance", ExecutionContextInstance.class);
        result.put("alloySerializationConfig", SerializationConfig.class);
        PureProtocolExtensionLoader.extensions().forEach(extension -> extension.getExtraClassInstanceTypeMappings().forEach(result::put));
        return result;
    }

    private static <T> T withPureProtocolExtensions(T mapperOrBuilder, BiConsumer<? super T, ? super Module> addModule, BiConsumer<? super T, ? super NamedType> registerSubTypes, Predicate<? super String> excludeSubType)
    {
        PureProtocolExtensionLoader.logExtensionList();
        // register sub types
        Map<ProtocolSubTypeInfo<?>, PureProtocolExtension> protocolSubTypeInfoByExtension = Maps.mutable.empty();
        Map<Class<?>, ProtocolSubTypeInfo<?>> superTypesWithDefaultRegisteredSubtype = Maps.mutable.empty();
        Map<Class<?>, ProtocolSubTypeInfo<?>> registeredSubTypes = Maps.mutable.empty();
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    protocolSubTypeInfoByExtension.put(info, extension);
                    // register default subtype if specified
                    if (info.getDefaultSubType() != null)
                    {
                        // verify that the default sub type is not already been registered by another extension, this will cause unexpected behavior
                        if (superTypesWithDefaultRegisteredSubtype.containsKey(info.getSuperType()))
                        {
                            ProtocolSubTypeInfo<?> found = superTypesWithDefaultRegisteredSubtype.get(info.getSuperType());
                            throw new RuntimeException("Can't register default sub type '" + info.getDefaultSubType().getSimpleName() + "' for class '" + info.getSuperType().getSimpleName() + "' in extension '" + extension.getClass().getSimpleName() +
                                    "'. The default sub type for this class has already been registered as class '" + found.getDefaultSubType().getSimpleName() + "' by extension '" + protocolSubTypeInfoByExtension.get(found).getClass().getSimpleName() + "'");
                        }
                        superTypesWithDefaultRegisteredSubtype.put(info.getSuperType(), info);
                        addModule.accept(mapperOrBuilder, info.registerDefaultSubType());
                    }

                    // Add extra deserializer
                    SimpleModule module = new SimpleModule();
                    extension.getExtraDeserializer().forEach(x -> module.addDeserializer(x.getOne(), x.getTwo()));
                    addModule.accept(mapperOrBuilder, module);

                    // register sub types by type ID
                    info.getSubTypes().forEach(subType ->
                    {
                        if ((excludeSubType == null) || !excludeSubType.test(subType.getTwo()))
                        {
                            // verify that the default sub type is not already been registered by another extension, this will cause unexpected behavior
                            if (registeredSubTypes.containsKey(subType.getOne()))
                            {
                                ProtocolSubTypeInfo<?> found = registeredSubTypes.get(subType.getOne());
                                throw new RuntimeException("Can't register sub type '" + subType.getOne().getSimpleName() + "' for class '" + info.getSuperType().getSimpleName() + "' in extension '" + extension.getClass().getSimpleName() +
                                        "'. This sub type has already been registered by extension '" + protocolSubTypeInfoByExtension.get(found).getClass().getSimpleName() + "'");
                            }
                            registeredSubTypes.put(subType.getOne(), info);
                            registerSubTypes.accept(mapperOrBuilder, new NamedType(subType.getOne(), subType.getTwo()));
                        }
                    });
                }));
        return mapperOrBuilder;
    }

    public static ObjectMapper getNewObjectMapper()
    {
        return withPureProtocolExtensions(new ObjectMapper());
    }

    public static ObjectMapper getNewObjectMapper(Set<String> excludedSubTypes)
    {
        return withPureProtocolExtensions(new ObjectMapper(), excludedSubTypes);
    }

    public static ObjectMapper withPureProtocolConverter(ObjectMapper objectMapper)
    {
        List<ProtocolConverter<?>> protocolConverters = PureProtocolExtensionLoader.extensions().stream()
                .map(PureProtocolExtension::getProtocolConverters)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        Map<Class<?>, ProtocolConverter<?>> converterByType = protocolConverters
                .stream()
                .collect(Collectors.toMap(
                        ProtocolConverter::getType,
                        Function.identity(),
                        (a, b) -> ProtocolConverter.merge((ProtocolConverter<Object>) a, (ProtocolConverter<Object>) b))
                );

        DeserializationConfig deserializationConfig = objectMapper
                .getDeserializationConfig()
                .with(new ConverterHandlerInstantiator(converterByType));

        SimpleModule module = new SimpleModule("protocol converters");
        module.setDeserializerModifier(new ConverterBeanDeserializerModifier(converterByType));

        return objectMapper.setConfig(deserializationConfig).registerModule(module);
    }

    private static class ConverterHandlerInstantiator extends HandlerInstantiator
    {
        private final Map<Class<?>, ProtocolConverter<?>> converterByType;

        public ConverterHandlerInstantiator(Map<Class<?>, ProtocolConverter<?>> converterByType)
        {
            this.converterByType = converterByType;
        }

        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass)
        {
            ProtocolConverter<?> converter = this.converterByType.get(annotated.getRawType());
            if (converter != null)
            {
                JsonDeserializer<?> deserializer = (JsonDeserializer<?>) ClassUtil.createInstance(deserClass, config.canOverrideAccessModifiers());
                return new StdDelegatingDeserializer(new JacksonProtocolConverter<>(converter), annotated.getType(), deserializer);
            }
            return null;
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> serializerInstance(com.fasterxml.jackson.databind.SerializationConfig config, Annotated annotated, Class<?> serClass)
        {
            return null;
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass)
        {
            return null;
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass)
        {
            return null;
        }
    }

    private static class ConverterBeanDeserializerModifier extends BeanDeserializerModifier
    {
        private final Map<Class<?>, ProtocolConverter<?>> converterByType;

        public ConverterBeanDeserializerModifier(Map<Class<?>, ProtocolConverter<?>> converterByType)
        {
            this.converterByType = converterByType;
        }

        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer)
        {
            ProtocolConverter<?> converter = this.converterByType.get(beanDesc.getBeanClass());
            if (converter != null)
            {
                return new StdDelegatingDeserializer(new JacksonProtocolConverter<>(converter), beanDesc.getType(), deserializer);
            }
            return deserializer;
        }
    }

    private static class JacksonProtocolConverter<T> implements Converter<T, T>
    {
        private final ProtocolConverter<T> converter;

        private JacksonProtocolConverter(ProtocolConverter<T> converter)
        {
            this.converter = converter;
        }

        @Override
        public T convert(T value)
        {
            return this.converter.convert(value);
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory)
        {
            return typeFactory.constructType(this.converter.getType());
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory)
        {
            return typeFactory.constructType(this.converter.getType());
        }
    }
}
