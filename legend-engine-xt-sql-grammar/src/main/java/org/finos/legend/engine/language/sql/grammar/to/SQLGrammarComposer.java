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
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.NodeVisitor;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QueryBody;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.Select;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
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
                return "* ";
            }

            @Override
            public String visit(Query val)
            {
                return val.queryBody.accept(this);
            }

            @Override
            public String visit(QueryBody val)
            {
                return null;
            }

            @Override
            public String visit(QuerySpecification val)
            {
                return val.select.accept(this) + "from " + visit(val.from);
            }

            @Override
            public String visit(Relation val)
            {
                return null;
            }

            @Override
            public String visit(Select val)
            {
                return "select " + (val.distinct ? "distinct " : "") + visit(val.selectItems);
            }

            @Override
            public String visit(SelectItem val)
            {
                return null;
            }

            @Override
            public String visit(Statement val)
            {
                return null;
            }

            @Override
            public String visit(Table val)
            {
                return val.name + " ";
            }

            private String visit(List<? extends Node> nodes)
            {
                return nodes.stream()
                        .map(node -> node.accept(this))
                        .collect(Collectors.joining(" "));
            }
        });
    }
}
