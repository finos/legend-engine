// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class DbSpecificTests
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected void testConnection(Connection connection, String sqlExpression) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        MutableList<Future<Boolean>> result = FastList.newList();
        for (int i = 0; i < 30; i++)
        {
            result.add(executor.submit(() -> {
                try (Statement st = connection.createStatement();
                     ResultSet resultSet = st.executeQuery(sqlExpression))
                {
                    while (resultSet.next())
                    {
                        for (int i1 = 1; i1 < resultSet.getMetaData().getColumnCount() + 1; i1++)
                        {
                            System.out.println(resultSet.getMetaData().getColumnLabel(i1) + " = " + resultSet.getObject(i1));
                        }
                    }
                    return true;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        executor.awaitTermination(100000, TimeUnit.MINUTES);

        boolean res = true;
        for (Future<Boolean> val : result)
        {
            res = res && val.get();
        }
        assert (res);
    }

}
