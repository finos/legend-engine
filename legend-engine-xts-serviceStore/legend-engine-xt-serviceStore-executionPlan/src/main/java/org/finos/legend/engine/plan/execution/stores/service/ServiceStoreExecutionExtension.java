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

import org.apache.commons.codec.binary.Base64;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.service.auth.HttpConnectionBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.LimitExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RestServiceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ServiceParametersResolutionExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ApiKeySecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpSecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.function.Function5;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.pac4j.core.profile.CommonProfile;

import java.util.Collections;
import java.util.List;

public class ServiceStoreExecutionExtension implements IServiceStoreExecutionExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Service");
    }

    @Override
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList(((executionNode, profiles, executionState) ->
        {
            if (executionNode instanceof RestServiceExecutionNode || executionNode instanceof ServiceParametersResolutionExecutionNode || executionNode instanceof LimitExecutionNode)
            {
                return executionNode.accept(executionState.getStoreExecutionState(StoreType.Service).getVisitor(profiles, executionState));
            }
            return null;
        }));
    }

    public List<Function5<SecurityScheme, Credential, HttpConnectionBuilder, Identity, CredentialProviderProvider, Boolean>> getExtraSecuritySchemeProcessors(List<Function<Credential,String>> credentialConsumers)
    {
        return Lists.mutable.with((securityScheme, credential, httpConnectionBuilder, identity, credentialProviderProvider) ->
        {
            if (securityScheme instanceof HttpSecurityScheme)
            {
                String cred = getCredentialString(credential,credentialConsumers);
                httpConnectionBuilder.requestBuilder.addHeader("Authorization", "Basic " + cred);
                return true;
            }
            else if (securityScheme instanceof ApiKeySecurityScheme)
            {
                ApiKeySecurityScheme apiKeySecurityScheme = (ApiKeySecurityScheme) securityScheme;
                if (apiKeySecurityScheme.location == ApiKeySecurityScheme.Location.COOKIE)
                {
                    String cred = getCredentialString(credential,credentialConsumers);
                    httpConnectionBuilder.requestBuilder.addHeader("Cookie", String.format("%s=%s", apiKeySecurityScheme.keyName, cred));
                }
                return true;
            }
            return null;
        });
    }

    public List<Function<Credential, String>> getExtraCredentialConsumers()
    {
        return Lists.mutable.with(credential ->
        {
            if (credential instanceof PlaintextUserPasswordCredential)
            {
                PlaintextUserPasswordCredential cred = (PlaintextUserPasswordCredential) credential;
                return Base64.encodeBase64String((cred.getUser() + ":" + cred.getPassword()).getBytes());
            }
            else if (credential instanceof ApiTokenCredential)
            {
                ApiTokenCredential cred = (ApiTokenCredential) credential;
                return cred.getApiToken();
            }
            return null;
        });
    }
}
