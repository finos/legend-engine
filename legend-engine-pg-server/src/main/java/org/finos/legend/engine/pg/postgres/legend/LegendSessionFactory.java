/*
 * // Copyright 2020 Goldman Sachs
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 */

package org.finos.legend.engine.pg.postgres.legend;

import java.sql.SQLException;
import org.finos.legend.engine.Session;
import org.finos.legend.engine.SessionHandler;
import org.finos.legend.engine.SessionsFactory;
import org.finos.legend.engine.pg.postgres.LegendExecutionClient;
import org.finos.legend.engine.pg.postgres.PostgresPreparedStatement;
import org.finos.legend.engine.pg.postgres.PostgresStatement;
import org.finos.legend.engine.pg.postgres.auth.User;
import org.jetbrains.annotations.Nullable;

public class LegendSessionFactory implements SessionsFactory
{

  private LegendExecutionClient legendExecutionClient;

  public LegendSessionFactory(LegendExecutionClient legendExecutionClient)
  {
    this.legendExecutionClient = legendExecutionClient;
  }

  @Override
  public Session createSession(@Nullable String defaultSchema, User authenticatedUser)
      throws Exception
  {
    return new Session(new SessionHandler()
    {
      @Override
      public PostgresPreparedStatement prepareStatement(String query) throws SQLException
      {
        return new LegendPreparedStatement(query, legendExecutionClient);
      }

      @Override
      public PostgresStatement createStatement() throws SQLException
      {
        return new LegendStatement(legendExecutionClient);
      }
    });
  }
}
