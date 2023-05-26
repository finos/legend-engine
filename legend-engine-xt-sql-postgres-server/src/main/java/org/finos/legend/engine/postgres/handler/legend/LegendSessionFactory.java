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

import org.finos.legend.engine.postgres.Session;
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.shared.core.identity.Identity;

public class LegendSessionFactory implements SessionsFactory
{

    private final LegendExecutionClient legendExecutionClient;

    public LegendSessionFactory(LegendExecutionClient legendExecutionClient)
    {
        this.legendExecutionClient = legendExecutionClient;
    }

    @Override
    public Session createSession(String defaultSchema, Identity identity)
    {
        return new Session(new LegendSessionHandler(legendExecutionClient, identity), null);
    }

    private static class LegendSessionHandler implements SessionHandler
    {
        private final LegendExecutionClient legendExecutionClient;
        private final Identity identity;

        public LegendSessionHandler(LegendExecutionClient legendExecutionClient, Identity identity)
        {
            this.legendExecutionClient = legendExecutionClient;
            this.identity = identity;
        }

        @Override
        public PostgresPreparedStatement prepareStatement(String query)
        {
            return new LegendPreparedStatement(query, legendExecutionClient, identity);
        }

        @Override
        public PostgresStatement createStatement()
        {
            return new LegendStatement(legendExecutionClient, identity);
        }
    }
}
