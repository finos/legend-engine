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

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement;

import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendResultSet;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendResultSetMetaData;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSetMetaData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendConstrainedKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.sql.ParameterMetaData;
import java.util.Optional;

public class LegendPreparedStatement implements PostgresPreparedStatement
{
    private final String query;
    private final LegendExecution client;
    private final Identity identity;
    private boolean isExecuted;
    private int maxRows;
    private LegendResultSet legendResultSet;
    private final String database;
    private final String options;

    public LegendPreparedStatement(String query, LegendExecution client, String database, String options, Identity identity)
    {
        this.query = query;
        this.client = client;
        this.identity = identity;
        this.database = database;
        this.options = options;
    }

    @Override
    public void setObject(int i, Object o)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        Optional<LegendKerberosCredential> legendKerberosCredential = identity.getCredential(LegendKerberosCredential.class);
        Optional<LegendConstrainedKerberosCredential> legendConstrainedKerberosCreds = identity.getCredential(LegendConstrainedKerberosCredential.class);
        if (legendKerberosCredential.isPresent())
        {
            LegendKerberosCredential credential = legendKerberosCredential.get();
            return Subject.doAs(credential.getSubject(), (PrivilegedAction<LegendResultSetMetaData>) () ->
                    new LegendResultSetMetaData(client.getSchema(query, database)));
        }
        else if (legendConstrainedKerberosCreds.isPresent())
        {
            LegendConstrainedKerberosCredential credential = legendConstrainedKerberosCreds.get();
            return Subject.doAs(credential.getMergedSubject(), (PrivilegedAction<LegendResultSetMetaData>) () ->
                    new LegendResultSetMetaData(client.getSchema(query, database)));
        }
        else
        {
            return new LegendResultSetMetaData(client.getSchema(query, database));
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
        if (legendResultSet != null)
        {
            legendResultSet.close();
        }
    }

    @Override
    public void setMaxRows(int maxRows)
    {
        this.maxRows = maxRows;
    }

    @Override
    public int getMaxRows()
    {
        return maxRows;
    }

    @Override
    public boolean isExecuted()
    {
        return isExecuted;
    }

    @Override
    public boolean execute()
    {
        isExecuted = true;
        Optional<LegendKerberosCredential> legendKerberosCredential = identity.getCredential(LegendKerberosCredential.class);
        Optional<LegendConstrainedKerberosCredential> legendConstrainedKerberosCreds = identity.getCredential(LegendConstrainedKerberosCredential.class);
        if (legendKerberosCredential.isPresent())
        {
            LegendKerberosCredential credential = legendKerberosCredential.get();
            return Subject.doAs(credential.getSubject(), (PrivilegedAction<Boolean>) this::executePrivate);
        }
        else if (legendConstrainedKerberosCreds.isPresent())
        {
            LegendConstrainedKerberosCredential credential = legendConstrainedKerberosCreds.get();
            return Subject.doAs(credential.getMergedSubject(),(PrivilegedAction<Boolean>) this::executePrivate);
        }
        else
        {
            return executePrivate();
        }
    }

    private boolean executePrivate()
    {
        legendResultSet = new LegendResultSet(client.executeQuery(query, database, options));
        return true;
    }

    @Override
    public PostgresResultSet getResultSet()
    {
        return legendResultSet;
    }

    @Override
    public String toString()
    {
        return "LegendPreparedStatement{" +
                "query='" + query + '\'' +
                '}';
    }
}
