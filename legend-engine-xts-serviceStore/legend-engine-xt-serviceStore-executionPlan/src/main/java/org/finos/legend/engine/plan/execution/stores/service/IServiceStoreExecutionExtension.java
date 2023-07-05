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

package org.finos.legend.engine.plan.execution.stores.service;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.service.auth.HttpConnectionBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public interface IServiceStoreExecutionExtension extends ExecutionExtension
{
    static List<IServiceStoreExecutionExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IServiceStoreExecutionExtension.class));
    }

    default List<Function5<SecurityScheme, Credential, HttpConnectionBuilder, Identity, CredentialProviderProvider, Boolean>> getExtraSecuritySchemeProcessors(List<Function<Credential,String>> credentialProcessors)
    {
        return Collections.emptyList();
    }

    default List<Function<Credential,String>> getExtraCredentialConsumers()
    {
        return Collections.emptyList();
    }

    default String getCredentialString(Credential credential,List<Function<Credential,String>> credentialConsumers)
    {
        return ListIterate
                .collect(credentialConsumers, processor -> processor.valueOf(credential))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new RuntimeException("Can't use the obtained credential"));
    }

}
