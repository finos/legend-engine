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

package org.finos.legend.engine.query.sql.providers;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
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
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Optional;

/**
 * This class serves for handling the **service** source type
 * <p>
 * Sample Select statement
 * select * from service('/my/service', coordinates => 'com.gs:proj1:1.0.0')
 * select * from service('/my/service', project => 'PROD-12345', workspace => 'myWorkspace')
 * select * from service('/my/service', project => 'PROD-12345', groupWorkspace => 'myGroupWorkspace')
 */
public class LegendServiceSQLSourceProvider implements SQLSourceProvider
{
    private static final String PATTERN = "pattern";
    private static final String SERVICE = "service";

    private final ProjectCoordinateLoader projectCoordinateLoader;

    public LegendServiceSQLSourceProvider(ProjectCoordinateLoader projectCoordinateLoader)
    {
        this.projectCoordinateLoader = projectCoordinateLoader;
    }

    @Override
    public String getType()
    {
        return SERVICE;
    }

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        MutableList<Pair<SQLSource, PureModelContext>> resolved = ListIterate.collect(sources, source ->
        {
            String pattern = source.getArgumentValueAs(PATTERN, 0, String.class, true);
            ProjectCoordinateWrapper projectCoordinateWrapper = ProjectCoordinateWrapper.extractFromTableSource(source);

            ProjectResolvedContext resolvedProject = projectCoordinateLoader.resolve(projectCoordinateWrapper, identity);

            Service service = SQLProviderUtils.extractElement("service", Service.class, resolvedProject.getData(), s -> pattern.equals(s.pattern));
            FastList<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(PATTERN, 0, pattern));
            projectCoordinateWrapper.addProjectCoordinatesAsSQLSourceArguments(keys);
            SQLSource resolvedSource;

            if (service.execution instanceof PureSingleExecution)
            {
                resolvedSource = from((PureSingleExecution) service.execution, keys);
            }
            else if (service.execution instanceof PureMultiExecution)
            {
                resolvedSource = from((PureMultiExecution) service.execution, source, keys);
            }
            else
            {
                throw new EngineException("Execution Type Unsupported");
            }

            return Tuples.pair(resolvedSource, resolvedProject.getContext());
        });

        return new SQLSourceResolvedContext(resolved.collect(Pair::getTwo), resolved.collect(Pair::getOne));
    }

    private SQLSource from(PureSingleExecution pse, List<SQLSourceArgument> keys)
    {
        return new SQLSource(SERVICE, pse.func, pse.mapping, pse.runtime, pse.executionOptions, null, keys);
    }

    private SQLSource from(PureMultiExecution pme, TableSource source, List<SQLSourceArgument> keys)
    {
        String key = (String) source.getArgument(pme.executionKey, -1).getValue();
        Optional<KeyedExecutionParameter> optional = ListIterate.select(pme.executionParameters, e -> e.key.equals(key)).getFirstOptional();

        KeyedExecutionParameter execution = optional.orElseThrow(() -> new IllegalArgumentException("No execution found for key " + key));

        keys.add(new SQLSourceArgument(pme.executionKey, null, key));

        return new SQLSource(SERVICE, pme.func, execution.mapping, execution.runtime, execution.executionOptions, null, keys);
    }
}