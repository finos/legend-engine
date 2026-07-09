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

package org.finos.legend.engine.test.emit.maven;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestEMITCoverageReportGenerationMojo
{
    private static final String GOAL = "generate-EMIT-coverage-report";
    private static final String PLUGIN_GROUP_ID = "org.finos.legend.engine";
    private static final String PLUGIN_ARTIFACT_ID = "legend-engine-emit-maven-plugin";
    private static final String DEFAULT_INCLUDED_SUBPATH = "src/test/resources/emit-models";

    @ClassRule
    public static TemporaryFolder TMP_FOLDER = new TemporaryFolder();

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void mojoIsRegisteredWithPluginDescriptor() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "project", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        Object mojo = this.mojoRule.lookupConfiguredMojo(mavenProject, GOAL);
        Assert.assertNotNull("Mojo should be discoverable via the generated plugin descriptor", mojo);
        Assert.assertTrue(
                "Expected an EMITCoverageReportGenerationMojo, got " + mojo.getClass().getName(),
                mojo instanceof EMITCoverageReportGenerationMojo);
    }

    @Test
    public void producesReportWhenNoEmitModelsExist() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "empty", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue("Report file should be written even when no models are found",
                getExpectedOutputFile(mavenProject).exists());
        Assert.assertTrue("No descriptors should be discovered when the project is empty",
                discoverDescriptors(mojo, projectDir).isEmpty());
    }

    @Test
    public void discoversSingleEmitModelInDefaultLocation() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "single-model", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/alpha.emit.yaml", "alpha-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "alpha-model");
    }

    @Test
    public void discoversMultipleModelsAcrossSiblingModules() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "multi-module", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/alpha.emit.yaml", "alpha-model");
        touchYaml(projectDir, "moduleB/" + DEFAULT_INCLUDED_SUBPATH + "/beta.emit.yaml", "beta-model");
        touchYaml(projectDir, "moduleC/nested/" + DEFAULT_INCLUDED_SUBPATH + "/gamma.emit.yaml", "gamma-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir),
                "alpha-model", "beta-model", "gamma-model");
    }

    @Test
    public void discoversAllYamlsInsideSameEmitModelsDir() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "multi-yaml", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);
        String base = "moduleA/" + DEFAULT_INCLUDED_SUBPATH;
        touchYaml(projectDir, base + "/first.emit.yaml", "first-model");
        touchYaml(projectDir, base + "/nested/second.emit.yaml", "second-model");
        touchYaml(projectDir, base + "/nested/deeper/third.emit.yaml", "third-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir),
                "first-model", "second-model", "third-model");
    }

    @Test
    public void defaultExclusionsPruneTargetSubtrees() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "target-pruned", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, "moduleA/target/" + DEFAULT_INCLUDED_SUBPATH + "/dropped.emit.yaml", "dropped-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void defaultExclusionsPruneHiddenDirectories() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "hidden-pruned", "org.finos.test", "test-project", "1.0.0",
                null, null, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, ".git/" + DEFAULT_INCLUDED_SUBPATH + "/dropped.emit.yaml", "dropped-model");
        touchYaml(projectDir, ".idea/nested/" + DEFAULT_INCLUDED_SUBPATH + "/also-dropped.emit.yaml", "also-dropped-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void defaultExclusionsPruneSrcMainSubtrees() throws Exception
    {
        List<String> included = Lists.fixedSize.with("emit-models");
        File projectDir = buildSingleModuleProject(
                "srcmain-pruned", "org.finos.test", "test-project", "1.0.0",
                null, included, null, null, null);
        touchYaml(projectDir, "moduleA/src/test/emit-models/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, "moduleA/src/main/emit-models/dropped.emit.yaml", "dropped-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void customIncludedRelativeSubpathsChangeDiscoveryLocation() throws Exception
    {
        List<String> included = Lists.fixedSize.with("shared/emit-catalogue");
        File projectDir = buildSingleModuleProject(
                "custom-inclusion", "org.finos.test", "test-project", "1.0.0",
                null, included, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/default-loc.emit.yaml", "default-loc-model");
        touchYaml(projectDir, "moduleA/shared/emit-catalogue/custom.emit.yaml", "custom-loc-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "custom-loc-model");
    }

    @Test
    public void customExcludedDirectoryNamesArePruned() throws Exception
    {
        Set<String> excludedNames = Sets.fixedSize.with("target", "generated-sources", "vendored");
        File projectDir = buildSingleModuleProject(
                "custom-name-excl", "org.finos.test", "test-project", "1.0.0",
                null, null, excludedNames, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, "moduleA/vendored/" + DEFAULT_INCLUDED_SUBPATH + "/dropped.emit.yaml", "dropped-model");
        touchYaml(projectDir, "moduleB/generated-sources/deep/" + DEFAULT_INCLUDED_SUBPATH + "/also-dropped.emit.yaml", "also-dropped-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void customExcludedDirectoryNamePrefixesArePruned() throws Exception
    {
        List<String> excludedPrefixes = Lists.fixedSize.with(".", "_");
        File projectDir = buildSingleModuleProject(
                "custom-prefix-excl", "org.finos.test", "test-project", "1.0.0",
                null, null, null, excludedPrefixes, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, ".hidden/" + DEFAULT_INCLUDED_SUBPATH + "/dropped-dot.emit.yaml", "dropped-dot-model");
        touchYaml(projectDir, "_scratch/" + DEFAULT_INCLUDED_SUBPATH + "/dropped-underscore.emit.yaml", "dropped-underscore-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void customExcludedRelativeSubpathsArePruned() throws Exception
    {
        List<String> included = Lists.fixedSize.with("emit-models");
        List<String> excludedSubpaths = Lists.fixedSize.with("legacy/deprecated");
        File projectDir = buildSingleModuleProject(
                "custom-subpath-excl", "org.finos.test", "test-project", "1.0.0",
                null, included, null, null, excludedSubpaths);
        touchYaml(projectDir, "moduleA/emit-models/kept.emit.yaml", "kept-model");
        touchYaml(projectDir, "moduleA/legacy/deprecated/emit-models/dropped.emit.yaml", "dropped-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "kept-model");
    }

    @Test
    public void relaxingDefaultExclusionsAllowsPreviouslyHiddenModels() throws Exception
    {
        File projectDir = buildSingleModuleProject(
                "relaxed-excl", "org.finos.test", "test-project", "1.0.0",
                null, null, Sets.fixedSize.empty(), null, null);
        touchYaml(projectDir, "moduleA/target/" + DEFAULT_INCLUDED_SUBPATH + "/nowKept.emit.yaml", "now-kept-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue(getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "now-kept-model");
    }

    @Test
    public void customOutputFilePathIsHonored() throws Exception
    {
        File customOutputFile = new File(TMP_FOLDER.newFolder(), "custom/dir/coverage.html");
        File projectDir = buildSingleModuleProject(
                "custom-output", "org.finos.test", "test-project", "1.0.0",
                customOutputFile, null, null, null, null);
        touchYaml(projectDir, "moduleA/" + DEFAULT_INCLUDED_SUBPATH + "/alpha.emit.yaml", "alpha-model");

        MavenProject mavenProject = this.mojoRule.readMavenProject(projectDir);
        EMITCoverageReportGenerationMojo mojo = executeMojo(mavenProject);

        Assert.assertTrue("Report should be written to the explicitly-configured outputFilePath",
                customOutputFile.exists());
        Assert.assertFalse("Default output location should not be used when overridden",
                getExpectedOutputFile(mavenProject).exists());
        assertDescriptorNamesEqual(discoverDescriptors(mojo, projectDir), "alpha-model");
    }

    @Test
    public void invalidPathInIncludedRelativeSubpathsThrowsWithParameterAndValueInMessage() throws Exception
    {
        Path repoRoot = TMP_FOLDER.newFolder().toPath();
        String invalidSubpath = "src/main\u0000/emit-models";
        List<String> included = Arrays.asList(DEFAULT_INCLUDED_SUBPATH, invalidSubpath);
        try
        {
            EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repoRoot, included, null, null, null);
            Assert.fail("Expected IllegalArgumentException for an invalid path entry");
        }
        catch (IllegalArgumentException e)
        {
            String msg = e.getMessage();
            Assert.assertTrue("Message should name the offending parameter; got: " + msg,
                    msg.contains("includedRelativeSubpaths"));
            Assert.assertTrue("Message should reference the offending index; got: " + msg,
                    msg.contains("index 1"));
            Assert.assertTrue("Message should include the offending value; got: " + msg,
                    msg.contains(invalidSubpath));
            Assert.assertNotNull("Underlying InvalidPathException should be preserved as cause", e.getCause());
            Assert.assertTrue("Cause should be InvalidPathException; got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof InvalidPathException);
        }
    }

    @Test
    public void invalidPathInExcludedRelativeSubpathsThrowsWithParameterAndValueInMessage() throws Exception
    {
        Path repoRoot = TMP_FOLDER.newFolder().toPath();
        String invalidSubpath = "legacy\u0000/deprecated";
        List<String> excluded = Arrays.asList("legacy/deprecated", invalidSubpath);
        try
        {
            EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repoRoot, Lists.fixedSize.with(DEFAULT_INCLUDED_SUBPATH), null, null, excluded);
            Assert.fail("Expected IllegalArgumentException for an invalid path entry");
        }
        catch (IllegalArgumentException e)
        {
            String msg = e.getMessage();
            Assert.assertTrue("Message should name the offending parameter; got: " + msg,
                    msg.contains("excludedRelativeSubpaths"));
            Assert.assertTrue("Message should reference the offending index; got: " + msg,
                    msg.contains("index 1"));
            Assert.assertTrue("Message should include the offending value; got: " + msg,
                    msg.contains(invalidSubpath));
            Assert.assertNotNull("Underlying InvalidPathException should be preserved as cause", e.getCause());
            Assert.assertTrue("Cause should be InvalidPathException; got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof InvalidPathException);
        }
    }

    @Test
    public void nullEntryInIncludedRelativeSubpathsThrowsWithParameterInMessage() throws Exception
    {
        Path repoRoot = TMP_FOLDER.newFolder().toPath();
        List<String> included = Arrays.asList(DEFAULT_INCLUDED_SUBPATH, null);
        try
        {
            EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repoRoot, included, null, null, null);
            Assert.fail("Expected IllegalArgumentException for a null path entry");
        }
        catch (IllegalArgumentException e)
        {
            String msg = e.getMessage();
            Assert.assertTrue("Message should name the offending parameter; got: " + msg,
                    msg.contains("includedRelativeSubpaths"));
            Assert.assertTrue("Message should reference the offending index; got: " + msg,
                    msg.contains("index 1"));
            Assert.assertTrue("Message should call out that the value is null; got: " + msg,
                    msg.contains("null"));
        }
    }

    @Test
    public void nullEntryInExcludedRelativeSubpathsThrowsWithParameterInMessage() throws Exception
    {
        Path repoRoot = TMP_FOLDER.newFolder().toPath();
        List<String> excluded = Arrays.asList("legacy/deprecated", null);
        try
        {
            EMITCoverageReportGenerationMojo.collectEmitModelsDirs(repoRoot, Lists.fixedSize.with(DEFAULT_INCLUDED_SUBPATH), null, null, excluded);
            Assert.fail("Expected IllegalArgumentException for a null path entry");
        }
        catch (IllegalArgumentException e)
        {
            String msg = e.getMessage();
            Assert.assertTrue("Message should name the offending parameter; got: " + msg,
                    msg.contains("excludedRelativeSubpaths"));
            Assert.assertTrue("Message should reference the offending index; got: " + msg,
                    msg.contains("index 1"));
            Assert.assertTrue("Message should call out that the value is null; got: " + msg,
                    msg.contains("null"));
        }
    }

    private EMITCoverageReportGenerationMojo executeMojo(MavenProject mavenProject) throws Exception
    {
        EMITCoverageReportGenerationMojo mojo = (EMITCoverageReportGenerationMojo) this.mojoRule.lookupConfiguredMojo(mavenProject, GOAL);
        mojo.execute();
        return mojo;
    }

    private List<EMITModelDescriptor> discoverDescriptors(EMITCoverageReportGenerationMojo mojo, File projectDir) throws Exception
    {
        List<String> included = (List<String>) this.mojoRule.getVariableValueFromObject(mojo, "includedRelativeSubpaths");
        Set<String> excludedNames = (Set<String>) this.mojoRule.getVariableValueFromObject(mojo, "excludedDirectoryNames");
        List<String> excludedPrefixes = (List<String>) this.mojoRule.getVariableValueFromObject(mojo, "excludedDirectoryNamePrefixes");
        List<String> excludedSubpaths = (List<String>) this.mojoRule.getVariableValueFromObject(mojo, "excludedRelativeSubpaths");

        List<Path> dirs = EMITCoverageReportGenerationMojo.collectEmitModelsDirs(projectDir.toPath(), included, excludedNames, excludedPrefixes, excludedSubpaths);
        return EMITCoverageReportGenerationMojo.parseDescriptorsUnder(dirs);
    }

    private static void assertDescriptorNamesEqual(List<EMITModelDescriptor> descriptors, String... expectedNames)
    {
        Set<String> actualNames = descriptors.stream()
                .map(EMITModelDescriptor::getName)
                .collect(Collectors.toSet());
        Assert.assertEquals("Descriptor count mismatch", expectedNames.length, actualNames.size());
        for (String expectedName : expectedNames)
        {
            Assert.assertTrue(
                    "Expected descriptor '" + expectedName + "' but got " + actualNames,
                    actualNames.contains(expectedName));
        }
    }

    private File getExpectedOutputFile(MavenProject mavenProject)
    {
        return new File(mavenProject.getBuild().getOutputDirectory(), "emit/emit-coverage.html");
    }

    private Path touchYaml(File projectDir, String relativePath, String modelName) throws IOException
    {
        Path yaml = projectDir.toPath().resolve(relativePath);
        Files.createDirectories(yaml.getParent());
        String yamlBody = "name: " + modelName + "\n";
        Files.write(yaml, yamlBody.getBytes(StandardCharsets.UTF_8));
        return yaml;
    }

    private File buildSingleModuleProject(
            String projectDirName,
            String groupId,
            String artifactId,
            String version,
            File outputFile,
            List<String> includedRelativeSubpaths,
            Set<String> excludedDirectoryNames,
            List<String> excludedDirectoryNamePrefixes,
            List<String> excludedRelativeSubpaths) throws IOException
    {
        Model model = buildMavenModelWithPlugin(
                groupId, artifactId, version,
                outputFile,
                includedRelativeSubpaths,
                excludedDirectoryNames,
                excludedDirectoryNamePrefixes,
                excludedRelativeSubpaths);
        return buildProject(projectDirName, model);
    }

    private File buildProject(String projectDirName, Model model) throws IOException
    {
        File projectParentDir = TMP_FOLDER.newFolder();
        File projectDir = new File(projectParentDir, projectDirName);
        if (!projectDir.mkdirs())
        {
            throw new IOException("Could not create project directory " + projectDir);
        }
        serializeMavenModel(projectDir, model);
        return projectDir;
    }

    private void serializeMavenModel(File projectDir, Model model) throws IOException
    {
        serializeMavenModel(projectDir.toPath(), model);
    }

    private void serializeMavenModel(Path projectDir, Model model) throws IOException
    {
        Files.createDirectories(projectDir);
        try (Writer writer = Files.newBufferedWriter(projectDir.resolve("pom.xml"), StandardCharsets.UTF_8))
        {
            new MavenXpp3Writer().write(writer, model);
        }
    }

    private Model buildMavenModelWithPlugin(
            String groupId,
            String artifactId,
            String version,
            File outputFile,
            List<String> includedRelativeSubpaths,
            Set<String> excludedDirectoryNames,
            List<String> excludedDirectoryNamePrefixes,
            List<String> excludedRelativeSubpaths)
    {
        Model model = buildMavenModel(groupId, artifactId, version, null);
        Build build = new Build();
        build.addPlugin(buildPlugin(
                outputFile,
                includedRelativeSubpaths,
                excludedDirectoryNames,
                excludedDirectoryNamePrefixes,
                excludedRelativeSubpaths));
        model.setBuild(build);
        return model;
    }

    private Model buildMavenModel(String groupId, String artifactId, String version, String packaging)
    {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setModelEncoding(StandardCharsets.UTF_8.name());
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging(packaging);
        return model;
    }

    private Plugin buildPlugin(
            File outputFile,
            List<String> includedRelativeSubpaths,
            Set<String> excludedDirectoryNames,
            List<String> excludedDirectoryNamePrefixes,
            List<String> excludedRelativeSubpaths)
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId(PLUGIN_GROUP_ID);
        plugin.setArtifactId(PLUGIN_ARTIFACT_ID);

        Xpp3Dom configuration = newXpp3Dom("configuration", null, null);
        plugin.setConfiguration(configuration);

        if (outputFile != null)
        {
            newXpp3Dom("outputFilePath", outputFile.getAbsolutePath(), configuration);
        }
        appendStringCollection(configuration, "includedRelativeSubpaths", "includedRelativeSubpath", includedRelativeSubpaths);
        appendStringCollection(configuration, "excludedDirectoryNames", "excludedDirectoryName", excludedDirectoryNames);
        appendStringCollection(configuration, "excludedDirectoryNamePrefixes", "excludedDirectoryNamePrefix", excludedDirectoryNamePrefixes);
        appendStringCollection(configuration, "excludedRelativeSubpaths", "excludedRelativeSubpath", excludedRelativeSubpaths);

        PluginExecution execution = new PluginExecution();
        plugin.addExecution(execution);
        execution.setPhase("generate-resources");
        execution.getGoals().add(GOAL);

        return plugin;
    }

    private void appendStringCollection(Xpp3Dom parent, String parentName, String childName, Collection<String> values)
    {
        if (values == null)
        {
            return;
        }
        Xpp3Dom container = newXpp3Dom(parentName, null, parent);
        for (String v : values)
        {
            newXpp3Dom(childName, v, container);
        }
    }

    private Xpp3Dom newXpp3Dom(String name, String value, Xpp3Dom parent)
    {
        Xpp3Dom element = new Xpp3Dom(name);
        if (value != null)
        {
            element.setValue(value);
        }
        if (parent != null)
        {
            parent.addChild(element);
        }
        return element;
    }
}
