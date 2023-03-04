// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBConnection;
import org.pac4j.core.profile.CommonProfile;

import java.io.InputStream;
import java.util.List;

public class MongoDBExecutor
{

    private final CredentialProviderProvider credentialProviderProvider;

    public MongoDBExecutor(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public InputStreamResult executeMongoDBQuery(String dbCommand, MongoDBConnection dbConnection)
                                                 //List<Header> headers, StringEntity requestBodyEntity, HttpMethod httpMethod, String mimeType, List<SecurityScheme> securitySchemes, List<AuthenticationSchemeRequirement> authenticationSchemeRequirements, MutableList<CommonProfile> profiles)
    {
        // Conection has datasource details & authentication.
        // TODO : Stream?
        InputStream response = null;
        return new InputStreamResult(response, org.eclipse.collections.api.factory.Lists.mutable.with(new MongoDBStoreExecutionActivity(dbCommand)));
    }
}
