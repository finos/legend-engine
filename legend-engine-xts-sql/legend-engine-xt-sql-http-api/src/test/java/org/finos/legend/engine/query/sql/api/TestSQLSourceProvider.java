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

package org.finos.legend.engine.query.sql.api;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.query.sql.providers.core.SQLContext;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestSQLSourceProvider implements SQLSourceProvider
{
    private static final PureModelContextData pureModelContextData;
    private static final String service = "service";

    static
    {

        try (InputStream pureProjectInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("proj-1.pure");
             InputStreamReader pureProjectReader = new InputStreamReader(Objects.requireNonNull(pureProjectInputStream), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(pureProjectReader))
        {
            String pureProject1Contents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            pureModelContextData = PureModelContextData.newBuilder().withOrigin(null).withSerializer(null).withPureModelContextData(PureGrammarParser.newInstance().parseModel(pureProject1Contents)).build();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getType()
    {
        return service;
    }

    @Override
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, Identity identity)
    {
        List<SQLSource> sqlSources = FastList.newList();
        ListIterate.forEach(sources, source ->
        {
            String PATTERN = "pattern";
            String pattern = (String) source.getArgument(PATTERN, 0).getValue();
            String PROJECT_ID = "projectId";
            Optional<TableSourceArgument> projectId = source.getNamedArgument(PROJECT_ID);

            if (pureModelContextData == null)
            {
                throw new IllegalArgumentException("No Service found for pattern '" + pattern + "'");
            }

            LazyIterable<Service> services = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                    .collect(e -> (Service) e)
                    .select(s -> s.pattern.equals(pattern));

            if (services.isEmpty())
            {
                throw new IllegalArgumentException("No Service found for pattern '" + pattern + "'");
            }

            if (services.size() > 1)
            {
                throw new IllegalArgumentException("Multiple Services found for pattern '" + pattern + "'");
            }

            Service service = services.getOnly();

            List<SQLSourceArgument> keys = FastList.newListWith(new SQLSourceArgument(PATTERN, 0, pattern));
            projectId.ifPresent(tsa -> keys.add(new SQLSourceArgument(PROJECT_ID, 1, tsa.getValue())));

            if (service.execution instanceof PureSingleExecution)
            {
                sqlSources.add(from((PureSingleExecution) service.execution, keys));
            }
        });
        return new SQLSourceResolvedContext(pureModelContextData, sqlSources);
    }

    public PureModelContextData getPureModelContextData()
    {
        return pureModelContextData;
    }

    private SQLSource from(PureSingleExecution pse, List<SQLSourceArgument> keys)
    {
        return new SQLSource(service, pse.func, pse.mapping, pse.runtime, pse.executionOptions, null, keys);
    }
}
