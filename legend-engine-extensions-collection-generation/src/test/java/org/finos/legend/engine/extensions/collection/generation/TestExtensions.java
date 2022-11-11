// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.extensions.collection.generation;

import java.util.Set;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.generation.DataSpaceAnalyticsArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.from.DataSpaceParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.DiagramParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.TextParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.DataSpaceGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.DiagramGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.TextGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedMetadataSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.ServiceLoader;

public class TestExtensions
{
    @Test
    public void testExpectedProtocolExtensionsArePresent()
    {
        assertHasExtensions(getExpectedPureProtocolExtensions(), PureProtocolExtension.class);
        assertHasExtensions(PureProtocolExtensionLoader.extensions(), getExpectedPureProtocolExtensions(), PureProtocolExtension.class);
    }

    @Test
    public void testExpectedGenerationExtension()
    {
        assertHasExtensions(getExpectedGenerationExtensions(), GenerationExtension.class);
    }

    @Test
    public void testExpectedGrammarExtensionsArePresent()
    {
        assertHasExtensions(getExpectedGrammarParserExtensions(), PureGrammarParserExtension.class);
        assertHasExtensions(getExpectedGrammarComposerExtensions(), PureGrammarComposerExtension.class);
    }

    @Test
    public void testExpectedCompilerExtensionsArePresent()
    {
        assertHasExtensions(getExpectedCompilerExtensions(), CompilerExtension.class);
    }

    @Test
    public void testPlanGeneratorExtensionArePresent()
    {
        assertHasExtensions(getExpectedPlanGeneratorExtensions(), PlanGeneratorExtension.class);
    }

    @Test
    public void testExpectedExternalFormatExtensionsArePresent()
    {
        assertHasExtensions(getExpectedExternalFormatExtensions(), ExternalFormatExtension.class);
    }

    @Test
    public void testExpectedArtifactGenerationExtensionsArePresent()
    {
        assertHasExtensions(ArtifactGenerationExtensionLoader.extensions(), getExpectedArtifactGenerationExtensions(), ArtifactGenerationExtension.class);
    }

    @Test
    public void testCodeRepositories()
    {
        Assert.assertEquals(Lists.mutable.withAll(getExpectedCodeRepositories()).sortThis(), CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName, Lists.mutable.empty()).sortThis());
    }

    @Test
    public void testMetadataSpecifications()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Iterable<String> expectedRepos = getExpectedCodeRepositories();

        MutableSet<String> allSpecNames = Iterate.collect(DistributedMetadataSpecification.loadAllSpecifications(classLoader), DistributedMetadataSpecification::getName, Sets.mutable.empty());
        Assert.assertEquals(Lists.fixedSize.empty(), Iterate.reject(expectedRepos, allSpecNames::contains, Lists.mutable.empty()));

        MutableSet<String> specNames = Iterate.collect(DistributedMetadataSpecification.loadSpecifications(classLoader, expectedRepos), DistributedMetadataSpecification::getName, Sets.mutable.empty());
        Assert.assertEquals(Sets.mutable.withAll(expectedRepos).with("platform"), specNames);
    }

    @Test
    public void testMetadataDeserializer()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        DistributedBinaryGraphDeserializer deserializer = DistributedBinaryGraphDeserializer.newBuilder(classLoader).withMetadataNames(getExpectedCodeRepositories()).build();

        Assert.assertTrue(deserializer.hasClassifier("meta::pure::metamodel::type::Class"));
        Assert.assertTrue(deserializer.hasInstance("meta::pure::metamodel::type::Class", "Root::meta::pure::metamodel::type::Class"));

        MutableSet<String> expectedClassifiers = Iterate.flatCollect(PureProtocolExtensionLoader.extensions(), ext -> ext.getExtraProtocolToClassifierPathMap().values(), Sets.mutable.empty());
        Assert.assertEquals(
                Lists.fixedSize.empty(),
                expectedClassifiers.reject(cl -> deserializer.hasInstance("meta::pure::metamodel::type::Class", "Root::" + cl), Lists.mutable.empty()));
    }

    @Test
    public void testMetadata()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        MetadataLazy metadataLazy = MetadataLazy.fromClassLoader(classLoader, getExpectedCodeRepositories());
        MutableSet<String> expectedClassifiers = Iterate.flatCollect(PureProtocolExtensionLoader.extensions(), ext -> ext.getExtraProtocolToClassifierPathMap().values(), Sets.mutable.empty());
        Assert.assertEquals(
                Lists.fixedSize.empty(),
                expectedClassifiers.select(cl ->
                {
                    try
                    {
                        return metadataLazy.getMetadata("meta::pure::metamodel::type::Class", "Root::" + cl) == null;
                    }
                    catch (DistributedBinaryGraphDeserializer.UnknownInstanceException ignore)
                    {
                        return true;
                    }
                }, Lists.mutable.empty()));
    }

    @Test
    public void testPureModel()
    {
        PureModel pureModel = new PureModel(PureModelContextData.newPureModelContextData(), Lists.immutable.empty(), DeploymentMode.PROD);
        MutableSet<String> expectedClassifiers = Iterate.flatCollect(PureProtocolExtensionLoader.extensions(), ext -> ext.getExtraProtocolToClassifierPathMap().values(), Sets.mutable.empty());
        Assert.assertEquals(
                Lists.fixedSize.empty(),
                expectedClassifiers.select(cl ->
                {
                    try
                    {
                        return pureModel.getClass(cl) == null;
                    }
                    catch (EngineException e)
                    {
                        String cantFindMessage = "Can't find class '" + cl + "'";
                        if (cantFindMessage.equals(e.getMessage()))
                        {
                            return true;
                        }
                        throw e;
                    }
                }, Lists.mutable.empty()));
    }

    protected <T> void assertHasExtensions(Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass)
    {
        assertHasExtensions(Lists.mutable.withAll(ServiceLoader.load(extensionClass)), expectedExtensionClasses, extensionClass);
    }

    protected <T> void assertHasExtensions(Iterable<? extends T> availableClasses, Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass)
    {
        MutableSet<Class<? extends T>> missingClasses = Sets.mutable.withAll(expectedExtensionClasses);
        MutableList<Class<?>> unexpectedClasses = Lists.mutable.empty();
        availableClasses.forEach(e ->
        {
            if (!missingClasses.remove(e.getClass()))
            {
                unexpectedClasses.add(e.getClass());
            }
        });
        Assert.assertEquals("Missing extensions for " + extensionClass.getName(), Collections.emptySet(), missingClasses);
        Assert.assertEquals("Unexpected extensions for " + extensionClass.getName(), Collections.emptyList(), unexpectedClasses);
    }

    protected Iterable<? extends Class<? extends PureProtocolExtension>> getExpectedPureProtocolExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureProtocolExtension>>empty()
                .with(org.finos.legend.engine.protocol.pure.v1.CorePureProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.DataSpaceProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.DiagramProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.GenerationProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceCloudProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceRelationalProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.MasteryProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.RelationalProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.BigQueryProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.SpannerProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.ServiceProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.ServiceStoreProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.TextProtocolExtension.class)
                .with(org.finos.legend.engine.external.format.flatdata.FlatDataProtocolExtension.class)
                .with(org.finos.legend.engine.external.format.json.JsonProtocolExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureProtocolExtension.class)
                .with(org.finos.legend.engine.external.format.xml.XmlProtocolExtension.class);
    }

    protected Iterable<? extends Class<? extends GenerationExtension>> getExpectedGenerationExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends GenerationExtension>>empty()
                .with(org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.avro.extension.AvroGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.jsonSchema.extension.JSONSchemaGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.rosetta.extension.RosettaGenerationExtension.class)
                .with(org.finos.legend.engine.external.language.morphir.extension.MorphirGenerationExtension.class)
                .with(org.finos.legend.engine.query.graphQL.api.format.generation.GraphQLGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.daml.generation.DAMLGenerationExtension.class);
    }

    protected Iterable<? extends Class<? extends PureGrammarParserExtension>> getExpectedGrammarParserExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureGrammarParserExtension>>empty()
                .with(org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser.class)
                .with(DataSpaceParserExtension.class)
                .with(DiagramParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.from.ExternalFormatConnectionGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.from.ExternalFormatGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.generation.grammar.from.GenerationParserExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.MasteryParserExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceParserExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.from.PersistenceCloudParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.service.grammar.from.ServiceParserExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.from.ServiceStoreGrammarParserExtension.class)
                .with(TextParserExtension.class);
    }

    protected Iterable<? extends Class<? extends PureGrammarComposerExtension>> getExpectedGrammarComposerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureGrammarComposerExtension>>empty()
                .with(org.finos.legend.engine.language.pure.grammar.to.CorePureGrammarComposer.class)
                .with(DataSpaceGrammarComposerExtension.class)
                .with(DiagramGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.to.ExternalFormatGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.generation.grammar.to.GenerationGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.mastery.grammar.to.MasteryGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.grammar.to.PersistenceComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.to.PersistenceCloudComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.to.RelationalGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.to.BigQueryGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.to.SpannerGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.service.grammar.to.ServiceGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.to.ServiceStoreGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureGrammarComposerExtension.class)
                .with(TextGrammarComposerExtension.class);
    }

    protected Iterable<? extends Class<? extends CompilerExtension>> getExpectedCompilerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends CompilerExtension>>empty()
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DiagramCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DataSpaceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.TextCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.CoreCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph.ServiceCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ExternalFormatCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ExternalFormatConnectionCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph.MasteryCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.PersistenceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph.PersistenceCloudCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationalCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.BigQueryCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SpannerCompilerExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ServiceStoreCompilerExtension.class);
    }

    protected Iterable<? extends Class<? extends PlanGeneratorExtension>> getExpectedPlanGeneratorExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PlanGeneratorExtension>>empty()
                .with(org.finos.legend.engine.plan.generation.extension.LegendPlanGeneratorExtension.class);
    }

    protected Iterable<? extends Class<? extends ExternalFormatExtension<?>>> getExpectedExternalFormatExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends ExternalFormatExtension<?>>>empty()
                .with(org.finos.legend.engine.external.format.flatdata.FlatDataExternalFormatExtension.class)
                .with(org.finos.legend.engine.external.format.json.JsonExternalFormatExtension.class)
                .with(org.finos.legend.engine.external.format.xsd.XsdExternalFormatExtension.class)
                .with(org.finos.legend.engine.external.format.protobuf.ProtobufFormatExtension.class)
                .with(org.finos.legend.engine.query.graphQL.api.format.GraphQLFormatExtension.class)
                .with(org.finos.legend.engine.query.graphQL.api.format.GraphQLSDLFormatExtension.class)
                .with(org.finos.legend.engine.external.format.daml.DamlFormatExtension.class);
    }

    protected Iterable<? extends Class<? extends ArtifactGenerationExtension>> getExpectedArtifactGenerationExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends ArtifactGenerationExtension>>empty()
                .with(DataSpaceAnalyticsArtifactGenerationExtension.class);
    }

    protected Iterable<String> getExpectedCodeRepositories()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<String>empty()
                .with("core")
                .with("core_analytics_mapping")
                .with("core_data_space")
                .with("core_diagram")
                .with("core_external_format_avro")
                .with("core_external_format_rosetta")
                .with("core_external_language_morphir")
                .with("core_external_format_flatdata")
                .with("core_external_format_json")
                .with("core_external_format_protobuf")
                .with("core_external_format_xml")
                .with("core_external_query_graphql")
                .with("core_external_query_graphql_metamodel")
                .with("core_external_compiler")
                .with("core_persistence")
                .with("core_mastery")
                .with("core_external_language_daml")
                .with("core_external_language_haskell")
                .with("core_persistence_cloud")
                .with("core_relational")
                .with("core_relational_bigquery")
                .with("core_relational_spanner")
                .with("core_servicestore")
                .with("core_text")
                .with("core_external_language_java")
                .with("core_java_platform_binding")
                .with("core_relational_java_platform_binding")
                .with("core_servicestore_java_platform_binding")
                .with("core_external_format_flatdata_java_platform_binding")
                .with("core_external_format_json_java_platform_binding")
                .with("core_external_format_xml_java_platform_binding")
                .with("core_configuration");
    }
}
