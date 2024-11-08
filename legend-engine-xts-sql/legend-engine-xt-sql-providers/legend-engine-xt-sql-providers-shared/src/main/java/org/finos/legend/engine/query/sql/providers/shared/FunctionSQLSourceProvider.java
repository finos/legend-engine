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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
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

/**
 * This class serves for handling the **function** source type
 * <p>
 * Sample Select statement
 * select * from func('my::func__TabularDataSet_1_', coordinates => 'com.gs:proj1:1.0.0')
 * select * from func('my::func__TabularDataSet_1_', project => 'PROD-12345', workspace => 'myWorkspace')
 * select * from func('my::func__TabularDataSet_1_', project => 'PROD-12345', groupWorkspace => 'myGroupWorkspace')
 * select * from func('my::func_String_1__TabularDataSet_1_', project => 'PROD-12345', groupWorkspace => 'myGroupWorkspace', myParam => 'abc')
 */
public class FunctionSQLSourceProvider implements SQLSourceProvider
{

    private static final String FUNCTION = "func";
    private static final String PATH = "path";

    private static final ImmutableSet<String> TABULAR_TYPES = Sets.immutable.of(
            "meta::pure::tds::TabularDataSet"
    );

    private final ProjectCoordinateLoader projectCoordinateLoader;

    public FunctionSQLSourceProvider(ProjectCoordinateLoader projectCoordinateLoader)
    {
        this.projectCoordinateLoader = projectCoordinateLoader;
    }

    @Override
    public String getType()
    {
        return FUNCTION;
    }

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        MutableList<Pair<SQLSource, PureModelContext>> resolved = ListIterate.collect(sources, source ->
        {
            String path = source.getArgumentValueAs(PATH, 0, String.class, true);
            ProjectCoordinateWrapper projectCoordinateWrapper = ProjectCoordinateWrapper.extractFromTableSource(source);

            ProjectResolvedContext resolvedProject = projectCoordinateLoader.resolve(projectCoordinateWrapper, identity);

            Function function = SQLProviderUtils.extractElement("function", Function.class, resolvedProject.getData(), f -> path.equals(f.getPath()));

            if (!TABULAR_TYPES.contains(function.returnType))
            {
                throw new EngineException("Function " + path + " does not return Tabular data type");
            }

            Lambda lambda = new Lambda();
            lambda.parameters = function.parameters;
            lambda.body = function.body;

            List<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(PATH, 0, path));
            projectCoordinateWrapper.addProjectCoordinatesAsSQLSourceArguments(keys);

            return Tuples.pair(new SQLSource(getType(), lambda, null, null, FastList.newList(), null, keys), resolvedProject.getContext());
        });

        return new SQLSourceResolvedContext(resolved.collect(Pair::getTwo), resolved.collect(Pair::getOne));
    }
}