// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.postgres.protocol.sql.serialization.SQLSerializer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AST-based FROM rewriter that uses the Legend SQL parser to parse the query,
 * then visits the tree and replaces table references with func('e2e::prefix_tableName') calls.
 * <p>
 * This correctly handles:
 * - Subqueries (doesn't rewrite subquery aliases)
 * - CTEs (doesn't rewrite CTE name references)
 * - JOINs (rewrites each table independently)
 * - Aliases (preserves them)
 * - Table-free queries (pass through as-is)
 */
public class AstFromRewriter extends SQLSerializer
{
    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

    private final String functionPrefix;
    private final Set<String> knownTables;
    private final ThreadLocal<Set<String>> cteNames = ThreadLocal.withInitial(HashSet::new);

    public AstFromRewriter(String functionPrefix, Set<String> knownTables)
    {
        this.functionPrefix = functionPrefix;
        this.knownTables = knownTables;
    }

    /**
     * Rewrites the SQL using AST-based approach.
     */
    public String rewrite(String sql)
    {
        try
        {
            cteNames.get().clear();
            SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(sql, "query");
            SqlBaseParser.SingleStatementContext tree = parser.singleStatement();
            String result = tree.accept(this);
            if (result == null)
            {
                throw new RuntimeException("AST rewriter returned null for: " + sql);
            }
            // SQLSerializer's visitSingleStatement appends ";" — strip it if original didn't have one
            if (!sql.trim().endsWith(";") && result.endsWith(";"))
            {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }
        finally
        {
            cteNames.remove();
        }
    }

    @Override
    public String visitNamedQuery(SqlBaseParser.NamedQueryContext ctx)
    {
        // Track CTE names so we don't rewrite references to them
        String cteName = ctx.name.getText().toLowerCase();
        cteNames.get().add(cteName);
        return super.visitNamedQuery(ctx);
    }

    @Override
    public String visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        String tableName = ctx.qname().accept(this);
        String normalizedName = tableName.toLowerCase().replace("\"", "");

        // Don't rewrite CTE references or unknown tables
        if (cteNames.get().contains(normalizedName))
        {
            return tableName;
        }

        // Handle schema-qualified names (e.g., public.persons -> persons)
        String simpleName = normalizedName;
        if (simpleName.contains("."))
        {
            String[] parts = simpleName.split("\\.");
            simpleName = parts[parts.length - 1];
        }

        if (knownTables.contains(simpleName))
        {
            return "func('e2e::" + functionPrefix + "_" + simpleName + "')";
        }

        return tableName;
    }

    /**
     * Checks if the SQL contains any table references that would be rewritten.
     */
    public boolean hasTableReferences(String sql)
    {
        Matcher matcher = FROM_TABLE_PATTERN.matcher(sql);
        while (matcher.find())
        {
            String tableName = matcher.group(2);
            if (knownTables.contains(tableName.toLowerCase()))
            {
                return true;
            }
        }
        return false;
    }
}
