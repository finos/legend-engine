// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.server.test.shared;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.ws.rs.core.Response;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExecuteInRelationalDbTest
{
  public static final String DROP_TABLE_SQL = "DROP TABLE ...";
  public static final String CREATE_TABLE_SQL = "CREATE TABLE ...";
  @InjectMocks
  private ExecuteInRelationalDb executeInRelationalDb;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ConnectionManagerSelector connectionManagerSelector;

  @Test
  public void shouldNotFailOnDropTableException() throws SQLException
  {
    Statement statement =
        connectionManagerSelector.getDatabaseConnection(any(MutableList.class), any()).createStatement();
    when(statement.execute(DROP_TABLE_SQL)).thenThrow(new SQLException());

    ExecuteInRelationalDbInput input = new ExecuteInRelationalDbInput();
    input.sqls = Lists.mutable.with(DROP_TABLE_SQL, DROP_TABLE_SQL.toLowerCase(Locale.ROOT), CREATE_TABLE_SQL);
    Response response = executeInRelationalDb.executeInRelationalDb(null, input, null);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void shouldFailOnNonDropTableException() throws SQLException
  {
    Statement statement =
        connectionManagerSelector.getDatabaseConnection(any(MutableList.class), any()).createStatement();
    when(statement.execute(CREATE_TABLE_SQL)).thenThrow(new SQLException());

    ExecuteInRelationalDbInput input = new ExecuteInRelationalDbInput();
    input.sqls = Lists.mutable.with(DROP_TABLE_SQL, DROP_TABLE_SQL.toLowerCase(Locale.ROOT), CREATE_TABLE_SQL);
    Response response = executeInRelationalDb.executeInRelationalDb(null, input, null);

    assertThat(response.getStatus(), is(500));
  }
}