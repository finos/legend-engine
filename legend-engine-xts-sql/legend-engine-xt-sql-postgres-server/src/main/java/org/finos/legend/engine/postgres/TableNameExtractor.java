// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.postgres;

import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParserBaseVisitor;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedName;

import java.util.List;

public class TableNameExtractor extends SqlBaseParserBaseVisitor<List<QualifiedName>>
{
    private final Boolean extractTables;
    private final Boolean extractTableFunctions;

    public TableNameExtractor() 
    {
        this(true, true);
    }

    public TableNameExtractor(Boolean extractTables, Boolean extractTableFunctions) 
    {
        this.extractTables = extractTables;
        this.extractTableFunctions = extractTableFunctions;
    }

    @Override
    public List<QualifiedName> visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        QualifiedName qualifiedName = getQualifiedName(ctx.qname());
        return this.extractTables ? Lists.fixedSize.with(qualifiedName) : Lists.fixedSize.empty();
    }

    @Override
    public List<QualifiedName> visitTableFunction(SqlBaseParser.TableFunctionContext ctx)
    {
        QualifiedName qualifiedName = getQualifiedName(ctx.qname());
        return this.extractTableFunctions ? Lists.fixedSize.with(qualifiedName) : Lists.fixedSize.empty();
    }

    @Override
    protected List<QualifiedName> aggregateResult(List<QualifiedName> aggregate, List<QualifiedName> nextResult)
    {
        MutableList<QualifiedName> result = Lists.fixedSize.empty();
        if (nextResult != null)
        {
            result = result.withAll(nextResult);
        }
        if (aggregate != null)
        {
            result = result.withAll(aggregate);
        }
        return result;
    }

    private List<String> visitIdentifier(ParseTree tree)
    {
        if (tree instanceof SqlBaseParser.UnquotedIdentifierContext)
        {
            SqlBaseParser.UnquotedIdentifierContext context = (SqlBaseParser.UnquotedIdentifierContext) tree;
            String text = context.getText();
            return Lists.fixedSize.with(text);
        }
        if (tree instanceof SqlBaseParser.QuotedIdentifierContext)
        {
            SqlBaseParser.QuotedIdentifierContext ctx = (SqlBaseParser.QuotedIdentifierContext) tree;
            String token = ctx.getText();
            String identifier = token.substring(1, token.length() - 1)
                    .replace("\"\"", "\"");
            return Lists.fixedSize.with(identifier);
        }
        else
        {
            return Lists.fixedSize.empty();
        }
    }


    private List<String> visitIdentifier(SqlBaseParser.IdentContext node)
    {
        List<String> result = Lists.fixedSize.empty();
        int n = node.getChildCount();

        for (int i = 0; i < n; ++i)
        {
            ParseTree c = node.getChild(i);
            List<String> childResult = visitIdentifier(c);
            result = Lists.fixedSize.withAll(result).withAll(childResult);
        }

        return result;
    }

    private List<String> getIdentText(SqlBaseParser.IdentContext ident)
    {
        if (ident != null)
        {
            return visitIdentifier(ident);
        }
        return null;
    }

    private QualifiedName getQualifiedName(SqlBaseParser.QnameContext context)
    {
        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.parts = identsToStrings(context.ident());
        return qualifiedName;
    }

    private List<String> identsToStrings(List<SqlBaseParser.IdentContext> idents)
    {
        return ListIterate.flatCollect(idents, this::getIdentText);
    }
}
