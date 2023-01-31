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

import java.sql.ParameterMetaData;
import java.util.List;
import org.finos.legend.engine.pg.postgres.LegendColumn;
import org.finos.legend.engine.pg.postgres.LegendExecutionClient;
import org.finos.legend.engine.pg.postgres.PostgresPreparedStatement;
import org.finos.legend.engine.pg.postgres.PostgresResultSet;
import org.finos.legend.engine.pg.postgres.PostgresResultSetMetaData;
import org.finos.legend.engine.pg.postgres.TDSRow;

public class LegendPreparedStatement implements PostgresPreparedStatement
{
  private String query;
  private LegendExecutionClient client;
  private Iterable<TDSRow> tdsRows;
  private List<LegendColumn> columns;

  public LegendPreparedStatement(String query, LegendExecutionClient client)
  {
    this.query = query;
    this.client = client;
  }

  @Override
  public void setObject(int i, Object o) throws Exception
  {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PostgresResultSetMetaData getMetaData() throws Exception
  {
    return new LegendResultSetMetaData(client.getSchema(query));
  }

  @Override
  public ParameterMetaData getParameterMetaData() throws Exception
  {
    return null;
  }

  @Override
  public void close() throws Exception
  {

  }

  @Override
  public void setMaxRows(int maxRows) throws Exception
  {

  }

  @Override
  public boolean execute() throws Exception
  {
    tdsRows= client.executeQuery(query);
    columns = client.getSchema(query);
    return true;
  }

  @Override
  public PostgresResultSet getResultSet() throws Exception
  {
    return new LegendResultSet(tdsRows, columns);
  }
}
