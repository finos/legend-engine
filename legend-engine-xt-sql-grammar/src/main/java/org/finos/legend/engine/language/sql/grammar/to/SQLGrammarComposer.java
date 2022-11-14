// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.sql.grammar.to;

import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.AllRows;
import org.finos.legend.engine.protocol.sql.metamodel.Expression;
import org.finos.legend.engine.protocol.sql.metamodel.Identifier;
import org.finos.legend.engine.protocol.sql.metamodel.Limit;
import org.finos.legend.engine.protocol.sql.metamodel.Literal;
import org.finos.legend.engine.protocol.sql.metamodel.LongLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.NodeVisitor;
import org.finos.legend.engine.protocol.sql.metamodel.OrderBy;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QueryBody;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.Select;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
import org.finos.legend.engine.protocol.sql.metamodel.SingleColumn;
import org.finos.legend.engine.protocol.sql.metamodel.SortItem;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.engine.protocol.sql.metamodel.Table;

import java.util.List;
import java.util.stream.Collectors;

public class SQLGrammarComposer
{
    private SQLGrammarComposer()
    {
    }

    public static SQLGrammarComposer newInstance()
    {
        return new SQLGrammarComposer();
    }

    public String renderNode(Node node)
    {
        return node.accept(new NodeVisitor<String>()
        {
            @Override
            public String visit(AllColumns val)
            {
                return "*";
            }

            @Override
            public String visit(AllRows val)
            {
                return "ALL";
            }

            @Override
            public String visit(Expression val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(Identifier val)
            {
                return val.delimited ? "\"" + val.value + "\"" : val.value;
            }

            @Override
            public String visit(Limit val)
            {
                return " limit " + val.rowCount.accept(this);
            }

            @Override
            public String visit(Literal val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(LongLiteral val)
            {
                return Long.toString(val.value);
            }

            @Override
            public String visit(OrderBy val)
            {
                return val.sortItems.isEmpty() ? "" : " order by " + visit(val.sortItems, ", ");
            }

            @Override
            public String visit(Query val)
            {
                return val.queryBody.accept(this)
                        + val.orderBy.accept(this)
                        + val.limit.accept(this);
            }

            @Override
            public String visit(QueryBody val)
            {
                return null;
            }

            @Override
            public String visit(QuerySpecification val)
            {
                return val.select.accept(this) + " from " + visit(val.from, "");
            }

            @Override
            public String visit(Relation val)
            {
                return null;
            }

            @Override
            public String visit(Select val)
            {
                return "select " + (val.distinct ? "distinct " : "") + visit(val.selectItems, ", ");
            }

            @Override
            public String visit(SelectItem val)
            {
                return null;
            }

            @Override
            public String visit(SingleColumn val)
            {
                String column = "";
                Identifier prefixIdent = val.prefix;
                if (prefixIdent != null)
                {
                    column += prefixIdent.accept(this) + ".";
                }
                return column + val.expression.accept(this);
            }

            @Override
            public String visit(SortItem val)
            {
                String sortItem = "";
                sortItem += val.sortKey.accept(this);
                switch (val.ordering)
                {
                    case ASCENDING:
                        sortItem += " ASC";
                        break;
                    case DESCENDING:
                        sortItem += " DESC";
                        break;
                }
                switch (val.nullOrdering)
                {
                    case FIRST:
                        sortItem += " NULLS FIRST";
                        break;
                    case LAST:
                        sortItem += " NULLS LAST";
                        break;
                }
                return sortItem;
            }

            @Override
            public String visit(Statement val)
            {
                return null;
            }

            @Override
            public String visit(Table val)
            {
                return visit(val.name, ".");
            }

            private String visit(List<? extends Node> nodes, String delimiter)
            {
                return nodes.stream()
                        .map(node -> node.accept(this))
                        .collect(Collectors.joining(delimiter));
            }
        });
    }
}
