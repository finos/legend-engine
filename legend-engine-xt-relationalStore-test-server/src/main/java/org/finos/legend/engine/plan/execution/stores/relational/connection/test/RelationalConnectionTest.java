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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RelationalConnectionTest
{
    protected String getResourceAsString(String filePath)
    {
        InputStream stream = RelationalConnectionTest.class.getResourceAsStream(filePath);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    protected List<RelationalDatabaseConnection> readRelationalConnections(String connectionJson) throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        PureProtocolObjectMapperFactory.withPureProtocolExtensions(objectMapper);
        ObjectMapperFactory.withStandardConfigurations(objectMapper);
        return objectMapper.readValue(connectionJson, new TypeReference<List<RelationalDatabaseConnection>>(){});
    }

    protected RelationalDatabaseConnection getRelationalConnectionByElement(List<RelationalDatabaseConnection> connections, String element)
    {
        return ListIterate.detect(connections, c -> Objects.equals(c.element, element));
    }

    protected void testConnection(Connection connection, int repeat, String sqlExpression) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        MutableList<Future<Boolean>> result = FastList.newList();
        for (int i = 0; i < repeat; i++)
        {
            result.add(executor.submit(() ->
            {
                try (Statement st = connection.createStatement();
                     ResultSet resultSet = st.executeQuery(sqlExpression))
                {
                    while (resultSet.next())
                    {
                        for (int i1 = 1; i1 < resultSet.getMetaData().getColumnCount() + 1; i1++)
                        {
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
