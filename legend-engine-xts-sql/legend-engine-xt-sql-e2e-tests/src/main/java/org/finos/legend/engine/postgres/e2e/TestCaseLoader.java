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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestCaseLoader
{
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static TestFile load(String resourcePath)
    {
        return load(resourcePath, null);
    }

    /**
     * Reads one corpus file, from {@code baseDir} when given and from the classpath otherwise.
     * {@code baseDir} is the resource root (the directory containing {@code parity-tests/}), so the
     * same relative paths work either way and there is only one list of corpus files to maintain.
     */
    public static TestFile load(String resourcePath, Path baseDir)
    {
        try (InputStream is = open(resourcePath, baseDir))
        {
            if (is == null)
            {
                throw new RuntimeException("Resource not found: " + resourcePath
                        + (baseDir == null ? "" : " (under " + baseDir + ")"));
            }
            TestFile file = YAML_MAPPER.readValue(is, TestFile.class);
            validate(file, resourcePath);
            return file;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load test file: " + resourcePath, e);
        }
    }

    private static InputStream open(String resourcePath, Path baseDir) throws IOException
    {
        if (baseDir == null)
        {
            return TestCaseLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        Path file = baseDir.resolve(resourcePath);
        return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
    }

    /**
     * Enforces that at most one of function/operator/predicate/format_token/feature
     * is set per test case, so misuse of the coverage-linkage fields fails loudly at
     * load time rather than silently in a coverage mapper.
     */
    private static void validate(TestFile file, String resourcePath)
    {
        if (file.tests == null)
        {
            return;
        }
        for (TestCase tc : file.tests)
        {
            int count = 0;
            count += tc.function != null ? 1 : 0;
            count += tc.operator != null ? 1 : 0;
            count += tc.predicate != null ? 1 : 0;
            count += tc.format_token != null ? 1 : 0;
            count += tc.feature != null ? 1 : 0;
            if (count > 1)
            {
                throw new IllegalStateException(
                        "Test case '" + tc.id + "' in " + resourcePath
                                + " sets more than one of function/operator/predicate/format_token/feature; "
                                + "at most one is allowed.");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestFile
    {
        public Schema schema;
        public List<TestCase> tests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Schema
    {
        public List<TableDef> tables;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TableDef
    {
        public String name;
        public List<ColumnDef> columns;
        public List<List<Object>> rows;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColumnDef
    {
        public String name;
        public String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestCase
    {
        public String id;
        public String sql;
        public String skip;  // reason if test should be skipped
        public Boolean join_func;  // if true, use joined model function instead of FROM rewrite
        public String function;  // function name this test covers (for coverage linking)
        public String operator;  // operator symbol this test covers, e.g. "->", "||", "=" (for operator coverage linking)
        public String predicate; // predicate keyword this test covers, e.g. "BETWEEN", "IS NULL"
        public String format_token;  // to_char/EXTRACT token this test covers, e.g. "YYYY", "isodow"
        public String signature;  // exact catalog signature this test covers
        public String feature;   // structural feature being tested (for structural parity)
        public String category;  // structural parity grouping (joins, window_frames, etc.)
        public String expected_tds_status;  // expected status for TDS path: PASS, FAIL, ERROR, SKIP, BUG
        public String expected_rel_status;  // expected status for Relation path: PASS, FAIL, ERROR, SKIP, BUG
    }
}
