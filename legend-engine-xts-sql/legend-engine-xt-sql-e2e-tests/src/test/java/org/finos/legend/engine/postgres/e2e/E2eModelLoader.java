// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;

public class E2eModelLoader implements ModelLoader
{
    public static final String GROUP_ID = "org.finos.legend.engine.e2e";
    public static final String ARTIFACT_ID = "sql-e2e-model";
    public static final String VERSION = "1.0.0";
    public static final String COORDINATES = GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION;
    public static final String CONNECTION_PATH = "e2e::TestConnection";
    public static final String VAULT_REFERENCE_BASE = "e2e.";

    private final PureModelContextData model;

    public E2eModelLoader(PureModelContextData baseModel, String pgHost, int pgPort, String pgDatabase)
    {
        this.model = pointConnectionAtPostgres(baseModel, pgHost, pgPort, pgDatabase);
    }

    @Override
    public boolean supports(PureModelContext context)
    {
        return context instanceof PureModelContextPointer && isE2eProject(((PureModelContextPointer) context).sdlcInfo);
    }

    @Override
    public PureModelContextData load(Identity identity, PureModelContext context, String clientVersion, Span parentSpan)
    {
        if (!supports(context))
        {
            throw new IllegalArgumentException("Only project " + COORDINATES + " is served, got: " + context);
        }
        return this.model;
    }

    @Override
    public void setModelManager(ModelManager modelManager)
    {
    }

    @Override
    public boolean shouldCache(PureModelContext context)
    {
        return true;
    }

    @Override
    public PureModelContext cacheKey(PureModelContext context, Identity identity)
    {
        return context;
    }

    private static boolean isE2eProject(Object sdlc)
    {
        if (!(sdlc instanceof AlloySDLC))
        {
            return false;
        }
        AlloySDLC alloy = (AlloySDLC) sdlc;
        return GROUP_ID.equals(alloy.groupId) && ARTIFACT_ID.equals(alloy.artifactId) && VERSION.equals(alloy.version);
    }

    private static PureModelContextData pointConnectionAtPostgres(PureModelContextData baseModel, String pgHost, int pgPort, String pgDatabase)
    {
        PureModelContextData model = copyOf(baseModel);
        PackageableConnection packageableConnection = model.getElementsOfType(PackageableConnection.class).stream()
                .filter(c -> CONNECTION_PATH.equals(c.getPath()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("e2e model has no connection " + CONNECTION_PATH));
        RelationalDatabaseConnection connection = (RelationalDatabaseConnection) packageableConnection.connectionValue;

        StaticDatasourceSpecification specification = new StaticDatasourceSpecification();
        specification.host = pgHost;
        specification.port = pgPort;
        specification.databaseName = pgDatabase;
        connection.datasourceSpecification = specification;

        UserNamePasswordAuthenticationStrategy authentication = new UserNamePasswordAuthenticationStrategy();
        authentication.baseVaultReference = VAULT_REFERENCE_BASE;
        authentication.userNameVaultReference = "user";
        authentication.passwordVaultReference = "password";
        connection.authenticationStrategy = authentication;
        return model;
    }

    private static PureModelContextData copyOf(PureModelContextData model)
    {
        ObjectMapper mapper = PureProtocolObjectMapperFactory.getNewObjectMapper();
        try
        {
            return mapper.readValue(mapper.writeValueAsString(model), PureModelContextData.class);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("Could not copy the e2e model", e);
        }
    }
}
