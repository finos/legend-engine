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

package org.finos.legend.engine.query.sql.api.sources;

import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.pac4j.core.profile.CommonProfile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class TestSQLSourceProvider implements SQLSourceProvider
{
    private static final PureModelContextData pureModelContextData;
    private static String service = "service";

    static
    {

        try
        {
            String pureProject1 = ResourceHelpers.resourceFilePath("proj-1.pure");
            String pureProject1Contents = FileUtils.readFileToString(Paths.get(pureProject1).toFile(), Charset.defaultCharset());

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
    public SQLSourceResolvedContext resolve(List<TableSource> sources, SQLContext context, MutableList<CommonProfile> profiles)
    {
        List<SQLSource> sqlSources = FastList.newList();
        ListIterate.forEach(sources, source ->
        {
            String PATTERN = "pattern";
            String pattern = (String) source.getArgument(PATTERN, 0).getValue();
            String PROJECT_ID = "projectId";
            Optional<TableSourceArgument> projectId = source.getNamedArgument(PROJECT_ID);

            Service service = LazyIterate.select(pureModelContextData.getElements(), e -> e instanceof Service)
                    .collect(e -> (Service) e)
                    .select(s -> s.pattern.equals(pattern))
                    .getFirst();

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
        return new SQLSource(service, pse.func, pse.mapping, pse.runtime, pse.executionOptions, keys);
    }
}
