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

import java.security.PrivilegedAction;
import java.sql.ParameterMetaData;
import java.util.List;
import javax.security.auth.Subject;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

public class LegendPreparedStatement implements PostgresPreparedStatement
{
    private final String query;
    private final LegendExecutionClient client;
    private Iterable<TDSRow> tdsRows;
    private List<LegendColumn> columns;
    private final Identity identity;

    public LegendPreparedStatement(String query, LegendExecutionClient client, Identity identity)
    {
        this.query = query;
        this.client = client;
        this.identity = identity;
    }

    @Override
    public void setObject(int i, Object o)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        if (identity.getFirstCredential() instanceof LegendKerberosCredential)

        {
            LegendKerberosCredential credential = (LegendKerberosCredential) identity.getFirstCredential();
            return Subject.doAs(credential.getSubject(), (PrivilegedAction<LegendResultSetMetaData>) () ->
                    new LegendResultSetMetaData(client.getSchema(query)));
        }
        else
        {
            return new LegendResultSetMetaData(client.getSchema(query));
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData()
    {
        return PostgresPreparedStatement.emptyParameterMetaData();
    }

    @Override
    public void close() throws Exception
    {

    }

    @Override
    public void setMaxRows(int maxRows)
    {

    }

    @Override
    public boolean execute()
    {
        if (identity.getFirstCredential() instanceof LegendKerberosCredential)

        {
            LegendKerberosCredential credential = (LegendKerberosCredential) identity.getFirstCredential();
            return Subject.doAs(credential.getSubject(), (PrivilegedAction<Boolean>) this::executePrivate);
        }
        else
        {
            return executePrivate();
        }
    }

    private boolean executePrivate()
    {
        Pair<List<LegendColumn>, Iterable<TDSRow>> schemaAndResult = client.getSchemaAndExecuteQuery(query);
        columns = schemaAndResult.getOne();
        tdsRows = schemaAndResult.getTwo();
        return true;
    }

    @Override
    public PostgresResultSet getResultSet()
    {
        return new LegendResultSet(tdsRows, columns);
    }
}
