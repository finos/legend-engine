// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.generation;

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class TestPowerBIArtifactGenerationExtension
{
    private static final String minimumPureClientVersion = "v1_20_0";
    private static final ImmutableList<String> testVersions = PureClientVersions.versionsSince(minimumPureClientVersion);
    private static final PowerBIArtifactGenerationExtension extension = new PowerBIArtifactGenerationExtension();
    private static final String DOT_PLATFORM_ARTIFACT_REGEX = "\\{\"\\$schema\":\"https://developer\\.microsoft\\.com/json-schemas/fabric/gitIntegration/platformProperties/2\\.0\\.0/schema\\.json\",\"metadata\":\\{\"type\":\"(Report|SemanticModel)\",\"displayName\":\"[^\"]+\"},\"config\":\\{\"version\":\"2\\.0\",\"logicalId\":\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"}}";

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithoutStereotype()
    {
        String pureModelString = getResourceAsString("models/powerbi-artifact-generation-dataspaces.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, null, Identity.getAnonymousIdentity().getName());

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement("spaces::dataspaceWithoutPowerBIStereotype");
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace);
        Assert.assertFalse(extension.canGenerate(packageableElement));
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithNoExecutables()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithNoExecutables", "Data spaces must have atleast one exectuable for Power BI artifact generation. The data space with name \"dataspaceWithNoExecutables\" does not have any executables.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithMultiExecutionServiceExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithMultiExecutionService", "Only single execution services are supported for Power BI artifact generation. Service with pattern \"/dummy/multi\" is not a single execution service.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithNonTDSResultExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithNonTDSService", "Only executables with TDS/Relation result types are supported for Power BI artifact generation. Executable with title \"Non TDS Service\" does not have a result type of TDS.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithParameterizedExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithParametrizedFunctionExecutable", "Parameterised executables are not supported for Power BI artifact generation. Executable with title \"Parameterized Function\" is not supported.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithMultiStepExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithExecutableLocalVars", "Executables with local vars are not supported for Power BI artifact generation. Executable with title \"Service With Local VAR\" is not supported.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithNonSFRuntimeExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithNonSnowflakeRuntime", "Only executables with a Relational database connection of type Snowflake are supported for Power BI artifact generation. Executable with title \"Dummy Service\" does not have a Snowflake connection based runtime.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithSFRuntimeWithoutRoleExecutable()
    {
        testPowerBIArtifactGenerationPureAssertionExceptions("models/powerbi-artifact-generation-dataspaces.pure", "spaces::dataspaceWithSnowflakeRuntimeWithoutRole", "Only Snowflake connections with a role specified are supported for Power BI Artifact generation.");
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithValidExecutables()
    {
        String dataSpaceName = "dataspaceWithValidExecutables";
        String functionPointerExecutableTitle = "Customer_Type_Function";
        String serviceExecutableTitle = "Customer_Service";
        String inlineExecutableTitle = "Inline_Query_Executable";
        String queryMode = "Import";

        Map<String, String> expectedArtifactsContent = getExpectedArtifacts(dataSpaceName, functionPointerExecutableTitle, serviceExecutableTitle, inlineExecutableTitle, queryMode);
        testPowerBIArtifactGenerationOutput("models/powerbi-artifact-generation-dataspaces.pure", "spaces::" + dataSpaceName, expectedArtifactsContent);
    }

    @Test
    public void testPowerBIArtifactGenerationForDataspaceWithValidExecutablesDirectQueryMode()
    {
        String dataSpaceName = "dataspaceWithValidExecutablesDirectQueryMode";
        String functionPointerExecutableTitle = "Customer_Type_Function";
        String serviceExecutableTitle = "Customer_Service";
        String inlineExecutableTitle = "Inline_Query_Executable";
        String queryMode = "DirectQuery";

        Map<String, String> expectedArtifactsContent = getExpectedArtifacts(dataSpaceName, functionPointerExecutableTitle, serviceExecutableTitle, inlineExecutableTitle, queryMode);
        testPowerBIArtifactGenerationOutput("models/powerbi-artifact-generation-dataspaces.pure", "spaces::" + dataSpaceName, expectedArtifactsContent);
    }

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource(path);
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : null;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testPowerBIArtifactGenerationOutput(String modelPath, String dataSpacePath, Map<String,String> expectedArtifactsContent)
    {
        String pureModelString = getResourceAsString(modelPath);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, null, Identity.getAnonymousIdentity().getName());

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement(dataSpacePath);
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace);
        Assert.assertTrue(extension.canGenerate(packageableElement));
        for (String pureClientVersion : testVersions)
        {
            List<Artifact> powerBIArtifacts = extension.generate(packageableElement, pureModel, pureModelContextData, pureClientVersion);
            Assert.assertEquals(powerBIArtifacts.size(), 11);

            for (Artifact artifact: powerBIArtifacts)
            {
                if (artifact.path.endsWith("/.platform"))
                {
                    //Asserting only regex as UUID will be random
                    Assert.assertTrue(Pattern.matches(DOT_PLATFORM_ARTIFACT_REGEX, artifact.content));
                    continue;
                }
                Assert.assertTrue(expectedArtifactsContent.containsKey(artifact.path));
                Assert.assertEquals(expectedArtifactsContent.get(artifact.path), artifact.content);
            }
        }
    }

    private void testPowerBIArtifactGenerationPureAssertionExceptions(String modelPath, String dataSpacePath, String pureAssertionExceptionMessage)
    {
        String pureModelString = getResourceAsString(modelPath);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, null, Identity.getAnonymousIdentity().getName());

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement(dataSpacePath);

        for (String pureClientVersion : testVersions)
        {
            Assert.assertTrue(packageableElement instanceof Root_meta_pure_metamodel_dataSpace_DataSpace);
            Assert.assertTrue(extension.canGenerate(packageableElement));
            Assert.assertEquals(pureAssertionExceptionMessage, Assert.assertThrows(PureException.class, () -> extension.generate(packageableElement, pureModel, pureModelContextData, pureClientVersion)).getInfo());
        }
    }

    private Map<String, String> getExpectedArtifacts(String dataSpaceName, String functionPointerExecutableTitle, String serviceExecutableTitle, String inlineExecutableTitle, String queryMode)
    {
        String dataspaceNamePlaceholder = "dataspaceNamePlaceholder";
        Map<String, String> expectedArtifacts = new HashMap<>();
        //Main pbip file
        expectedArtifacts.put(dataSpaceName + ".pbip", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/dataspace.pbip")), dataSpaceName));

        //Semantic model files
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition.pbism", getResourceAsString("powerbi_expected_artifacts/definition.pbism"));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/cultures/en_US.tmdl", getResourceAsString("powerbi_expected_artifacts/en_US.tmdl"));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/database.tmdl", getResourceAsString("powerbi_expected_artifacts/database.tmdl"));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/model.tmdl", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/model.tmdl")), functionPointerExecutableTitle, serviceExecutableTitle, inlineExecutableTitle));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/tables/" + serviceExecutableTitle + ".tmdl", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/serviceTable.tmdl")), queryMode).replace(dataspaceNamePlaceholder, dataSpaceName));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/tables/" + inlineExecutableTitle + ".tmdl", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/inlineQueryTable.tmdl")), queryMode).replace(dataspaceNamePlaceholder, dataSpaceName));
        expectedArtifacts.put(dataSpaceName + ".SemanticModel/definition/tables/" + functionPointerExecutableTitle + ".tmdl", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/functionPointerTable.tmdl")), queryMode).replace(dataspaceNamePlaceholder, dataSpaceName));

        //Report files
        expectedArtifacts.put(dataSpaceName + ".Report/definition.pbir", String.format(Objects.requireNonNull(getResourceAsString("powerbi_expected_artifacts/definition.pbir")), dataSpaceName));

        return expectedArtifacts;
    }
}
