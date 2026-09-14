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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts all built-in Postgres operators from pg_catalog.pg_operator,
 * categorized by documentation section (aligned to PostgreSQL 16 docs — see
 * {@code TestPostgresParity}'s Testcontainers image tag, {@code postgres:16-alpine}).
 *
 * <p>Logical operators (AND/OR/NOT) are parser keywords, not catalog rows in
 * {@code pg_operator}, so they are injected as synthetic entries.
 */
public class OperatorCatalogExtractor
{
    public static final String PG_DOCS_VERSION = "16";

    private final DataSource dataSource;

    public OperatorCatalogExtractor(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public Map<String, List<PgOperator>> extractCatalog()
    {
        Map<String, List<PgOperator>> catalog = new LinkedHashMap<>();
        Map<String, Map<String, PgOperator>> canonicalByCategory = new LinkedHashMap<>();
        for (String cat : DOC_CATEGORIES)
        {
            catalog.put(cat, new ArrayList<>());
            canonicalByCategory.put(cat, new LinkedHashMap<>());
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(CATALOG_QUERY))
        {
            while (rs.next())
            {
                String name = rs.getString("operator_name");
                String leftType = rs.getString("left_type");
                String rightType = rs.getString("right_type");
                String resultType = rs.getString("result_type");
                String kind = rs.getString("kind");

                String category = classify(name, leftType, rightType, resultType);
                String concreteSignature = signature(name, leftType, rightType, resultType);
                String canonicalSignature = genericSignature(name, leftType, rightType, resultType, category);

                Map<String, PgOperator> byCanonical = canonicalByCategory.get(category);
                PgOperator op = byCanonical.get(canonicalSignature);
                if (op == null)
                {
                    op = new PgOperator(name, canonicalSignature, leftType, rightType, resultType, kind, category);
                    byCanonical.put(canonicalSignature, op);
                    catalog.get(category).add(op);
                }
                op.memberSignatures.add(concreteSignature);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to extract operator catalog from Postgres", e);
        }

        // Logical operators are parser-level keywords, not pg_operator rows.
        for (String logicalOp : Arrays.asList("AND", "OR", "NOT"))
        {
            String signature = logicalOp + "(boolean, boolean) → boolean";
            PgOperator op = new PgOperator(logicalOp, signature, "bool", "bool", "bool", "b", CAT_LOGICAL);
            op.memberSignatures.add(signature);
            catalog.get(CAT_LOGICAL).add(op);
        }

        return catalog;
    }

    /**
     * Concrete operator signature string: {@code name(leftType, rightType) → resultType}, using
     * Postgres's exact catalog type names. Prefix/postfix operators use "-" for the missing
     * operand type.
     */
    public static String signature(String name, String leftType, String rightType, String resultType)
    {
        return name + "(" + leftType + ", " + rightType + ") → " + resultType;
    }

    /**
     * Signature string used for grouping/reporting: identical to {@link #signature} except that,
     * for categories where the Postgres docs describe one operator entry regardless of the
     * specific operand type (Mathematical and Comparison operators — see e.g.
     * <a href="https://www.postgresql.org/docs/16/functions-math.html">functions-math.html</a>),
     * operand/result types are collapsed to a broad type class (numeric/string/datetime/boolean)
     * so that {@code <(int2, int2)}, {@code <(int4, int8)}, etc. all report as a single row.
     */
    public static String genericSignature(String name, String leftType, String rightType, String resultType, String category)
    {
        if (CAT_MATH.equals(category) || CAT_COMPARISON.equals(category))
        {
            String left = "-".equals(leftType) ? "-" : genericType(leftType);
            String right = "-".equals(rightType) ? "-" : genericType(rightType);
            String result = CAT_COMPARISON.equals(category) ? "boolean" : genericType(resultType);
            return name + "(" + left + ", " + right + ") → " + result;
        }
        return signature(name, leftType, rightType, resultType);
    }

    /**
     * Parses a hand-authored test {@code signature} string (e.g. {@code "+(integer, integer) → integer"})
     * and re-renders it using the same type-class collapsing as {@link #genericSignature}, so that
     * test signatures written with SQL-standard type names (e.g. {@code integer}, {@code boolean})
     * can be matched against catalog rows built from Postgres's internal type names (e.g. {@code int4}).
     * Returns {@code null} if the category isn't collapsed, or the string doesn't parse.
     */
    public static String normalizeTestSignature(String rawSignature, String category)
    {
        if (!(CAT_MATH.equals(category) || CAT_COMPARISON.equals(category)) || rawSignature == null)
        {
            return null;
        }
        Matcher m = SIGNATURE_PATTERN.matcher(rawSignature.trim());
        if (!m.matches())
        {
            return null;
        }
        String name = m.group(1).trim();
        String[] args = m.group(2).trim().isEmpty() ? new String[0] : m.group(2).split(",");
        String left = args.length > 0 ? args[0].trim() : "-";
        String right = args.length > 1 ? args[1].trim() : "-";
        String result = m.group(3).trim();
        return genericSignature(name, left, right, result, category);
    }

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("^(.+?)\\(([^)]*)\\)\\s*→\\s*(.+)$");

    /**
     * Collapses a concrete Postgres/SQL-standard type name to a broad type class used to merge
     * per-type operator overloads into a single doc-aligned row.
     */
    public static String genericType(String type)
    {
        if (type == null || "-".equals(type))
        {
            return "-";
        }
        String t = type.toLowerCase();
        if (NUMERIC_TYPES.contains(t) || NUMERIC_TYPE_SYNONYMS.contains(t))
        {
            return "numeric";
        }
        if (STRING_TYPES.contains(t) || STRING_TYPE_SYNONYMS.contains(t))
        {
            return "string";
        }
        if ("bool".equals(t) || "boolean".equals(t))
        {
            return "boolean";
        }
        if (DATETIME_TYPES.contains(t) || DATETIME_TYPE_SYNONYMS.contains(t))
        {
            return "datetime";
        }
        return type;
    }

    private static String classify(String name, String leftType, String rightType, String resultType)
    {
        Set<String> types = new HashSet<>(Arrays.asList(leftType, rightType));

        if (containsAny(types, ARRAY_TYPE_MARKERS))
        {
            return CAT_ARRAY;
        }
        if (containsAny(types, RANGE_TYPES))
        {
            return CAT_RANGE;
        }
        if (containsAny(types, JSON_TYPES))
        {
            return CAT_JSON;
        }
        if (containsAny(types, NETWORK_TYPES))
        {
            return CAT_NETWORK;
        }
        if (containsAny(types, GEOMETRIC_TYPES))
        {
            return CAT_GEOMETRIC;
        }
        if (containsAny(types, FTS_TYPES))
        {
            return CAT_FTS;
        }
        if (containsAny(types, BITSTRING_TYPES))
        {
            return CAT_BITSTRING;
        }
        if ("bool".equals(resultType) && COMPARISON_OPS.contains(name)
                && (containsAny(types, NUMERIC_TYPES) || containsAny(types, STRING_TYPES)
                || containsAny(types, DATETIME_TYPES) || types.contains("bool")))
        {
            return CAT_COMPARISON;
        }
        if (containsAny(types, STRING_TYPES))
        {
            return CAT_STRING;
        }
        if (containsAny(types, NUMERIC_TYPES))
        {
            return CAT_MATH;
        }
        return CAT_OTHER;
    }

    private static boolean containsAny(Set<String> types, Set<String> candidates)
    {
        for (String t : types)
        {
            if (candidates.contains(t))
            {
                return true;
            }
        }
        return false;
    }

    // ============ Category Constants (aligned to PG docs ch. 9) ============

    public static final String CAT_LOGICAL = "Logical Operators (9.1)";
    public static final String CAT_COMPARISON = "Comparison Operators (9.2)";
    public static final String CAT_MATH = "Mathematical Operators (9.3)";
    public static final String CAT_STRING = "String Operators (9.4)";
    public static final String CAT_BITSTRING = "Bit String Operators (9.6)";
    public static final String CAT_ARRAY = "Array Operators (9.19)";
    public static final String CAT_RANGE = "Range/Multirange Operators (9.19)";
    public static final String CAT_JSON = "JSON/JSONB Operators (9.16)";
    public static final String CAT_NETWORK = "Network Address Operators (9.12)";
    public static final String CAT_GEOMETRIC = "Geometric Operators (9.11)";
    public static final String CAT_FTS = "Full Text Search Operators (9.13)";
    public static final String CAT_OTHER = "Other Operators";

    private static final String[] DOC_CATEGORIES = {
            CAT_LOGICAL, CAT_COMPARISON, CAT_MATH, CAT_STRING, CAT_BITSTRING,
            CAT_ARRAY, CAT_RANGE, CAT_JSON, CAT_NETWORK, CAT_GEOMETRIC, CAT_FTS, CAT_OTHER
    };

    private static final Set<String> COMPARISON_OPS = new HashSet<>(Arrays.asList("=", "<>", "!=", "<", "<=", ">", ">="));

    private static final Set<String> NUMERIC_TYPES = new HashSet<>(Arrays.asList(
            "int2", "int4", "int8", "float4", "float8", "numeric", "money"));

    private static final Set<String> NUMERIC_TYPE_SYNONYMS = new HashSet<>(Arrays.asList(
            "smallint", "integer", "int", "bigint", "real", "double precision", "decimal"));

    private static final Set<String> STRING_TYPES = new HashSet<>(Arrays.asList(
            "text", "varchar", "bpchar", "char", "name"));

    private static final Set<String> STRING_TYPE_SYNONYMS = new HashSet<>(Arrays.asList(
            "character varying", "character", "string", "\"char\""));

    private static final Set<String> DATETIME_TYPES = new HashSet<>(Arrays.asList(
            "date", "time", "timetz", "timestamp", "timestamptz", "interval"));

    private static final Set<String> DATETIME_TYPE_SYNONYMS = new HashSet<>(Arrays.asList(
            "time without time zone", "time with time zone",
            "timestamp without time zone", "timestamp with time zone"));

    private static final Set<String> BITSTRING_TYPES = new HashSet<>(Arrays.asList("bit", "varbit"));

    private static final Set<String> ARRAY_TYPE_MARKERS = new HashSet<>(Arrays.asList("anyarray"));

    private static final Set<String> RANGE_TYPES = new HashSet<>(Arrays.asList(
            "int4range", "int8range", "numrange", "tsrange", "tstzrange", "daterange",
            "int4multirange", "int8multirange", "nummultirange", "tsmultirange", "tstzmultirange", "datemultirange",
            "anyrange", "anymultirange"));

    private static final Set<String> JSON_TYPES = new HashSet<>(Arrays.asList("json", "jsonb", "jsonpath"));

    private static final Set<String> NETWORK_TYPES = new HashSet<>(Arrays.asList("inet", "cidr", "macaddr", "macaddr8"));

    private static final Set<String> GEOMETRIC_TYPES = new HashSet<>(Arrays.asList(
            "point", "line", "lseg", "box", "path", "polygon", "circle"));

    private static final Set<String> FTS_TYPES = new HashSet<>(Arrays.asList("tsvector", "tsquery"));

    /**
     * Returns only PUBLIC-API operators registered under pg_catalog. Array types are
     * detected via the "[]" suffix on typname's alias form, so we use typcategory
     * where practical; here we keep the raw base typname and additionally special-case
     * arrays by checking typcategory = 'A' server-side and reporting them as "anyarray".
     */
    private static final String CATALOG_QUERY =
            "SELECT o.oprname AS operator_name, "
                    + "CASE WHEN tl.typcategory = 'A' THEN 'anyarray' "
                    + "     WHEN tl.typcategory = 'R' THEN COALESCE(tl.typname, 'anyrange') "
                    + "     ELSE COALESCE(tl.typname, '-') END AS left_type, "
                    + "CASE WHEN tr.typcategory = 'A' THEN 'anyarray' "
                    + "     WHEN tr.typcategory = 'R' THEN COALESCE(tr.typname, 'anyrange') "
                    + "     ELSE COALESCE(tr.typname, '-') END AS right_type, "
                    + "tres.typname AS result_type, "
                    + "o.oprkind AS kind "
                    + "FROM pg_operator o "
                    + "JOIN pg_namespace n ON o.oprnamespace = n.oid "
                    + "LEFT JOIN pg_type tl ON o.oprleft = tl.oid "
                    + "LEFT JOIN pg_type tr ON o.oprright = tr.oid "
                    + "LEFT JOIN pg_type tres ON o.oprresult = tres.oid "
                    + "WHERE n.nspname = 'pg_catalog' "
                    + "AND o.oprcode != 0 "
                    + "ORDER BY o.oprname, left_type, right_type";

    /**
     * Represents a single Postgres operator signature.
     */
    public static class PgOperator
    {
        public final String name;
        public final String signature;
        public final String leftType;
        public final String rightType;
        public final String resultType;
        public final String kind; // b=binary(infix), l=prefix, r=postfix
        public final String category;
        public final List<String> memberSignatures = new ArrayList<>();
        public String tdsStatus = "UNTESTED";
        public String relStatus = "UNTESTED";
        public OperatorCoverageMapper.SignatureCoverage coverage;
        public String notes = "";

        public PgOperator(String name, String signature, String leftType, String rightType,
                           String resultType, String kind, String category)
        {
            this.name = name;
            this.signature = signature;
            this.leftType = leftType;
            this.rightType = rightType;
            this.resultType = resultType;
            this.kind = kind;
            this.category = category;
        }
    }
}

