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

package org.finos.legend.engine.test.emit.report;

import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestEMITCoverageReportGeneration
{
    @Test
    public void buildHTMLReturnsValidReportForEmptyDescriptorList()
    {
        String html = EMIT_to_HTML.buildHTML(Collections.emptyList(), null);

        Assertions.assertTrue(html.contains("EMIT Coverage Report"), "header must always render");
        Assertions.assertTrue(html.contains(">0</span><span class=\"stat-label\">Models<"),
                "zero-models stat-bar must render");
    }

    @Test
    public void buildHTMLRendersEachDescriptorInTheAllModelsTab()
    {
        EMITModelDescriptor a = descriptor("model-a", "Model A", Arrays.asList("grammar:class-inheritance"), "basic");
        EMITModelDescriptor b = descriptor("model-b", "Model B", Arrays.asList("mapping:mapping"), "intermediate");

        String html = EMIT_to_HTML.buildHTML(Arrays.asList(a, b), null);

        Assertions.assertTrue(html.contains(">model-a<"), "model-a must appear in the report");
        Assertions.assertTrue(html.contains(">model-b<"), "model-b must appear in the report");
        Assertions.assertTrue(html.contains(">2</span><span class=\"stat-label\">Models<"),
                "stat-bar must report the correct model count");
    }

    @Test
    public void buildHTMLDerivesResourcePathAndModuleFromSourceUrlAndRepoRoot(@TempDir Path repo) throws IOException
    {
        Path yaml = repo.resolve("moduleA/src/test/resources/emit-models/nested/sub.emit.yaml");
        Files.createDirectories(yaml.getParent());
        Files.write(yaml, new byte[0]);

        EMITModelDescriptor d = descriptor("sub", "Subtitled", Collections.emptyList(), "basic");
        d.setSource(yaml.toUri().toURL());

        String html = EMIT_to_HTML.buildHTML(Collections.singletonList(d), repo);

        Assertions.assertTrue(html.contains("moduleA/src/test/resources/emit-models/nested/sub.emit.yaml"),
                "resourcePath (derived from source URL relative to repoRoot) must appear in the YAML Path column");
        Assertions.assertTrue(html.contains(">moduleA<"),
                "module (derived from the resourcePath) must appear in the Module column");
    }

    @Test
    public void buildHTMLLeavesModuleAndPathBlankWhenRepoRootIsNull() throws MalformedURLException
    {
        EMITModelDescriptor d = descriptor("no-context", "No context", Collections.emptyList(), "basic");
        d.setSource(new URL("file:///anywhere/moduleA/src/test/resources/emit-models/x.emit.yaml"));

        String html = EMIT_to_HTML.buildHTML(Collections.singletonList(d), null);

        Assertions.assertTrue(html.contains(">no-context<"), "descriptor must still be rendered");
        Assertions.assertTrue(html.contains("<code>—</code>"),
                "null repoRoot → YAML Path column shows the placeholder");
    }

    @Test
    public void buildHTMLLeavesPathBlankWhenDescriptorHasNoSource()
    {
        EMITModelDescriptor d = descriptor("no-src", "No source", Collections.emptyList(), "basic");

        String html = EMIT_to_HTML.buildHTML(Collections.singletonList(d), Paths.get("/anywhere"));

        Assertions.assertTrue(html.contains(">no-src<"), "descriptor must still be rendered");
        Assertions.assertTrue(html.contains("<code>—</code>"),
                "missing source URL → YAML Path column shows the placeholder");
    }

    @Test
    public void buildHTMLGroupsModelsWithIdenticalFeatureSetsIntoCombinations()
    {
        List<String> sharedFeatures = Arrays.asList("grammar:class-inheritance", "mapping:mapping");
        EMITModelDescriptor a = descriptor("combo-a", "A", sharedFeatures, "basic");
        EMITModelDescriptor b = descriptor("combo-b", "B", sharedFeatures, "basic");
        EMITModelDescriptor c = descriptor("solo",    "C", Arrays.asList("store:relational-multi-table"), "basic");

        String html = EMIT_to_HTML.buildHTML(Arrays.asList(a, b, c), null);

        Assertions.assertTrue(html.contains(">2</span><span class=\"stat-label\">Unique Combinations<"),
                "stat-bar must count distinct feature combinations");
    }

    @Test
    public void buildHTMLListsUncoveredTaxonomyFeaturesAsCoverageGaps()
    {
        EMITModelDescriptor d = descriptor("m", "M", Arrays.asList("grammar:class-inheritance"), "basic");

        String html = EMIT_to_HTML.buildHTML(Collections.singletonList(d), null);

        Assertions.assertTrue(html.contains(">1</span><span class=\"stat-label\">Features Covered<"),
                "must report one covered feature");
        Assertions.assertTrue(html.contains(">service-test<"),
                "unrelated taxonomy features must appear as coverage gaps");
    }

    @Test
    public void buildHTMLRejectsNullDescriptorList()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.buildHTML(null, null),
                "null descriptor list must be rejected");
    }

    @Test
    public void writeHTMLProducesTheSameBytesAsBuildHTML(@TempDir Path tmp) throws IOException
    {
        EMITModelDescriptor d = descriptor("x", "X", Collections.emptyList(), "basic");
        List<EMITModelDescriptor> descriptors = Collections.singletonList(d);
        Path output = tmp.resolve("out.html");

        EMIT_to_HTML.writeHTML(descriptors, output, null);
        String onDisk = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);

        Assertions.assertEquals(EMIT_to_HTML.buildHTML(descriptors, null), onDisk,
                "writeHTML must persist exactly the same bytes as buildHTML returns");
    }

    @Test
    public void writeHTMLCreatesParentDirectoriesIfMissing(@TempDir Path tmp) throws IOException
    {
        EMITModelDescriptor d = descriptor("x", "X", Collections.emptyList(), "basic");
        Path output = tmp.resolve("nested/sub/dir/out.html");

        EMIT_to_HTML.writeHTML(Collections.singletonList(d), output, null);

        Assertions.assertTrue(Files.exists(output), "output file must be created");
        String written = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        Assertions.assertTrue(written.contains(">x<"), "rendered file must contain the model");
    }

    @Test
    public void writeHTMLRejectsNullArgs(@TempDir Path tmp)
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.writeHTML(null, tmp.resolve("out.html"), null),
                "null descriptors must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EMIT_to_HTML.writeHTML(Collections.emptyList(), null, null),
                "null outputFile must be rejected");
    }

    private static EMITModelDescriptor descriptor(String name, String title, List<String> features, String complexity)
    {
        return EMITModelDescriptor.newDescriptor(
                name,
                title,
                title,
                null,
                features,
                Collections.emptyList(),
                complexity,
                Collections.emptyList());
    }
}

