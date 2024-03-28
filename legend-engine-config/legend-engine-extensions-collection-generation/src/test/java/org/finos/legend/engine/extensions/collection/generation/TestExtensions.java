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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtension;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtensionLoader;
import org.finos.legend.engine.entitlement.services.RelationalDatabaseEntitlementServiceExtension;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.functionActivator.generation.FunctionActivatorArtifactGenerationExtension;
import org.finos.legend.engine.generation.DataSpaceAnalyticsArtifactGenerationExtension;
import org.finos.legend.engine.generation.OpenApiArtifactGenerationExtension;
import org.finos.legend.engine.generation.SearchDocumentArtifactGenerationExtension;
import org.finos.legend.engine.language.bigqueryFunction.compiler.toPureGraph.BigQueryFunctionCompilerExtension;
import org.finos.legend.engine.language.bigqueryFunction.grammar.from.BigQueryFunctionGrammarParserExtension;
import org.finos.legend.engine.language.bigqueryFunction.grammar.to.BigQueryFunctionGrammarComposer;
import org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLGrammarParserExtension;
import org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureGrammarComposerExtension;
import org.finos.legend.engine.language.hostedService.compiler.toPureGraph.HostedServiceCompilerExtension;
import org.finos.legend.engine.language.hostedService.generation.deployment.HostedServiceArtifactGenerationExtension;
import org.finos.legend.engine.language.hostedService.grammar.from.HostedServiceGrammarParserExtension;
import org.finos.legend.engine.language.hostedService.grammar.to.HostedServiceGrammarComposer;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.AuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.AuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.grammar.from.GenerationParserExtension;
import org.finos.legend.engine.language.pure.dsl.generation.grammar.to.GenerationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.MasteryParserExtension;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.to.MasteryGrammarComposerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.from.PersistenceCloudParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.to.PersistenceCloudComposerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.to.PersistenceComposerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.from.PersistenceRelationalParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.to.PersistenceRelationalComposerExtension;
import org.finos.legend.engine.language.pure.dsl.service.grammar.from.ServiceParserExtension;
import org.finos.legend.engine.language.pure.dsl.service.grammar.to.ServiceGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.DataSpaceParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.DiagramParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ExternalFormatGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ServiceStoreGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.TextParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.BigQueryGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.CorePureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.DataSpaceGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.DiagramGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.ExternalFormatGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.RelationalGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.ServiceStoreGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.SpannerGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.TextGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.TrinoGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.SnowflakeGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.RedshiftGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.DatabricksGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.snowflakeApp.compiler.toPureGraph.SnowflakeAppCompilerExtension;
import org.finos.legend.engine.language.snowflakeApp.generator.SnowflakeAppArtifactGenerationExtension;
import org.finos.legend.engine.language.snowflakeApp.grammar.from.SnowflakeAppGrammarParserExtension;
import org.finos.legend.engine.language.snowflakeApp.grammar.to.SnowflakeAppGrammarComposer;
import org.finos.legend.engine.language.sql.grammar.integration.SQLGrammarParserExtension;
import org.finos.legend.engine.language.sql.grammar.integration.SQLPureGrammarComposerExtension;
import org.finos.legend.engine.language.stores.elasticsearch.v7.from.ElasticsearchGrammarParserExtension;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionProtocolExtension;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceProtocolExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.code.core.ElasticsearchLegendPureCoreExtension;
import org.finos.legend.engine.language.stores.elasticsearch.v7.to.ElasticsearchGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppProtocolExtension;
import org.finos.legend.engine.pure.code.core.CoreLegendPureCoreExtension;
import org.finos.legend.engine.pure.code.core.LegendPureCoreExtension;
import org.finos.legend.engine.pure.code.core.ServiceLegendPureCoreExtension;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.code.core.*;
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
        assertHasExtensions(getExpectedPlanGeneratorExtensions(), LegendPureCoreExtension.class);
    }

    @Test
    public void testExpectedExternalFormatExtensionsArePresent()
    {
        assertHasExtensions(getExpectedExternalFormatExtensions(), ExternalFormatExtension.class);
    }

    @Test
    public void testFlatDataDriverDescription()
    {
        assertHasExtensions(getExpectedFlatDataDriverDescriptionExtensions(), FlatDataDriverDescription.class);
    }

    @Test
    public void testExpectedArtifactGenerationExtensionsArePresent()
    {
        assertHasExtensions(ArtifactGenerationExtensionLoader.extensions(), getExpectedArtifactGenerationExtensions(), ArtifactGenerationExtension.class);
    }

    @Test
    public void testExpectedEntitlementServiceExtensionsArePresent()
    {
        assertHasExtensions(EntitlementServiceExtensionLoader.extensions(), getExpectedEntitlementServiceExtensions(), EntitlementServiceExtension.class);
    }

    @Test
    public void testCodeRepositories()
    {
        Assert.assertEquals(Lists.mutable.withAll(getExpectedCodeRepositories()).sortThis(), CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName, Lists.mutable.empty()).select(c -> !c.startsWith("platform")).sortThis());
    }

    @Test
    public void testMetadataSpecifications()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Iterable<String> expectedRepos = getExpectedCodeRepositories();

        MutableSet<String> allSpecNames = Iterate.collect(DistributedMetadataSpecification.loadAllSpecifications(classLoader), DistributedMetadataSpecification::getName, Sets.mutable.empty());
        Assert.assertEquals(Lists.fixedSize.empty(), Iterate.reject(expectedRepos, allSpecNames::contains, Lists.mutable.empty()));

        MutableSet<String> specNames = Iterate.collect(DistributedMetadataSpecification.loadSpecifications(classLoader, expectedRepos), DistributedMetadataSpecification::getName, Sets.mutable.empty());
        Assert.assertEquals(Sets.mutable.withAll(expectedRepos).with("platform"), specNames.select(c -> !c.startsWith("platform_")));
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
        PureModel pureModel = new PureModel(PureModelContextData.newPureModelContextData(), IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName(), DeploymentMode.PROD);
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
                .with(SnowflakeAppProtocolExtension.class)
                .with(HostedServiceProtocolExtension.class)
                .with(BigQueryFunctionProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.DiagramProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.GenerationProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceCloudProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.PersistenceRelationalProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.MasteryProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.RelationalProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.BigQueryProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.SpannerProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.TrinoProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.SnowflakeProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.DatabricksProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.RedshiftProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.ServiceProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.ServiceStoreProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.AuthenticationProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.TextProtocolExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureProtocolExtension.class)
                .with(org.finos.legend.engine.language.sql.grammar.integration.SQLPureProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.store.elasticsearch.v7.ElasticsearchV7ProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDBPureProtocolExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends GenerationExtension>> getExpectedGenerationExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends GenerationExtension>>empty()
                .with(org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.avro.extension.AvroGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.jsonSchema.extension.JSONSchemaGenerationExtension.class)
                .with(org.finos.legend.engine.external.language.morphir.extension.MorphirGenerationExtension.class)
                .with(org.finos.legend.engine.query.graphQL.api.format.generation.GraphQLGenerationExtension.class)
                .with(org.finos.legend.engine.external.format.daml.generation.DAMLGenerationExtension.class);
    }

    protected Iterable<? extends Class<? extends PureGrammarParserExtension>> getExpectedGrammarParserExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureGrammarParserExtension>>empty()
                .with(CorePureGrammarParser.class)
                .with(DataSpaceParserExtension.class)
                .with(SnowflakeAppGrammarParserExtension.class)
                .with(HostedServiceGrammarParserExtension.class)
                .with(BigQueryFunctionGrammarParserExtension.class)
                .with(DiagramParserExtension.class)
                .with(ExternalFormatGrammarParserExtension.class)
                .with(GenerationParserExtension.class)
                .with(MasteryParserExtension.class)
                .with(PersistenceParserExtension.class)
                .with(PersistenceCloudParserExtension.class)
                .with(PersistenceRelationalParserExtension.class)
                .with(RelationalGrammarParserExtension.class)
                .with(ServiceParserExtension.class)
                .with(AuthenticationGrammarParserExtension.class)
                .with(GraphQLGrammarParserExtension.class)
                .with(SQLGrammarParserExtension.class)
                .with(ServiceStoreGrammarParserExtension.class)
                .with(TextParserExtension.class)
                .with(ElasticsearchGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBGrammarParserExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends PureGrammarComposerExtension>> getExpectedGrammarComposerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureGrammarComposerExtension>>empty()
                .with(CorePureGrammarComposer.class)
                .with(DataSpaceGrammarComposerExtension.class)
                .with(SnowflakeAppGrammarComposer.class)
                .with(HostedServiceGrammarComposer.class)
                .with(BigQueryFunctionGrammarComposer.class)
                .with(DiagramGrammarComposerExtension.class)
                .with(ExternalFormatGrammarComposerExtension.class)
                .with(GenerationGrammarComposerExtension.class)
                .with(MasteryGrammarComposerExtension.class)
                .with(PersistenceComposerExtension.class)
                .with(PersistenceCloudComposerExtension.class)
                .with(PersistenceRelationalComposerExtension.class)
                .with(RelationalGrammarComposerExtension.class)
                .with(BigQueryGrammarComposerExtension.class)
                .with(SpannerGrammarComposerExtension.class)
                .with(TrinoGrammarComposerExtension.class)
                .with(SnowflakeGrammarComposerExtension.class)
                .with(RedshiftGrammarComposerExtension.class)
                .with(DatabricksGrammarComposerExtension.class)
                .with(ServiceGrammarComposerExtension.class)
                .with(ServiceStoreGrammarComposerExtension.class)
                .with(GraphQLPureGrammarComposerExtension.class)
                .with(SQLPureGrammarComposerExtension.class)
                .with(AuthenticationGrammarComposerExtension.class)
                .with(TextGrammarComposerExtension.class)
                .with(ElasticsearchGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBGrammarComposerExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends CompilerExtension>> getExpectedCompilerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends CompilerExtension>>empty()
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DiagramCompilerExtension.class)
                .with(SnowflakeAppCompilerExtension.class)
                .with(HostedServiceCompilerExtension.class)
                .with(BigQueryFunctionCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DataSpaceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.TextCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.CoreCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph.ServiceCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ExternalFormatCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph.MasteryCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.PersistenceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph.PersistenceCloudCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.relational.compiler.toPureGraph.PersistenceRelationalCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationalCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.BigQueryCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SpannerCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.TrinoCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SnowflakeCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.RedshiftCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DatabricksCompilerExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLCompilerExtension.class)
                .with(org.finos.legend.engine.language.sql.grammar.integration.SQLCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ServiceStoreCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.AuthenticationCompilerExtension.class)
                .with(org.finos.legend.engine.language.stores.elasticsearch.v7.compiler.ElasticsearchCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBCompilerExtension.class)
                .with(org.finos.legend.engine.external.format.json.compile.JsonSchemaCompiler.class)
                ;
    }

    protected Iterable<? extends Class<? extends LegendPureCoreExtension>> getExpectedPlanGeneratorExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends LegendPureCoreExtension>>empty()
                .with(JSONJavaBindingLegendPureCoreExtension.class)
                .with(MongoDBLegendPureCoreExtension.class)
                .with(FlatDataLegendPureCoreExtension.class)
                .with(ElasticsearchLegendPureCoreExtension.class)
                .with(CoreLegendPureCoreExtension.class)
                .with(JSONLegendPureCoreExtension.class)
                .with(RelationalLegendPureCoreExtension.class)
                .with(ExternalFormatJavaBindingLegendPureCoreExtension.class)
                .with(M2MJavaBindingLegendPureCoreExtension.class)
                .with(ServiceStoreJavaBindingLegendPureCoreExtension.class)
                .with(ServiceStoreLegendPureCoreExtension.class)
                .with(FlatDataJavaBindingLegendPureCoreExtension.class)
                .with(XMLLegendPureCoreExtension.class)
                .with(XMLJavaBindingLegendPureCoreExtension.class)
                .with(ServiceLegendPureCoreExtension.class)
                .with(RelationalJavaBindingLegendPureCoreExtension.class)
                .with(ArrowLegendPureCoreExtension.class)
                ;
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
                .with(org.finos.legend.engine.external.format.daml.DamlFormatExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends FlatDataDriverDescription>> getExpectedFlatDataDriverDescriptionExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends FlatDataDriverDescription>>empty()
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.DelimitedWithHeadingsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.DelimitedWithoutHeadingsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.FixedWidthDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.ImmaterialLinesDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergActionsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergDataDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergExtendActionDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergMetadataDriverDescription.class);
    }

    protected Iterable<? extends Class<? extends ArtifactGenerationExtension>> getExpectedArtifactGenerationExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends ArtifactGenerationExtension>>empty()
                .with(DataSpaceAnalyticsArtifactGenerationExtension.class)
                .with(SearchDocumentArtifactGenerationExtension.class)
                .with(OpenApiArtifactGenerationExtension.class)
                .with(SnowflakeAppArtifactGenerationExtension.class)
                .with(HostedServiceArtifactGenerationExtension.class)
                .with(FunctionActivatorArtifactGenerationExtension.class);
    }

    protected Iterable<? extends Class<? extends EntitlementServiceExtension>> getExpectedEntitlementServiceExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends EntitlementServiceExtension>>empty()
                .with(RelationalDatabaseEntitlementServiceExtension.class);
    }

    protected Iterable<String> getExpectedCodeRepositories()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<String>empty()
                .with("core")
                .with("core_generation")
                .with("core_service")
                .with("core_functions")
                .with("core_analytics_binding")
                .with("core_analytics_class")
                .with("core_analytics_function")
                .with("core_analytics_lineage")
                .with("core_analytics_mapping")
                .with("core_analytics_search")
                .with("core_data_space")
                .with("core_data_space_metamodel")
                .with("core_diagram")
                .with("core_diagram_metamodel")
                .with("core_external_format_avro")
                .with("core_external_language_morphir")
                .with("core_external_format_flatdata")
                .with("core_external_format_json")
                .with("core_external_format_openapi")
                .with("core_external_format_protobuf")
                .with("core_external_format_xml")
                .with("core_external_format_arrow")
                .with("core_external_query_graphql")
                .with("core_external_query_graphql_metamodel")
                .with("core_external_query_sql_metamodel")
                .with("core_function_activator")
                .with("core_external_compiler")
                .with("core_persistence")
                .with("core_mastery")
                .with("core_external_language_daml")
                .with("core_external_language_haskell")
                .with("core_persistence_cloud")
                .with("core_persistence_relational")
                .with("core_relational")
                .with("core_relational_bigquery")
                .with("core_relational_spanner")
                .with("core_relational_trino")
                .with("core_relational_snowflake")
                .with("core_relational_redshift")
                .with("core_relational_databricks")
                .with("core_relational_postgres")
                .with("core_relational_hive")
                .with("core_relational_presto")
                .with("core_relational_sybase")
                .with("core_relational_sybaseiq")
                .with("core_relational_sparksql")
                .with("core_relational_store_entitlement")
                .with("core_servicestore")
                .with("core_authentication")
                .with("core_snowflakeapp")
                .with("core_bigqueryfunction")
                .with("core_hostedservice")
                .with("core_text_metamodel")
                .with("core_external_language_java")
                .with("core_java_platform_binding")
                .with("core_java_platform_binding_external_format")
                .with("core_relational_java_platform_binding")
                .with("core_servicestore_java_platform_binding")
                .with("core_external_format_flatdata_java_platform_binding")
                .with("core_external_format_json_java_platform_binding")
                .with("core_external_format_xml_java_platform_binding")
                .with("core_configuration")
                .with("core_elasticsearch_seven_metamodel")
                .with("core_nonrelational_mongodb")
                .with("core_nonrelational_mongodb_java_platform_binding")
                ;
    }
}
