// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog;

import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.postgres.protocol.sql.serialization.SQLSerializer;
import org.finos.legend.engine.postgres.protocol.wire.session.Session;

public class SQLRewrite extends SQLSerializer
{
    private final Session session;

    public SQLRewrite(Session session)
    {
        this.session = session;
    }

    @Override
    public String visitFunctionCall(SqlBaseParser.FunctionCallContext ctx)
    {
        if ("current_database".equals(ctx.qname().accept(this)))
        {
            return "'" + session.getDatabase() + "'";
        }
        return super.visitFunctionCall(ctx);
    }

    @Override
    public String visitTableRelation(SqlBaseParser.TableRelationContext ctx)
    {
        String value = super.visitTableRelation(ctx);
        switch (value)
        {
            case "pg_catalog.pg_database":
                return "metadata.database";
            case "pg_catalog.pg_namespace":
                return "metadata.namespace";
            case "pg_catalog.pg_class":
                return "metadata.class";
            case "pg_catalog.pg_attribute":
                return "metadata.attribute";
            case "pg_catalog.pg_proc":
                return "metadata.proc";
            default:
                return value;
        }
    }
}
