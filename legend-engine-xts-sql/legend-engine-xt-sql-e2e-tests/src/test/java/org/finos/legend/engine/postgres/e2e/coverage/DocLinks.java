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

package org.finos.legend.engine.postgres.e2e.coverage;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps report category headings (e.g. {@link FunctionCatalogExtractor#CAT_MATH}) to the
 * PostgreSQL documentation page slug that documents them, so generated reports can link
 * each row back to the doc page it's meant to mirror (gap-analysis section 3.4 / 7.6).
 */
public final class DocLinks
{
    public static final String PG_DOCS_VERSION = OperatorCatalogExtractor.PG_DOCS_VERSION;

    private static final Map<String, String> SLUGS = new HashMap<>();
    private static final Map<String, String> FILE_SLUGS = new HashMap<>();

    static
    {
        // Function categories (FunctionCatalogExtractor.CAT_*)
        SLUGS.put(FunctionCatalogExtractor.CAT_MATH, "functions-math");
        SLUGS.put(FunctionCatalogExtractor.CAT_STRING, "functions-string");
        SLUGS.put(FunctionCatalogExtractor.CAT_BINARY, "functions-binarystring");
        SLUGS.put(FunctionCatalogExtractor.CAT_PATTERN, "functions-matching");
        SLUGS.put(FunctionCatalogExtractor.CAT_FORMAT, "functions-formatting");
        SLUGS.put(FunctionCatalogExtractor.CAT_DATETIME, "functions-datetime");
        SLUGS.put(FunctionCatalogExtractor.CAT_CONDITIONAL, "functions-conditional");
        SLUGS.put(FunctionCatalogExtractor.CAT_JSON, "functions-json");
        SLUGS.put(FunctionCatalogExtractor.CAT_ARRAY, "functions-array");
        SLUGS.put(FunctionCatalogExtractor.CAT_AGGREGATE, "functions-aggregate");
        SLUGS.put(FunctionCatalogExtractor.CAT_WINDOW, "functions-window");
        SLUGS.put(FunctionCatalogExtractor.CAT_NETWORK, "functions-net");
        SLUGS.put(FunctionCatalogExtractor.CAT_SYSTEM, "functions-info");
        SLUGS.put(FunctionCatalogExtractor.CAT_SEQUENCE, "functions-sequence");
        SLUGS.put(FunctionCatalogExtractor.CAT_SET_RETURNING, "functions-srf");
        SLUGS.put(FunctionCatalogExtractor.CAT_CRYPTOGRAPHIC, "pgcrypto");

        // Operator categories (OperatorCatalogExtractor.CAT_*)
        SLUGS.put(OperatorCatalogExtractor.CAT_LOGICAL, "functions-logical");
        SLUGS.put(OperatorCatalogExtractor.CAT_COMPARISON, "functions-comparison");
        SLUGS.put(OperatorCatalogExtractor.CAT_MATH, "functions-math");
        SLUGS.put(OperatorCatalogExtractor.CAT_STRING, "functions-string");
        SLUGS.put(OperatorCatalogExtractor.CAT_BITSTRING, "functions-bitstring");
        SLUGS.put(OperatorCatalogExtractor.CAT_ARRAY, "functions-array");
        SLUGS.put(OperatorCatalogExtractor.CAT_RANGE, "rangetypes");
        SLUGS.put(OperatorCatalogExtractor.CAT_JSON, "functions-json");
        SLUGS.put(OperatorCatalogExtractor.CAT_NETWORK, "functions-net");
        SLUGS.put(OperatorCatalogExtractor.CAT_GEOMETRIC, "functions-geometry");
        SLUGS.put(OperatorCatalogExtractor.CAT_FTS, "textsearch");

        // Structural/composition categories (StructuralParityReport category keys, from the
        // `category:` field in parity-tests/structural, /window_frames and /compositions). These
        // are SQL language constructs rather than catalog functions, so they map to the PG docs
        // chapter that defines each construct instead of a functions-* reference page.
        SLUGS.put("joins", "queries-table-expressions");
        SLUGS.put("lateral_joins", "queries-table-expressions");
        SLUGS.put("where_predicates", "queries-table-expressions");
        SLUGS.put("group_by", "queries-table-expressions");
        SLUGS.put("grouping_sets", "queries-table-expressions");
        SLUGS.put("having", "queries-table-expressions");
        SLUGS.put("aliases", "queries-table-expressions");
        SLUGS.put("column_resolution", "queries-table-expressions");
        SLUGS.put("column_resolution_corpus", "queries-table-expressions");
        SLUGS.put("tablesample", "queries-table-expressions");
        SLUGS.put("values_clause", "queries-values");
        SLUGS.put("distinct", "queries-select-lists");
        SLUGS.put("select_star", "queries-select-lists");
        SLUGS.put("order_limit_offset", "queries-limit");
        SLUGS.put("fetch_with_ties", "sql-select");
        SLUGS.put("set_operations", "queries-union");
        SLUGS.put("ctes", "queries-with");
        SLUGS.put("recursive_ctes", "queries-with");
        SLUGS.put("subqueries", "functions-subquery");
        SLUGS.put("case_expressions", "functions-conditional");
        SLUGS.put("boolean_logic", "functions-logical");
        SLUGS.put("null_semantics", "functions-comparison");
        SLUGS.put("type_casting", "sql-expressions");
        SLUGS.put("filter_clause", "sql-expressions");
        SLUGS.put("window_frames", "sql-expressions");
        SLUGS.put("window_partitioning", "sql-expressions");
        SLUGS.put("within_group", "functions-aggregate");
        SLUGS.put("json_operators", "functions-json");
        SLUGS.put("interval_arithmetic", "functions-datetime");
        SLUGS.put("multiple_schemas", "ddl-schemas");
        SLUGS.put("compositions", "queries");

        // Short page-tree slugs for the parity site (plan §3) ? deliberately distinct from
        // the PG docs URL slugs above, e.g. "functions-string" (docs) vs "string" (our file).
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_MATH, "math");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_STRING, "string");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_BINARY, "binary");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_PATTERN, "pattern-matching");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_FORMAT, "formatting");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_DATETIME, "datetime");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_CONDITIONAL, "conditional");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_JSON, "json");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_ARRAY, "array");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_AGGREGATE, "aggregate");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_WINDOW, "window");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_NETWORK, "network");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_SYSTEM, "system");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_SEQUENCE, "sequence");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_SET_RETURNING, "set-returning");
        FILE_SLUGS.put(FunctionCatalogExtractor.CAT_CRYPTOGRAPHIC, "cryptographic");

        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_LOGICAL, "logical");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_COMPARISON, "comparison");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_MATH, "math");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_STRING, "string");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_BITSTRING, "bitstring");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_ARRAY, "array");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_RANGE, "range");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_JSON, "json");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_NETWORK, "network");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_GEOMETRIC, "geometric");
        FILE_SLUGS.put(OperatorCatalogExtractor.CAT_FTS, "fts");
    }

    private DocLinks()
    {
    }

    /**
     * Returns a full URL to the PostgreSQL docs page for the given category heading,
     * or {@code null} if no mapping is known (e.g. "Other Functions"/"Other Operators").
     */
    public static String urlFor(String category)
    {
        String slug = SLUGS.get(category);
        if (slug == null)
        {
            return null;
        }
        return "https://www.postgresql.org/docs/" + PG_DOCS_VERSION + "/" + slug + ".html";
    }

    /**
     * Renders a category heading as a markdown link to its docs page, falling back to
     * plain text when no mapping exists.
     */
    public static String linkedHeading(String category)
    {
        String url = urlFor(category);
        return url == null ? category : "[" + category + "](" + url + ")";
    }

    /**
     * Returns a filesystem/URL-safe slug for the given category heading, suitable as an
     * HTML page basename. Uses the site's short page-tree name when known (plan &sect;3,
     * e.g. "String Functions and Operators (9.4)" -&gt; "string"); otherwise derives one
     * from the heading text (e.g. "Other Functions" -&gt; "other").
     */
    public static String slugFor(String category)
    {
        String slug = FILE_SLUGS.get(category);
        if (slug != null)
        {
            return slug;
        }
        String base = category.replaceAll("\\s*\\([^)]*\\)\\s*$", "");
        base = base.replaceAll("(?i)^(Functions and Operators|Operators|Functions)$", "$1");
        return base.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

