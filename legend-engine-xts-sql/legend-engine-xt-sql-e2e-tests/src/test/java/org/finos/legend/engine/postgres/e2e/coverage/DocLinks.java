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
}

