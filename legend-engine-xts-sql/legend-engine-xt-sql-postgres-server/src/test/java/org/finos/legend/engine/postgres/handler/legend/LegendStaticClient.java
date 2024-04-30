// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.handler.legend;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;

public class LegendStaticClient implements LegendClient
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(LegendStaticClient.class);
    private String queryResultPath;
    private String querySchemaPath;
    private int executionDelay = 0;

    public LegendStaticClient(String queryResultPath, String querySchemaPath)
    {
        this(queryResultPath, querySchemaPath, 0);
    }

    public LegendStaticClient(String queryResultPath, String querySchemaPath, int executionDelay)
    {
        this.queryResultPath = queryResultPath;
        this.querySchemaPath = querySchemaPath;
        this.executionDelay = executionDelay;
    }

    @Override
    public InputStream executeQueryApi(String query)
    {
        try
        {
            LOGGER.info("\n\n Start sleeping");

            Thread.sleep(executionDelay);

            LOGGER.info("\n\n Finish  sleeping");

            return Files.newInputStream(Paths.get(queryResultPath));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream executeSchemaApi(String query)
    {
        try
        {
            return Files.newInputStream(Paths.get(querySchemaPath));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
