// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.shared.core.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

import java.util.Objects;
import java.util.ServiceLoader;

public class VaultFactory
{
    public static <T extends ObjectMapper> T withVaultConfigurationExtensions(T objectMapper)
    {
        MutableList<Pair<String, Class<? extends VaultConfiguration>>> subTypes = IterableIterate.flatCollect(ServiceLoader.load(VaultExtension.class),
                VaultExtension::getExtraVaultConfigurationSubTypes);

        Bag<String> duplicateSubTypeNames = subTypes.collect(Pair::getOne).toBag().selectDuplicates();
        if (!duplicateSubTypeNames.isEmpty())
        {
            throw new RuntimeException("Can't register VaultConfiguration subtypes with names " + duplicateSubTypeNames.makeString(", ") + " as they are already registered");
        }

        Bag<? extends Class> duplicateSubTypeClasses = subTypes.collect(Pair::getTwo).toBag().selectDuplicates();
        if (!duplicateSubTypeClasses.isEmpty())
        {
            throw new RuntimeException("Can't register VaultConfiguration subtypes with classes " + duplicateSubTypeClasses.collect(Class::getSimpleName).makeString(", ") + " as they are already registered");
        }

        subTypes.forEach(x -> objectMapper.registerSubtypes(new NamedType(x.getTwo(), x.getOne())));
        return objectMapper;
    }

    public static VaultImplementation generateVaultImplementationFromConfiguration(VaultConfiguration vaultConfiguration)
    {
        return IterableIterate.flatCollect(ServiceLoader.load(VaultExtension.class), VaultExtension::getExtraVaultImplementationGenerators)
                .asLazy().collect(x -> x.apply(vaultConfiguration)).detect(Objects::nonNull);
    }
}
