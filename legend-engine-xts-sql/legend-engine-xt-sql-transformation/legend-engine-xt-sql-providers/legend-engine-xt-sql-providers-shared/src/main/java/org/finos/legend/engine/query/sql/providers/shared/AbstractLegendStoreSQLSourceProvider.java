// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.query.sql.providers.shared;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.query.sql.providers.core.SQLContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateWrapper;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectResolvedContext;
import org.finos.legend.engine.query.sql.providers.shared.utils.SQLProviderUtils;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;

public abstract class AbstractLegendStoreSQLSourceProvider<T extends Store> implements SQLSourceProvider
{

    private static final String ARG_CONNECTION = "connection";
    private static final String ARG_STORE = "store";

    private final Class<T> storeType;
    private final ProjectCoordinateLoader projectCoordinateLoader;

    public AbstractLegendStoreSQLSourceProvider(Class<T> storeType, ProjectCoordinateLoader projectCoordinateLoader)
    {
        this.storeType = storeType;
        this.projectCoordinateLoader = projectCoordinateLoader;
    }

    protected abstract SQLSource createSource(TableSource source, T store, PackageableConnection connection, List<SQLSourceArgument> keys, PureModelContextData pmcd);

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        List<PureModelContext> contexts = FastList.newList();
        List<SQLSource> sqlSources = FastList.newList();

        ListIterate.forEach(sources, source ->
        {
            ProjectCoordinateWrapper projectCoordinateWrapper = ProjectCoordinateWrapper.extractFromTableSource(source);
            ProjectResolvedContext resolved = projectCoordinateLoader.resolve(projectCoordinateWrapper, identity);

            String storeName = source.getArgumentValueAs(ARG_STORE, -1, String.class, true);
            String connectionName = source.getArgumentValueAs(ARG_CONNECTION, -1, String.class, true);

            T store = SQLProviderUtils.extractElement(ARG_STORE, this.storeType, resolved.getData(), s -> storeName.equals(s.getPath()));
            PackageableConnection connection = SQLProviderUtils.extractElement(ARG_CONNECTION, PackageableConnection.class, resolved.getData(), c -> connectionName.equals(c.getPath()));

            List<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(ARG_STORE, null, storeName), new SQLSourceArgument(ARG_CONNECTION, null, connectionName));
            projectCoordinateWrapper.addProjectCoordinatesAsSQLSourceArguments(keys);

            sqlSources.add(createSource(source, store, connection, keys, resolved.getData()));

            contexts.add(resolved.getContext());
        });

        return new SQLSourceResolvedContext(contexts, sqlSources);
    }
}