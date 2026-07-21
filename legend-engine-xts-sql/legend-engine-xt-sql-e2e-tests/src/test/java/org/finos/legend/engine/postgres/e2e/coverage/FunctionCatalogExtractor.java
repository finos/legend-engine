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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts all built-in Postgres functions from pg_catalog.pg_proc,
 * categorized by documentation section (aligned to PostgreSQL 16 docs).
 */
public class FunctionCatalogExtractor
{
    private final DataSource dataSource;

    public FunctionCatalogExtractor(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public Map<String, List<PgFunction>> extractCatalog()
    {
        Map<String, List<PgFunction>> catalog = new LinkedHashMap<>();
        // Initialize categories in documentation order
        for (String cat : DOC_CATEGORIES)
        {
            catalog.put(cat, new ArrayList<>());
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(CATALOG_QUERY))
        {
            while (rs.next())
            {
                String name = rs.getString("function_name");
                String args = rs.getString("arguments");
                String returnType = rs.getString("return_type");
                String kind = rs.getString("kind");

                String category = classifyFunction(name, kind);
                if (category != null)
                {
                    String signature = name + "(" + (args != null ? args : "") + ") → " + returnType;
                    PgFunction fn = new PgFunction(name, signature, args, returnType, kind, category);
                    catalog.computeIfAbsent(category, k -> new ArrayList<>()).add(fn);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to extract function catalog from Postgres", e);
        }

        return catalog;
    }

    /**
     * Classifies a function into a Postgres documentation category.
     */
    private String classifyFunction(String name, String kind)
    {
        if ("a".equals(kind))
        {
            return CAT_AGGREGATE;
        }
        if ("w".equals(kind))
        {
            return CAT_WINDOW;
        }

        // Math functions
        if (MATH_FUNCTIONS.contains(name))
        {
            return CAT_MATH;
        }
        // String functions
        if (STRING_FUNCTIONS.contains(name))
        {
            return CAT_STRING;
        }
        // Date/time functions
        if (DATETIME_FUNCTIONS.contains(name))
        {
            return CAT_DATETIME;
        }
        // Pattern matching
        if (PATTERN_FUNCTIONS.contains(name))
        {
            return CAT_PATTERN;
        }
        // Data type formatting
        if (FORMAT_FUNCTIONS.contains(name))
        {
            return CAT_FORMAT;
        }
        // Conditional
        if (CONDITIONAL_FUNCTIONS.contains(name))
        {
            return CAT_CONDITIONAL;
        }
        // JSON
        if (JSON_FUNCTIONS.contains(name))
        {
            return CAT_JSON;
        }
        // Array
        if (ARRAY_FUNCTIONS.contains(name))
        {
            return CAT_ARRAY;
        }
        // Network
        if (NETWORK_FUNCTIONS.contains(name))
        {
            return CAT_NETWORK;
        }
        // System info
        if (SYSTEM_FUNCTIONS.contains(name))
        {
            return CAT_SYSTEM;
        }
        // Binary
        if (BINARY_FUNCTIONS.contains(name))
        {
            return CAT_BINARY;
        }
        // Sequence
        if (SEQUENCE_FUNCTIONS.contains(name))
        {
            return CAT_SEQUENCE;
        }
        // Set returning
        if (SET_RETURNING_FUNCTIONS.contains(name))
        {
            return CAT_SET_RETURNING;
        }

        // Only include functions that match a known documentation category.
        // Unclassified functions are internal Postgres implementation details
        // (comparison operators, geometric functions, ACL functions, binary upgrade, etc.)
        // and are excluded from the public-API catalog.
        return null;
    }

    // ============ Category Constants ============

    public static final String CAT_MATH = "Mathematical Functions and Operators (9.3)";
    public static final String CAT_STRING = "String Functions and Operators (9.4)";
    public static final String CAT_BINARY = "Binary String Functions (9.5)";
    public static final String CAT_PATTERN = "Pattern Matching (9.7)";
    public static final String CAT_FORMAT = "Data Type Formatting (9.8)";
    public static final String CAT_DATETIME = "Date/Time Functions and Operators (9.9)";
    public static final String CAT_CONDITIONAL = "Conditional Expressions (9.18)";
    public static final String CAT_JSON = "JSON Functions and Operators (9.16)";
    public static final String CAT_ARRAY = "Array Functions and Operators (9.19)";
    public static final String CAT_AGGREGATE = "Aggregate Functions (9.21)";
    public static final String CAT_WINDOW = "Window Functions (9.22)";
    public static final String CAT_NETWORK = "Network Address Functions (9.12)";
    public static final String CAT_SYSTEM = "System Information Functions (9.26)";
    public static final String CAT_SEQUENCE = "Sequence Manipulation Functions (9.17)";
    public static final String CAT_SET_RETURNING = "Set Returning Functions (9.25)";
    public static final String CAT_OTHER = "Other Functions";

    private static final String[] DOC_CATEGORIES = {
            CAT_MATH, CAT_STRING, CAT_BINARY, CAT_PATTERN, CAT_FORMAT,
            CAT_DATETIME, CAT_CONDITIONAL, CAT_JSON, CAT_ARRAY,
            CAT_AGGREGATE, CAT_WINDOW, CAT_NETWORK, CAT_SYSTEM,
            CAT_SEQUENCE, CAT_SET_RETURNING, CAT_OTHER
    };

    // ============ Function Classification Sets ============

    private static final java.util.Set<String> MATH_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "abs", "ceil", "ceiling", "floor", "round", "trunc", "truncate",
            "mod", "power", "sqrt", "cbrt", "exp", "ln", "log", "log10",
            "sign", "pi", "degrees", "radians", "factorial", "gcd", "lcm",
            "min_scale", "scale", "trim_scale", "width_bucket",
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "random", "setseed", "div"
    ));

    private static final java.util.Set<String> STRING_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "length", "char_length", "character_length", "octet_length", "bit_length",
            "upper", "lower", "initcap",
            "substring", "substr", "left", "right",
            "trim", "ltrim", "rtrim", "btrim",
            "position", "strpos",
            "replace", "translate", "overlay",
            "concat", "concat_ws",
            "lpad", "rpad", "repeat", "reverse",
            "split_part", "string_to_array", "array_to_string",
            "chr", "ascii",
            "md5", "encode", "decode",
            "format", "quote_ident", "quote_literal", "quote_nullable",
            "regexp_replace", "regexp_match", "regexp_matches",
            "regexp_split_to_table", "regexp_split_to_array",
            "starts_with", "string_agg", "normalize", "is_normalized",
            "to_hex", "unicode", "unistr"
    ));

    private static final java.util.Set<String> DATETIME_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "age", "clock_timestamp", "current_date", "current_time", "current_timestamp",
            "date_part", "date_trunc", "extract",
            "isfinite", "justify_days", "justify_hours", "justify_interval",
            "localtime", "localtimestamp",
            "make_date", "make_interval", "make_time", "make_timestamp", "make_timestamptz",
            "now", "statement_timestamp", "timeofday", "transaction_timestamp",
            "to_timestamp"
    ));

    private static final java.util.Set<String> PATTERN_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "like", "ilike", "similar_to",
            "regexp_match", "regexp_matches", "regexp_replace",
            "regexp_split_to_table", "regexp_split_to_array",
            "regexp_count", "regexp_instr", "regexp_like", "regexp_substr"
    ));

    private static final java.util.Set<String> FORMAT_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "to_char", "to_date", "to_number", "to_timestamp"
    ));

    private static final java.util.Set<String> CONDITIONAL_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "coalesce", "nullif", "greatest", "least", "num_nulls", "num_nonnulls"
    ));

    private static final java.util.Set<String> JSON_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "to_json", "to_jsonb", "array_to_json", "row_to_json",
            "json_build_array", "jsonb_build_array", "json_build_object", "jsonb_build_object",
            "json_object", "jsonb_object",
            "json_array_length", "jsonb_array_length",
            "json_each", "jsonb_each", "json_each_text", "jsonb_each_text",
            "json_extract_path", "jsonb_extract_path",
            "json_extract_path_text", "jsonb_extract_path_text",
            "json_object_keys", "jsonb_object_keys",
            "json_array_elements", "jsonb_array_elements",
            "json_array_elements_text", "jsonb_array_elements_text",
            "json_typeof", "jsonb_typeof",
            "json_strip_nulls", "jsonb_strip_nulls",
            "jsonb_set", "jsonb_insert", "jsonb_pretty",
            "json_agg", "jsonb_agg", "json_object_agg", "jsonb_object_agg",
            "jsonb_path_query", "jsonb_path_query_array", "jsonb_path_exists",
            "jsonb_path_match"
    ));

    private static final java.util.Set<String> ARRAY_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "array_agg", "array_append", "array_cat", "array_dims",
            "array_fill", "array_length", "array_lower", "array_upper",
            "array_ndims", "array_position", "array_positions",
            "array_prepend", "array_remove", "array_replace",
            "array_to_string", "string_to_array",
            "unnest", "cardinality", "array_sample", "array_shuffle"
    ));

    private static final java.util.Set<String> NETWORK_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "abbrev", "broadcast", "family", "host", "hostmask",
            "inet_merge", "inet_same_family", "masklen", "netmask",
            "network", "set_masklen", "text", "inet_client_addr",
            "inet_client_port", "inet_server_addr", "inet_server_port"
    ));

    private static final java.util.Set<String> SYSTEM_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "current_database", "current_query", "current_schema", "current_schemas",
            "current_user", "session_user", "user",
            "inet_client_addr", "inet_server_addr",
            "version", "has_table_privilege", "has_schema_privilege",
            "has_database_privilege", "has_column_privilege",
            "obj_description", "col_description", "shobj_description"
    ));

    private static final java.util.Set<String> BINARY_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "octet_length", "overlay", "get_byte", "set_byte",
            "sha224", "sha256", "sha384", "sha512",
            "md5", "encode", "decode", "convert", "convert_from", "convert_to"
    ));

    private static final java.util.Set<String> SEQUENCE_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "nextval", "currval", "setval", "lastval"
    ));

    private static final java.util.Set<String> SET_RETURNING_FUNCTIONS = new java.util.HashSet<>(java.util.Arrays.asList(
            "generate_series", "generate_subscripts", "unnest"
    ));

    /**
     * Refined catalog query that returns only PUBLIC-API Postgres functions.
     * Excludes:
     * - Functions with 'internal' argument or return type (OID 2281) — never user-callable
     * - Aggregate transition/final/combine/serialize/deserialize functions (implementation details of aggregates)
     * - Type input/output/send/receive functions (type system internals)
     * - Index/operator-class support functions (hash, btree comparison, etc.)
     * - Known internal-name patterns (*_transfn, *_finalfn, *_combinefn, etc.)
     */
    private static final String CATALOG_QUERY =
            "SELECT p.proname AS function_name, " +
                    "pg_get_function_arguments(p.oid) AS arguments, " +
                    "pg_get_function_result(p.oid) AS return_type, " +
                    "p.prokind AS kind " +
                    "FROM pg_proc p " +
                    "JOIN pg_namespace n ON p.pronamespace = n.oid " +
                    "WHERE n.nspname = 'pg_catalog' " +
                    // Basic name filters
                    "AND p.proname NOT LIKE 'pg\\_%' ESCAPE '\\' " +
                    "AND substring(p.proname, 1, 1) <> '_' " +
                    "AND p.prokind IN ('f', 'a', 'w') " +
                    // Exclude functions with 'internal' pseudo-type args (OID 2281)
                    "AND NOT EXISTS (SELECT 1 FROM unnest(p.proargtypes) AS t(oid) WHERE t.oid = 2281) " +
                    // Exclude functions returning 'internal' pseudo-type
                    "AND p.prorettype <> 2281 " +
                    // Exclude aggregate transition/final/combine/serial/deserial functions
                    "AND p.oid NOT IN ( " +
                    "    SELECT aggtransfn FROM pg_aggregate WHERE aggtransfn <> 0 " +
                    "    UNION ALL SELECT aggfinalfn FROM pg_aggregate WHERE aggfinalfn <> 0 " +
                    "    UNION ALL SELECT aggcombinefn FROM pg_aggregate WHERE aggcombinefn <> 0 " +
                    "    UNION ALL SELECT aggserialfn FROM pg_aggregate WHERE aggserialfn <> 0 " +
                    "    UNION ALL SELECT aggdeserialfn FROM pg_aggregate WHERE aggdeserialfn <> 0 " +
                    "    UNION ALL SELECT aggmtransfn FROM pg_aggregate WHERE aggmtransfn <> 0 " +
                    "    UNION ALL SELECT aggmfinalfn FROM pg_aggregate WHERE aggmfinalfn <> 0 " +
                    ") " +
                    // Exclude type input/output/send/receive functions
                    "AND p.oid NOT IN ( " +
                    "    SELECT typinput::oid FROM pg_type WHERE typinput <> 0 " +
                    "    UNION ALL SELECT typoutput::oid FROM pg_type WHERE typoutput <> 0 " +
                    "    UNION ALL SELECT typsend::oid FROM pg_type WHERE typsend <> 0 " +
                    "    UNION ALL SELECT typreceive::oid FROM pg_type WHERE typreceive <> 0 " +
                    "    UNION ALL SELECT typmodin::oid FROM pg_type WHERE typmodin <> 0 " +
                    "    UNION ALL SELECT typmodout::oid FROM pg_type WHERE typmodout <> 0 " +
                    ") " +
                    // Exclude index/operator-class support functions
                    "AND p.oid NOT IN (SELECT amproc FROM pg_amproc) " +
                    // Exclude functions returning trigger, event_trigger, language_handler, fdw_handler
                    "AND p.prorettype NOT IN (2279, 3838, 2280, 3115) " +
                    // Exclude known internal-name patterns
                    "AND p.proname !~ '(_transfn|_finalfn|_combinefn|_serializefn|_deserializefn|_accum|_inv)$' " +
                    "AND p.proname !~ '^(hash|bt).*cmp$' " +
                    "AND p.proname !~ '^(int[248]|float[48]|numeric|text|varchar|bool|date|timestamp|interval|oid|bytea|uuid|json|jsonb|xml|bit|varbit|inet|cidr|macaddr|money|time|timetz|point|line|lseg|box|path|polygon|circle|tsvector|tsquery|regtype|regclass|regproc|regoper|regnamespace|regrole|regconfig|regdictionary)(in|out|send|recv|_cmp|_eq|_ne|_lt|_le|_gt|_ge|_larger|_smaller|_sortsupport|_hash)$' " +
                    "ORDER BY p.proname, pg_get_function_arguments(p.oid)";

    /**
     * Represents a single Postgres function signature.
     */
    public static class PgFunction
    {
        public final String name;
        public final String signature;
        public final String arguments;
        public final String returnType;
        public final String kind; // f=normal, a=aggregate, w=window
        public final String category;
        public String status = "UNTESTED";
        public String tdsStatus = "UNTESTED";
        public String relStatus = "UNTESTED";
        public FunctionCoverageMapper.SignatureCoverage coverage;
        public String notes = "";

        public PgFunction(String name, String signature, String arguments, String returnType, String kind, String category)
        {
            this.name = name;
            this.signature = signature;
            this.arguments = arguments;
            this.returnType = returnType;
            this.kind = kind;
            this.category = category;
        }
    }
}







