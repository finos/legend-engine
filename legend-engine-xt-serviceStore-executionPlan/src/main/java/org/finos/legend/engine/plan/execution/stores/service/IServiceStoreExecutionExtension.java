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

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.function.Function5;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.ServiceLoader;

public interface IServiceStoreExecutionExtension extends ExecutionExtension
{
    static List<IServiceStoreExecutionExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IServiceStoreExecutionExtension.class));
    }

    default List<Function5<SecurityScheme, AuthenticationSpecification, HttpClientBuilder, RequestBuilder, MutableList<CommonProfile>, Boolean>> getExtraSecuritySchemeProcessors()
    {
        return FastList.newList();
    }
}
