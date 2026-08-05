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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.postgres.e2e.TestCaseLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-references test results with the Postgres function catalog using
 * explicit test-to-signature linkage (via {@code function} and {@code signature}
 * fields in YAML test cases).
 *
 * <p>Coverage status per signature is derived purely from actual test results:
 * <ul>
 *   <li>PASS — all linked tests pass</li>
 *   <li>PARTIAL — some linked tests pass</li>
 *   <li>FAIL — no linked tests pass (all fail or error)</li>
 *   <li>UNTESTED — no tests linked to this signature</li>
 *   <li>NOT_APPLICABLE — unsupported type category (binary, network)</li>
 * </ul>
 */
public class FunctionCoverageMapper
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Holds per-test results for both TDS and Relation paths.
     */
    public static class TestResultEntry
    {
        public final String testId;
        public final String signature;
        public String tdsState;
        public String relationState;

        public TestResultEntry(String testId, String signature)
        {
            this.testId = testId;
            this.signature = signature;
        }
    }

    /**
     * Holds aggregated coverage for a single function signature.
     */
    public static class SignatureCoverage
    {
        public int tdsPass;
        public int tdsFail;
        public int tdsError;
        public int tdsSkip;
        public int relPass;
        public int relFail;
        public int relError;
        public int relSkip;
        public int total;
        public final List<TestResultEntry> testDetails = new ArrayList<>();

        public String tdsStatus()
        {
            if (total == 0)
            {
                return "UNTESTED";
            }
            int effective = total - tdsSkip;
            if (effective == 0)
            {
                return "NOT_APPLICABLE";
            }
            if (tdsPass == effective)
            {
                return "PASS";
            }
            if (tdsPass > 0)
            {
                return "PARTIAL";
            }
            // None pass: distinguish ERROR (server exception) from FAIL (result mismatch)
            if (tdsFail > 0)
            {
                return "FAIL";
            }
            return "ERROR";
        }

        public String relStatus()
        {
            if (total == 0)
            {
                return "UNTESTED";
            }
            int effective = total - relSkip;
            if (effective == 0)
            {
                return "NOT_APPLICABLE";
            }
            if (relPass == effective)
            {
                return "PASS";
            }
            if (relPass > 0)
            {
                return "PARTIAL";
            }
            // None pass: distinguish ERROR (server exception) from FAIL (result mismatch)
            if (relFail > 0)
            {
                return "FAIL";
            }
            return "ERROR";
        }
    }

    /**
     * Maps test results to function coverage using explicit test-to-signature linkage.
     *
     * @param catalog       Function catalog from FunctionCatalogExtractor
     * @param reportFile    Path to parity-report.json
     * @param allTestCases  All loaded test cases (with function/signature fields)
     * @return Map of signature string to SignatureCoverage
     */
    public Map<String, SignatureCoverage> mapCoverage(
            Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog,
            File reportFile,
            List<TestCaseLoader.TestCase> allTestCases) throws IOException
    {
        // Step 1: Build map of testId to TestResultEntry for tests that have signature linkage
        Map<String, TestResultEntry> linkedTests = new HashMap<>();
        Map<String, SignatureCoverage> signatureCoverageMap = new HashMap<>();

        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            if (tc.function != null && tc.signature != null)
            {
                TestResultEntry entry = new TestResultEntry(tc.id, tc.signature);
                linkedTests.put(tc.id, entry);

                SignatureCoverage cov = signatureCoverageMap.computeIfAbsent(
                        tc.signature, k -> new SignatureCoverage());
                cov.total++;
                cov.testDetails.add(entry);

                // If test is skipped, mark it now
                if (tc.skip != null)
                {
                    entry.tdsState = "SKIP";
                    entry.relationState = "SKIP";
                    cov.tdsSkip++;
                    cov.relSkip++;
                }
            }
        }

        // Step 2: Read actual test results from parity-report.json
        if (reportFile.exists())
        {
            JsonNode report = MAPPER.readTree(reportFile);
            JsonNode results = report.get("results");
            if (results != null)
            {
                for (JsonNode result : results)
                {
                    String id = result.has("id") ? result.get("id").asText() : "";
                    String state = result.has("state") ? result.get("state").asText() : "";
                    String path = result.has("path") ? result.get("path").asText() : "";

                    TestResultEntry entry = linkedTests.get(id);
                    if (entry != null)
                    {
                        if ("TDS".equals(path) && entry.tdsState == null)
                        {
                            entry.tdsState = state;
                        }
                        else if ("Relation".equals(path) && entry.relationState == null)
                        {
                            entry.relationState = state;
                        }
                    }
                }
            }
        }

        // Step 3: Tally results per signature
        for (SignatureCoverage cov : signatureCoverageMap.values())
        {
            cov.tdsPass = 0;
            cov.tdsFail = 0;
            cov.tdsError = 0;
            cov.relPass = 0;
            cov.relFail = 0;
            cov.relError = 0;
            int skipTds = 0;
            int skipRel = 0;

            for (TestResultEntry entry : cov.testDetails)
            {
                // TDS
                if ("PASS".equals(entry.tdsState))
                {
                    cov.tdsPass++;
                }
                else if ("FAIL".equals(entry.tdsState))
                {
                    cov.tdsFail++;
                }
                else if ("ERROR".equals(entry.tdsState) || "BUG".equals(entry.tdsState))
                {
                    cov.tdsError++;
                }
                else
                {
                    skipTds++;
                }

                // Relation
                if ("PASS".equals(entry.relationState))
                {
                    cov.relPass++;
                }
                else if ("FAIL".equals(entry.relationState))
                {
                    cov.relFail++;
                }
                else if ("ERROR".equals(entry.relationState) || "BUG".equals(entry.relationState))
                {
                    cov.relError++;
                }
                else
                {
                    skipRel++;
                }
            }
            cov.tdsSkip = skipTds;
            cov.relSkip = skipRel;
        }

        // Step 4: Apply to catalog entries
        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> catEntry : catalog.entrySet())
        {
            String categoryName = catEntry.getKey();
            boolean isUnsupported = FunctionCatalogExtractor.CAT_SYSTEM.equals(categoryName)
                    || FunctionCatalogExtractor.CAT_NETWORK.equals(categoryName)
                    || FunctionCatalogExtractor.CAT_BINARY.equals(categoryName)
                    || FunctionCatalogExtractor.CAT_SEQUENCE.equals(categoryName)
                    || FunctionCatalogExtractor.CAT_SET_RETURNING.equals(categoryName);

            for (FunctionCatalogExtractor.PgFunction fn : catEntry.getValue())
            {
                SignatureCoverage cov = signatureCoverageMap.get(fn.signature);
                if (cov != null)
                {
                    fn.tdsStatus = cov.tdsStatus();
                    fn.relStatus = cov.relStatus();
                    fn.coverage = cov;
                }
                else if (isUnsupported)
                {
                    fn.tdsStatus = "NOT_APPLICABLE";
                    fn.relStatus = "NOT_APPLICABLE";
                    fn.notes = "Unsupported category (system/network/binary)";
                }
                // else stays UNTESTED (default)
            }
        }

        return signatureCoverageMap;
    }
}
