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

package org.finos.legend.engine.pure.code.core.pct;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestPCTManifests extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_SOURCE_ID = "pctManifests.pure";
    private static final String ADAPTERS = "import meta::pure::test::pct::*;\n" +
                "import meta::pure::test::pct::utils::*;\n" +
            "\n" +
            "function {PCT.manifestDirectory='/test/manifests'} test::taggedAdapter<X|o>(f:Function<{->X[o]}>[1]):X[o]\n" +
            "{\n" +
            "  $f->eval();\n" +
            "}\n" +
            "\n" +
            "function test::untaggedAdapter<X|o>(f:Function<{->X[o]}>[1]):X[o]\n" +
            "{\n" +
            "  $f->eval();\n" +
            "}\n";

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_SOURCE_ID);
        runtime.compile();
    }

    @Test
    public void testTaggedAdapterResolvesEachScopeByNameOrPath()
    {
        compileTestSource(TEST_SOURCE_ID, ADAPTERS +
                "\n" +
                "function test::check():Any[*]\n" +
                "{\n" +
                "  assertEquals('/test/manifests/EssentialFunctions_manifest.json', discoverPCTManifestPath('essential', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/GrammarFunctions_manifest.json', discoverPCTManifestPath('grammar', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/StandardFunctions_manifest.json', discoverPCTManifestPath('standard', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/RelationFunctions_manifest.json', discoverPCTManifestPath('relation', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/UnclassifiedFunctions_manifest.json', discoverPCTManifestPath('unclassified', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/VariantFunctions_manifest.json', discoverPCTManifestPath('variant', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/ScenarioQuantFunctions_manifest.json', discoverPCTManifestPath('scenario_quant', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/RelationFunctions_manifest.json', discoverPCTManifestPath('/core_functions_relation/relation/functions/size.pure', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/GrammarFunctions_manifest.json', discoverPCTManifestPath('/platform/pure/grammar/functions/math/plus.pure', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/EssentialFunctions_manifest.json', discoverPCTManifestPath('EssentialFunctions', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/EssentialFunctions_manifest.json', discoverPCTManifestPath('ESSENTIAL', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/ScenarioQuantFunctions_manifest.json', discoverPCTManifestPath('ScenarioQuantFunctions', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/RelationFunctions_manifest.json', discoverPCTManifestPath('/core_functions_relation/relation/functions/variant/flatten.pure', test::taggedAdapter_Function_1__X_o_));\n" +
                "  assertEquals('/test/manifests/ScenarioQuantFunctions_manifest.json', discoverPCTManifestPath('/core_scenario_quant/scenario/quant/correlation.pure', test::taggedAdapter_Function_1__X_o_));\n" +
                "}\n");
        execute("test::check():Any[*]");
    }

    @Test
    public void testInMemoryAdapterResolvesTheScopesInterpretedManifest()
    {
        compileTestSource(TEST_SOURCE_ID, "import meta::pure::test::pct::*;\n" +
                "import meta::pure::test::pct::utils::*;\n" +
                "\n" +
                "function test::check():Any[*]\n" +
                "{\n" +
                "  assertEquals('/platform/pure/essential/pct_essential_native.json', discoverPCTManifestPath('/platform/pure/essential/collection/', testAdapterForInMemoryExecution_Function_1__X_o_));\n" +
                "  assertEquals('/platform/pure/grammar/functions/pct_grammar_native.json', discoverPCTManifestPath('grammar', testAdapterForInMemoryExecution_Function_1__X_o_));\n" +
                "  assertEquals('/core_functions_relation/core-interpreted/RelationFunctions_manifest.json', discoverPCTManifestPath('relation', testAdapterForInMemoryExecution_Function_1__X_o_));\n" +
                "  assertEquals('/core_scenario_quant/core-interpreted/ScenarioQuantFunctions_manifest.json', discoverPCTManifestPath('/core_scenario_quant/', testAdapterForInMemoryExecution_Function_1__X_o_));\n" +
                "}\n");
        execute("test::check():Any[*]");
    }

    @Test
    public void testScopeNameSelectsTheScopesTestsInBothForms()
    {
        compileTestSource(TEST_SOURCE_ID, "import meta::pure::test::pct::*;\n" +
                "import meta::pure::test::pct::utils::*;\n" +
                "\n" +
                "function test::check():Any[*]\n" +
                "{\n" +
                "  let byAdapter = runPCTTestsFor('meta::pure::functions::string::tests::reverse', 'essential', testAdapterForInMemoryExecution_Function_1__X_o_);\n" +
                "  assertEquals(1, $byAdapter.results->size());\n" +
                "  assertEquals(1, $byAdapter.passCount);\n" +
                "  let byManifest = runPCTTestsFor('meta::pure::functions::string::tests::reverse', 'EssentialFunctions', '/platform/pure/essential/pct_essential_native.json');\n" +
                "  assertEquals(1, $byManifest.results->size());\n" +
                "  assertEquals(1, $byManifest.passCount);\n" +
                "}\n");
        execute("test::check():Any[*]");
    }

    @Test
    public void testDiscoveryIsTheOnlyManifestResolver()
    {
        compileTestSource(TEST_SOURCE_ID, "function test::check():Any[*]\n" +
                "{\n" +
                "  assertEquals(meta::pure::test::pct::utils::discoverPCTManifestPath_String_1__Function_1__String_1_, meta::pure::test::pct::PCT->stereotype('manifestResolver').modelElements);\n" +
                "}\n");
        execute("test::check():Any[*]");
    }

    @Test
    public void testFilterSpanningScopesCannotIdentifyManifest()
    {
        compileTestSource(TEST_SOURCE_ID, ADAPTERS +
                "\n" +
                "function test::check():Any[*]\n" +
                "{\n" +
                "  discoverPCTManifestPath('/platform/', test::taggedAdapter_Function_1__X_o_);\n" +
                "}\n");
        Exception e = Assert.assertThrows(Exception.class, () -> execute("test::check():Any[*]"));
        assertOriginatingPureException(cannotIdentifyMessage("/platform/", "test::taggedAdapter_Function_1__X_o_"), e);
    }

    @Test
    public void testUntaggedAdapterCannotIdentifyManifest()
    {
        compileTestSource(TEST_SOURCE_ID, ADAPTERS +
                "\n" +
                "function test::check():Any[*]\n" +
                "{\n" +
                "  discoverPCTManifestPath('relation', test::untaggedAdapter_Function_1__X_o_);\n" +
                "}\n");
        Exception e = Assert.assertThrows(Exception.class, () -> execute("test::check():Any[*]"));
        assertOriginatingPureException(cannotIdentifyMessage("relation", "test::untaggedAdapter_Function_1__X_o_"), e);
    }

    private static String cannotIdentifyMessage(String filter, String adapter)
    {
        return "Cannot automatically identify manifest file to use for filter '" + filter + "' and adapter '" + adapter + "'. " +
                "The filter must name one PCT scope (essential, grammar, standard, relation, unclassified, variant or scenario_quant, in any case, e.g. EssentialFunctions or a source path), " +
                "and the adapter must carry a PCT.manifestDirectory tag; otherwise pass the manifest path explicitly.";
    }
}
