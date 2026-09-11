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
 * Cross-references test results with the Postgres operator catalog using
 * explicit test-to-signature linkage (via {@code operator} and {@code signature}
 * fields in YAML test cases). Mirrors {@link FunctionCoverageMapper}, keyed on
 * {@code operator} instead of {@code function}.
 */
public class OperatorCoverageMapper
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Holds aggregated coverage for a single operator signature.
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
        public final List<FunctionCoverageMapper.TestResultEntry> testDetails = new ArrayList<>();

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
            if (relFail > 0)
            {
                return "FAIL";
            }
            return "ERROR";
        }
    }

    public Map<String, SignatureCoverage> mapCoverage(
            Map<String, List<OperatorCatalogExtractor.PgOperator>> catalog,
            File reportFile,
            List<TestCaseLoader.TestCase> allTestCases) throws IOException
    {
        Map<String, FunctionCoverageMapper.TestResultEntry> linkedTests = new HashMap<>();
        Map<String, SignatureCoverage> signatureCoverageMap = new HashMap<>();

        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            if (tc.operator != null && tc.signature != null)
            {
                FunctionCoverageMapper.TestResultEntry entry = new FunctionCoverageMapper.TestResultEntry(tc.id, tc.signature);
                linkedTests.put(tc.id, entry);

                SignatureCoverage cov = signatureCoverageMap.computeIfAbsent(tc.signature, k -> new SignatureCoverage());
                cov.total++;
                cov.testDetails.add(entry);

                if (tc.skip != null)
                {
                    entry.tdsState = "SKIP";
                    entry.relationState = "SKIP";
                    cov.tdsSkip++;
                    cov.relSkip++;
                }
            }
        }

        if (reportFile != null && reportFile.exists())
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

                    FunctionCoverageMapper.TestResultEntry entry = linkedTests.get(id);
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

            for (FunctionCoverageMapper.TestResultEntry entry : cov.testDetails)
            {
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

        // Inject synthetic catalog entries for tested operator signatures that don't
        // correspond to any pg_operator row (e.g. chained/composite expressions
        // like "integer + integer + integer → integer"), mirroring
        // FunctionCoverageMapper's handling of untracked extension functions.
        for (Map.Entry<String, SignatureCoverage> entry : signatureCoverageMap.entrySet())
        {
            String sig = entry.getKey();
            boolean foundInCatalog = false;
            for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
            {
                for (OperatorCatalogExtractor.PgOperator op : ops)
                {
                    if (matches(op, sig))
                    {
                        foundInCatalog = true;
                        break;
                    }
                }
                if (foundInCatalog)
                {
                    break;
                }
            }
            if (!foundInCatalog)
            {
                String opName = sig.contains("(") ? sig.substring(0, sig.indexOf('(')) : sig;
                String category = OperatorCatalogExtractor.CAT_OTHER;
                OperatorCatalogExtractor.PgOperator syntheticOp = new OperatorCatalogExtractor.PgOperator(
                        opName, sig, "-", "-", "unknown", "b", category);
                syntheticOp.memberSignatures.add(sig);
                SignatureCoverage cov = entry.getValue();
                syntheticOp.tdsStatus = cov.tdsStatus();
                syntheticOp.relStatus = cov.relStatus();
                syntheticOp.coverage = cov;
                catalog.computeIfAbsent(category, k -> new ArrayList<>()).add(syntheticOp);
            }
        }

        // Apply to catalog entries. A single catalog row may now represent several
        // concrete-type test signatures collapsed under genericSignature() (e.g. int2/int4/int8
        // all reporting as one "numeric" row), so coverage from every matching test signature is
        // merged together. Untested-but-catalogued operators stay UNTESTED, consistent with
        // FunctionCoverageMapper's default (report-time diffing surfaces these as candidates for
        // an explicit UNSUPPORTED classification later).
        for (Map.Entry<String, List<OperatorCatalogExtractor.PgOperator>> catEntry : catalog.entrySet())
        {
            for (OperatorCatalogExtractor.PgOperator op : catEntry.getValue())
            {
                SignatureCoverage merged = null;
                for (Map.Entry<String, SignatureCoverage> sigEntry : signatureCoverageMap.entrySet())
                {
                    if (!matches(op, sigEntry.getKey()))
                    {
                        continue;
                    }
                    if (merged == null)
                    {
                        merged = new SignatureCoverage();
                    }
                    SignatureCoverage c = sigEntry.getValue();
                    merged.tdsPass += c.tdsPass;
                    merged.tdsFail += c.tdsFail;
                    merged.tdsError += c.tdsError;
                    merged.tdsSkip += c.tdsSkip;
                    merged.relPass += c.relPass;
                    merged.relFail += c.relFail;
                    merged.relError += c.relError;
                    merged.relSkip += c.relSkip;
                    merged.total += c.total;
                    merged.testDetails.addAll(c.testDetails);
                }
                if (merged != null)
                {
                    op.tdsStatus = merged.tdsStatus();
                    op.relStatus = merged.relStatus();
                    op.coverage = merged;
                }
            }
        }

        return signatureCoverageMap;
    }

    /**
     * True if the test-authored {@code testSignature} refers to catalog row {@code op}, either
     * because it's an exact match, matches one of the concrete pg_operator signatures collapsed
     * into {@code op} (see {@link OperatorCatalogExtractor#genericSignature}), or normalizes
     * (via SQL-standard-name-to-type-class collapsing) to {@code op}'s generic signature.
     */
    private static boolean matches(OperatorCatalogExtractor.PgOperator op, String testSignature)
    {
        if (testSignature.equals(op.signature) || op.memberSignatures.contains(testSignature))
        {
            return true;
        }
        String normalized = OperatorCatalogExtractor.normalizeTestSignature(testSignature, op.category);
        return normalized != null && normalized.equals(op.signature);
    }
}


