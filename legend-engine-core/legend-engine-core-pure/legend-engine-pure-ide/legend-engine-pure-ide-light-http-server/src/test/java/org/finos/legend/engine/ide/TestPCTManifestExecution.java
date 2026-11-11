// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.ide;

import org.finos.legend.pure.lsp.LegendPureSession;
import org.finos.legend.pure.lsp.RepositoryScanner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class TestPCTManifestExecution
{
    @Test
    public void lspRunsManifestByPathAndReadsSavedExclusions(@TempDir Path directory) throws Exception
    {
        Path resources = directory.resolve("src/main/resources");
        Path repository = resources.resolve("core_pct_manifest_test");
        Files.createDirectories(repository);
        Files.writeString(resources.resolve("core_pct_manifest_test.definition.json"),
                "{\"name\":\"core_pct_manifest_test\",\"pattern\":\"test::pct::manifest(::.*)?\",\"dependencies\":[\"platform\"]}");
        String manifestPath = "/core_pct_manifest_test/Functions_manifest.json";
        Path manifest = resources.resolve(manifestPath.substring(1));
        String adapter = "\"adapter\":\"meta::pure::test::pct::testAdapterForInMemoryExecution_Function_1__X_o_\"";
        Files.writeString(manifest, "{" + adapter + ",\"exclusions\":[{\"test\":\"test::pct::manifest::fails_Function_1__Boolean_1_\",\"expectedError\":\"expected failure\"}]}");
        Files.writeString(repository.resolve("tests.pure"),
                "function <<meta::pure::test::pct::PCT.test>> test::pct::manifest::fails(f:Function<Any>[1]):Boolean[1]\n" +
                        "{ assert(false, 'expected failure'); }\n" +
                        "function test::pct::manifest::run():Integer[1]\n" +
                        "{\n" +
                        "  let report = meta::pure::test::surveyor::runPCTTestsFromPath('test::pct::manifest', '/core_pct_manifest_test/', '" + manifestPath + "');\n" +
                        "  assertEquals(1, $report.results->size());\n" +
                        "  $report.failCount + $report.errorCount;\n" +
                        "}\n");

        RepositoryScanner scanner = new RepositoryScanner();
        scanner.scanRoot(directory);
        LegendPureSession session = new LegendPureSession();
        session.initialize(scanner, Collections.singleton("platform"));
        assertFailureCount(session, 0);

        Files.writeString(manifest, "{" + adapter + ",\"exclusions\":[]}");
        assertFailureCount(session, 1);
    }

    private static void assertFailureCount(LegendPureSession session, long expected)
    {
        LegendPureSession.ExecuteResult result = session.executeFunction("test::pct::manifest::run__Integer_1_");
        Assertions.assertTrue(result.isSuccess(), result.getError());
        Assertions.assertNotNull(result.getReturnValue(), result.getOutput());
        Assertions.assertEquals(expected, ((Number) result.getReturnValue().getValue()).longValue(), result.getOutput());
    }
}
