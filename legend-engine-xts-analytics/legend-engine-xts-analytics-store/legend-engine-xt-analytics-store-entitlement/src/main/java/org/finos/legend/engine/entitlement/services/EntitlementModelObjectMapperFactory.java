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

package org.finos.legend.engine.entitlement.services;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;

import java.util.Map;
import java.util.function.BiConsumer;

public class EntitlementModelObjectMapperFactory
{

    public static <T extends ObjectMapper> T withEntitlementModelExtensions(T objectMapper)
    {
        return withEntitlementModelExtensions(objectMapper, ObjectMapper::registerModule, ObjectMapper::registerSubtypes);
    }

    public static ObjectMapper getNewObjectMapper()
    {
        return withEntitlementModelExtensions(new ObjectMapper());
    }

    private static <T> T withEntitlementModelExtensions(T mapperOrBuilder, BiConsumer<? super T, ? super Module> addModule, BiConsumer<? super T, ? super NamedType> registerSubTypes)
    {
        EntitlementServiceExtensionLoader.logExtensionList();
        // register sub types
        Map<ProtocolSubTypeInfo<?>, EntitlementServiceExtension> subTypeInfoByExtension = Maps.mutable.empty();
        Map<Class<?>, ProtocolSubTypeInfo<?>> superTypesWithDefaultRegisteredSubtype = Maps.mutable.empty();
        Map<Class<?>, ProtocolSubTypeInfo<?>> registeredSubTypes = Maps.mutable.empty();
        EntitlementServiceExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    subTypeInfoByExtension.put(info, extension);
                    // register default subtype if specified
                    if (info.getDefaultSubType() != null)
                    {
                        // verify that the default sub type is not already been registered by another extension, this will cause unexpected behavior
                        if (superTypesWithDefaultRegisteredSubtype.containsKey(info.getSuperType()))
                        {
                            ProtocolSubTypeInfo<?> found = superTypesWithDefaultRegisteredSubtype.get(info.getSuperType());
                            throw new RuntimeException("Can't register default sub type '" + info.getDefaultSubType().getSimpleName() + "' for class '" + info.getSuperType().getSimpleName() + "' in extension '" + extension.getClass().getSimpleName() +
                                    "'. The default sub type for this class has already been registered as class '" + found.getDefaultSubType().getSimpleName() + "' by extension '" + subTypeInfoByExtension.get(found).getClass().getSimpleName() + "'");
                        }
                        superTypesWithDefaultRegisteredSubtype.put(info.getSuperType(), info);
                        addModule.accept(mapperOrBuilder, info.registerDefaultSubType());
                    }
                    // register sub types by type ID
                    info.getSubTypes().forEach(subType ->
                    {
                        // verify that the default sub type is not already been registered by another extension, this will cause unexpected behavior
                        if (registeredSubTypes.containsKey(subType.getOne()))
                        {
                            ProtocolSubTypeInfo<?> found = registeredSubTypes.get(subType.getOne());
                            throw new RuntimeException("Can't register sub type '" + subType.getOne().getSimpleName() + "' for class '" + info.getSuperType().getSimpleName() + "' in extension '" + extension.getClass().getSimpleName() +
                                    "'. This sub type has already been registered by extension '" + subTypeInfoByExtension.get(found).getClass().getSimpleName() + "'");
                        }
                        registeredSubTypes.put(subType.getOne(), info);
                        registerSubTypes.accept(mapperOrBuilder, new NamedType(subType.getOne(), subType.getTwo()));
                    });
                }));
        return mapperOrBuilder;
    }
}


