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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.query.sql.providers.core.SQLContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class E2eTestSourceProvider implements SQLSourceProvider
{
    private static final String FUNCTION = "func";
    private static final String PATH = "path";
    private static final String MAPPING = "e2e::TestMapping";
    private static final String STORE = "e2e::TestDB";

    private final PureModelContextData pureModelContextData;
    private final String pgHost;
    private final int pgPort;
    private final String pgDatabase;
    private final String pgUser;
    private final String pgPassword;

    public E2eTestSourceProvider(PureModelContextData pureModelContextData,
                                 String pgHost, int pgPort, String pgDatabase,
                                 String pgUser, String pgPassword)
    {
        this.pureModelContextData = pureModelContextData;
        this.pgHost = pgHost;
        this.pgPort = pgPort;
        this.pgDatabase = pgDatabase;
        this.pgUser = pgUser;
        this.pgPassword = pgPassword;
    }

    @Override
    public String getType()
    {
        return FUNCTION;
    }

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        List<SQLSource> sqlSources = FastList.newList();

        for (TableSource source : sources)
        {
            String path = (String) source.getArgument(PATH, 0).getValue();

            LazyIterable<Function> functions = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Function)
                    .collect(e -> (Function) e);

            Function function = functions
                    .select(f -> matchesFunctionName(f, path))
                    .getFirst();

            if (function == null)
            {
                String available = functions.collect(f -> f._package + "::" + stripSignature(f.name)).makeString(", ");
                throw new IllegalArgumentException("No function found for path '" + path + "'. Available: [" + available + "]");
            }

            LambdaFunction lambda = new LambdaFunction();
            lambda.parameters = function.parameters;
            lambda.body = function.body;

            EngineRuntime runtime = buildRuntime();

            List<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(PATH, 0, path));

            // Extract resolved arguments for parameterized functions
            Map<String, Object> resolvedArguments = resolveArguments(source, function);

            sqlSources.add(new SQLSource(getType(), lambda, MAPPING, runtime, FastList.newList(), null, keys, null, resolvedArguments));
        }

        return new SQLSourceResolvedContext(pureModelContextData, sqlSources);
    }

    private EngineRuntime buildRuntime()
    {
        StaticDatasourceSpecification spec = new StaticDatasourceSpecification();
        spec.host = pgHost;
        spec.port = pgPort;
        spec.databaseName = pgDatabase;

        UserNamePasswordAuthenticationStrategy authStrategy = new UserNamePasswordAuthenticationStrategy();
        authStrategy.baseVaultReference = "e2e.";
        authStrategy.userNameVaultReference = "user";
        authStrategy.passwordVaultReference = "password";

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
        connection.element = STORE;
        connection.type = DatabaseType.Postgres;
        connection.datasourceSpecification = spec;
        connection.authenticationStrategy = authStrategy;

        EngineRuntime runtime = new EngineRuntime();

        PackageableElementPointer mappingPtr = new PackageableElementPointer();
        mappingPtr.type = PackageableElementType.MAPPING;
        mappingPtr.path = MAPPING;
        runtime.mappings = FastList.newListWith(mappingPtr);

        StoreConnections storeConnections = new StoreConnections();
        PackageableElementPointer storePtr = new PackageableElementPointer();
        storePtr.type = PackageableElementType.STORE;
        storePtr.path = STORE;
        storeConnections.store = storePtr;

        IdentifiedConnection identifiedConnection = new IdentifiedConnection();
        identifiedConnection.id = "connection_1";
        identifiedConnection.connection = connection;
        storeConnections.storeConnections = FastList.newListWith(identifiedConnection);

        runtime.connections = FastList.newListWith(storeConnections);

        return runtime;
    }

    private boolean matchesFunctionName(Function function, String path)
    {
        String fullPath = function.getPath();
        if (fullPath != null && fullPath.equals(path))
        {
            return true;
        }
        String simplePath = function._package + "::" + stripSignature(function.name);
        if (simplePath.equals(path))
        {
            return true;
        }
        // For parameterized functions: path is "e2e::tds_persons_by_dept" but
        // simplePath is "e2e::tds_persons_by_dept_Integer_1" (includes param types before __)
        // Check if simplePath starts with path + "_" (parameter type suffix)
        return simplePath.startsWith(path + "_");
    }

    private Map<String, Object> resolveArguments(TableSource source, Function function)
    {
        Map<String, Object> resolved = new HashMap<>();
        List<TableSourceArgument> args = source.getArguments();

        // Arguments at index 1+ are the function parameters (index 0 is the path)
        if (function.parameters != null)
        {
            for (int i = 0; i < function.parameters.size(); i++)
            {
                String paramName = function.parameters.get(i).name;
                // The function argument is at position i+1 in the TableSource args (0 is path)
                int sourceIndex = i + 1;
                if (sourceIndex < args.size())
                {
                    TableSourceArgument arg = args.get(sourceIndex);
                    if (arg.getValue() != null)
                    {
                        resolved.put(paramName, arg.getValue());
                    }
                }
            }
        }

        // Also include any named arguments
        for (TableSourceArgument arg : args)
        {
            if (arg.getName() != null && arg.getValue() != null)
            {
                resolved.put(arg.getName(), arg.getValue());
            }
        }

        return resolved.isEmpty() ? null : resolved;
    }

    private static String stripSignature(String name)
    {
        int idx = name.indexOf("__");
        return idx > 0 ? name.substring(0, idx) : name;
    }

    public PureModelContextData getPureModelContextData()
    {
        return pureModelContextData;
    }
}

