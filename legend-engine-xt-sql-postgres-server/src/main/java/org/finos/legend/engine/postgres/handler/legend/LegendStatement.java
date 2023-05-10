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
import java.util.List;
import javax.security.auth.Subject;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.postgres.handler.PostgresResultSet;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

public class LegendStatement implements PostgresStatement
{

    private LegendExecutionClient client;
    private Iterable<TDSRow> tdsRows;
    private List<LegendColumn> columns;
    private Identity identity;

    public LegendStatement(LegendExecutionClient client, Identity identity)
    {
        this.client = client;
        this.identity = identity;
    }

    @Override
    public boolean execute(String query) throws Exception
    {
        if (identity.getFirstCredential() instanceof LegendKerberosCredential)
        {
            LegendKerberosCredential credential = (LegendKerberosCredential) identity.getFirstCredential();
            return Subject.doAs(credential.getSubject(), (PrivilegedAction<Boolean>) () ->
            {
                return executePrivate(query);
            });
        }
        else
        {
            return executePrivate(query);

        }
    }

    private boolean executePrivate(String query)
    {
        Pair<List<LegendColumn>, Iterable<TDSRow>> schemaAndResult = client.getSchemaAndExecuteQuery(query);
        columns = schemaAndResult.getOne();
        tdsRows = schemaAndResult.getTwo();
        return true;
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
        return new LegendResultSet(tdsRows, columns);
    }
}
