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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.code.core.SQLLegendPureCoreExtension;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtension;
import org.finos.legend.engine.entitlement.services.EntitlementServiceExtensionLoader;
import org.finos.legend.engine.entitlement.services.RelationalDatabaseEntitlementServiceExtension;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.functionActivator.generation.FunctionActivatorArtifactGenerationExtension;
import org.finos.legend.engine.generation.DataSpaceAnalyticsArtifactGenerationExtension;
import org.finos.legend.engine.generation.OpenApiArtifactGenerationExtension;
import org.finos.legend.engine.generation.PowerBIArtifactGenerationExtension;
import org.finos.legend.engine.generation.SearchDocumentArtifactGenerationExtension;
import org.finos.legend.engine.language.bigqueryFunction.compiler.toPureGraph.BigQueryFunctionCompilerExtension;
import org.finos.legend.engine.language.bigqueryFunction.grammar.from.BigQueryFunctionGrammarParserExtension;
import org.finos.legend.engine.language.bigqueryFunction.grammar.to.BigQueryFunctionGrammarComposer;
import org.finos.legend.engine.language.dataquality.grammar.from.DataQualityGrammarParserExtension;
import org.finos.legend.engine.language.dataquality.grammar.to.DataQualityGrammarComposerExtension;
import org.finos.legend.engine.language.deephaven.from.DeephavenGrammarParserExtension;
import org.finos.legend.engine.language.deephaven.to.DeephavenGrammarComposerExtension;
import org.finos.legend.engine.language.functionActivator.grammar.postDeployment.to.PostDeploymentActionGrammarComposer;
import org.finos.legend.engine.language.functionJar.compiler.toPureGraph.FunctionJarCompilerExtension;
import org.finos.legend.engine.language.functionJar.grammar.from.FunctionJarGrammarParserExtension;
import org.finos.legend.engine.language.functionJar.grammar.to.FunctionJarGrammarComposer;
import org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLGrammarParserExtension;
import org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureGrammarComposerExtension;
import org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLPureProtocolExtension;
import org.finos.legend.engine.language.hostedService.compiler.toPureGraph.HostedServiceCompilerExtension;
import org.finos.legend.engine.language.hostedService.generation.deployment.HostedServiceArtifactGenerationExtension;
import org.finos.legend.engine.language.hostedService.grammar.from.HostedServiceGrammarParserExtension;
import org.finos.legend.engine.language.hostedService.grammar.to.HostedServiceGrammarComposer;
import org.finos.legend.engine.language.memsqlFunction.compiler.toPureGraph.MemSqlFunctionCompilerExtension;
import org.finos.legend.engine.language.memsqlFunction.grammar.from.MemSqlFunctionGrammarParserExtension;
import org.finos.legend.engine.language.memsqlFunction.grammar.to.MemSqlFunctionGrammarComposer;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.AuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.AuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.dsl.generation.grammar.from.GenerationParserExtension;
import org.finos.legend.engine.language.pure.dsl.generation.grammar.to.GenerationGrammarComposerExtension;
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
import org.finos.legend.engine.language.pure.grammar.to.DatabricksGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.DiagramGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.ExternalFormatGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.RedshiftGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.RelationalGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.ServiceStoreGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.SnowflakeGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.SpannerGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.TextGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.TrinoGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.snowflake.compiler.toPureGraph.SnowflakeCompilerExtension;
import org.finos.legend.engine.language.snowflake.grammar.from.SnowflakeGrammarParserExtension;
import org.finos.legend.engine.language.snowflake.grammar.to.SnowflakeGrammarComposer;
import org.finos.legend.engine.language.snowflakeApp.generator.SnowflakeAppArtifactGenerationExtension;
import org.finos.legend.engine.language.snowflakeM2MUdf.generator.SnowflakeM2MUdfArtifactGenerationExtension;
import org.finos.legend.engine.language.sql.expression.grammar.parser.SQLExpressionGrammarParserExtension;
import org.finos.legend.engine.language.sql.expression.grammar.serializer.SQLExpressionGrammarComposerExtension;
import org.finos.legend.engine.language.sql.expression.protocol.SQLExpressionProtocolExtension;
import org.finos.legend.engine.language.stores.elasticsearch.v7.from.ElasticsearchGrammarParserExtension;
import org.finos.legend.engine.language.stores.elasticsearch.v7.to.ElasticsearchGrammarComposerExtension;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionProtocolExtension;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJarProtocolExtension;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceProtocolExtension;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionProtocolExtension;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDBPureProtocolExtension;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.AuthenticationProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.BigQueryProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.CorePureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.DataSpaceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.DatabricksProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.DiagramProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.GenerationProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.PersistenceCloudProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.PersistenceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.PersistenceRelationalProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.RedshiftProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.RelationalProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.ServiceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.ServiceStoreProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.SpannerProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.TextProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.TrinoProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.snowflake.SnowflakeProtocolExtension;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.ElasticsearchV7ProtocolExtension;
import org.finos.legend.engine.pure.code.core.CoreLegendPureCoreExtension;
import org.finos.legend.engine.pure.code.core.LegendPureCoreExtension;
import org.finos.legend.engine.pure.code.core.ServiceLegendPureCoreExtension;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.sql.compiler.SQLCompilerExtension;
import org.finos.legend.engine.tds.accessor.compiler.TDSAccessorCompilerExtension;
import org.finos.legend.engine.tds.accessor.grammar.parser.TDSRelationAccessorGrammarExtension;
import org.finos.legend.engine.tds.accessor.grammar.serializer.TDSRelationAccessorGrammarComposerExtension;
import org.finos.legend.engine.tds.accessor.protocol.TDSContainerPureProtocolExtension;
import org.finos.legend.pure.code.core.ArrowLegendPureCoreExtension;
import org.finos.legend.pure.code.core.DeephavenJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.DeephavenLegendPureCoreExtension;
import org.finos.legend.pure.code.core.DuckDbSqlDialectTranslationPureCoreExtension;
import org.finos.legend.pure.code.core.ElasticsearchLegendPureCoreExtension;
import org.finos.legend.pure.code.core.ExternalFormatJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.FlatDataJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.FlatDataLegendPureCoreExtension;
import org.finos.legend.pure.code.core.H2SqlDialectTranslationPureCoreExtension;
import org.finos.legend.pure.code.core.JSONJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.JSONLegendPureCoreExtension;
import org.finos.legend.pure.code.core.M2MJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.MongoDBLegendPureCoreExtension;
import org.finos.legend.pure.code.core.PostgresSqlDialectTranslationPureCoreExtension;
import org.finos.legend.pure.code.core.RelationalJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.RelationalLegendPureCoreExtension;
import org.finos.legend.pure.code.core.ServiceStoreJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.ServiceStoreLegendPureCoreExtension;
import org.finos.legend.pure.code.core.XMLJavaBindingLegendPureCoreExtension;
import org.finos.legend.pure.code.core.XMLLegendPureCoreExtension;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataPelt;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
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
        MutableSet<String> expectedCodeRepositories = Sets.mutable.withAll(getExpectedCodeRepositories());
        MutableSet<String> actual = CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName, Sets.mutable.empty()).select(c -> !c.startsWith("platform"));

        MutableSet<String> extraOnExpected = expectedCodeRepositories.difference(actual);
        MutableSet<String> extraOnActual = actual.difference(expectedCodeRepositories);

        Assert.assertEquals("Expected but not found on actual: ", extraOnExpected, Sets.mutable.empty());
        Assert.assertEquals("Actual missing on expected: ", extraOnActual, Sets.mutable.empty());
    }

    @Test
    public void testPackageableElementProtocolDefineClassifier()
    {
        List<PureProtocolExtension> extensions = PureProtocolExtensionLoader.extensions();

        MutableSet<? extends Class<?>> packageableElementProtocolClasses = Iterate.flatCollect(extensions, extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value)
                        .select(info -> info.getSuperType().equals(PackageableElement.class))
                        .flatCollect(ProtocolSubTypeInfo::getSubTypes)
                        .collect(Pair::getOne), Sets.mutable.empty());

        MutableSet<Class<? extends PackageableElement>> classesWithClassifiers = Iterate.flatCollect(extensions, ext -> ext.getExtraProtocolToClassifierPathMap().keySet(), Sets.mutable.empty());

        for (Class<?> packageableElementProtocolClass : packageableElementProtocolClasses)
        {
            Assert.assertTrue(packageableElementProtocolClass.getName() + " does not have entry on protocolToClassifierPathMap", classesWithClassifiers.remove(packageableElementProtocolClass));
        }
    }

    @Test
    public void testMetadata()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        MetadataPelt metadata = MetadataPelt.fromClassLoader(classLoader, getExpectedCodeRepositories());
        MutableSet<String> expectedClassifiers = Iterate.flatCollect(PureProtocolExtensionLoader.extensions(), ext -> ext.getExtraProtocolToClassifierPathMap().values(), Sets.mutable.empty());
        Assert.assertEquals(
                Lists.fixedSize.empty(),
                expectedClassifiers.select(cl ->
                {
                    try
                    {
                        return metadata.getMetadata(M3Paths.Class, cl) == null;
                    }
                    catch (Exception ignore)
                    {
                        return true;
                    }
                }, Lists.mutable.empty()));
    }

    @Test
    public void testPureModel()
    {
        PureModel pureModel = new PureModel(PureModelContextData.newPureModelContextData(), Identity.getAnonymousIdentity().getName(), DeploymentMode.PROD);
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
                .with(CorePureProtocolExtension.class)
                .with(DataSpaceProtocolExtension.class)
                .with(TDSContainerPureProtocolExtension.class)
                .with(SnowflakeProtocolExtension.class)
                .with(HostedServiceProtocolExtension.class)
                .with(FunctionJarProtocolExtension.class)
                .with(BigQueryFunctionProtocolExtension.class)
                .with(MemSqlFunctionProtocolExtension.class)
                .with(DiagramProtocolExtension.class)
                .with(GenerationProtocolExtension.class)
                .with(PersistenceProtocolExtension.class)
                .with(PersistenceCloudProtocolExtension.class)
                .with(PersistenceRelationalProtocolExtension.class)
                .with(RelationalProtocolExtension.class)
                .with(BigQueryProtocolExtension.class)
                .with(SpannerProtocolExtension.class)
                .with(TrinoProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.pure.v1.SnowflakeProtocolExtension.class)
                .with(DatabricksProtocolExtension.class)
                .with(RedshiftProtocolExtension.class)
                .with(ServiceProtocolExtension.class)
                .with(ServiceStoreProtocolExtension.class)
                .with(AuthenticationProtocolExtension.class)
                .with(TextProtocolExtension.class)
                .with(GraphQLPureProtocolExtension.class)
                .with(SQLExpressionProtocolExtension.class)
                .with(ElasticsearchV7ProtocolExtension.class)
                .with(MongoDBPureProtocolExtension.class)
                .with(org.finos.legend.engine.protocol.deephaven.metamodel.DeephavenProtocolExtension.class)
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
                .with(TDSRelationAccessorGrammarExtension.class)
                .with(SnowflakeGrammarParserExtension.class)
                .with(HostedServiceGrammarParserExtension.class)
                .with(FunctionJarGrammarParserExtension.class)
                .with(BigQueryFunctionGrammarParserExtension.class)
                .with(MemSqlFunctionGrammarParserExtension.class)
                .with(DiagramParserExtension.class)
                .with(ExternalFormatGrammarParserExtension.class)
                .with(GenerationParserExtension.class)
                .with(PersistenceParserExtension.class)
                .with(PersistenceCloudParserExtension.class)
                .with(PersistenceRelationalParserExtension.class)
                .with(DataQualityGrammarParserExtension.class)
                .with(RelationalGrammarParserExtension.class)
                .with(ServiceParserExtension.class)
                .with(AuthenticationGrammarParserExtension.class)
                .with(GraphQLGrammarParserExtension.class)
                .with(SQLExpressionGrammarParserExtension.class)
                .with(ServiceStoreGrammarParserExtension.class)
                .with(TextParserExtension.class)
                .with(ElasticsearchGrammarParserExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBGrammarParserExtension.class)
                .with(DeephavenGrammarParserExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends PureGrammarComposerExtension>> getExpectedGrammarComposerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends PureGrammarComposerExtension>>empty()
                .with(CorePureGrammarComposer.class)
                .with(DataSpaceGrammarComposerExtension.class)
                .with(SnowflakeGrammarComposer.class)
                .with(TDSRelationAccessorGrammarComposerExtension.class)
                .with(HostedServiceGrammarComposer.class)
                .with(FunctionJarGrammarComposer.class)
                .with(PostDeploymentActionGrammarComposer.class)
                .with(BigQueryFunctionGrammarComposer.class)
                .with(MemSqlFunctionGrammarComposer.class)
                .with(DiagramGrammarComposerExtension.class)
                .with(ExternalFormatGrammarComposerExtension.class)
                .with(GenerationGrammarComposerExtension.class)
                .with(PersistenceComposerExtension.class)
                .with(PersistenceCloudComposerExtension.class)
                .with(PersistenceRelationalComposerExtension.class)
                .with(DataQualityGrammarComposerExtension.class)
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
                .with(SQLExpressionGrammarComposerExtension.class)
                .with(AuthenticationGrammarComposerExtension.class)
                .with(TextGrammarComposerExtension.class)
                .with(ElasticsearchGrammarComposerExtension.class)
                .with(DeephavenGrammarComposerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBGrammarComposerExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends CompilerExtension>> getExpectedCompilerExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends CompilerExtension>>empty()
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DiagramCompilerExtension.class)
                .with(SnowflakeCompilerExtension.class)
                .with(HostedServiceCompilerExtension.class)
                .with(FunctionJarCompilerExtension.class)
                .with(BigQueryFunctionCompilerExtension.class)
                .with(MemSqlFunctionCompilerExtension.class)
                .with(TDSAccessorCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DataSpaceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.TextCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.CoreCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ProfileCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.EnumerationCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ClassCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.MeasureCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.AssociationCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.FunctionCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.MappingCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.PackageableRuntimeCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.PackageableConnectionCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SectionIndexCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DataElementCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph.ServiceCompilerExtensionImpl.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ExternalFormatCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.PersistenceCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph.PersistenceCloudCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.persistence.relational.compiler.toPureGraph.PersistenceRelationalCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DataQualityCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationalCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.BigQueryCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SpannerCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.TrinoCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.SnowflakeCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.RedshiftCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.DatabricksCompilerExtension.class)
                .with(org.finos.legend.engine.language.graphQL.grammar.integration.GraphQLCompilerExtension.class)
                .with(SQLCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.compiler.toPureGraph.ServiceStoreCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.AuthenticationCompilerExtension.class)
                .with(org.finos.legend.engine.language.stores.elasticsearch.v7.compiler.ElasticsearchCompilerExtension.class)
                .with(org.finos.legend.engine.language.pure.grammar.integration.MongoDBCompilerExtension.class)
                .with(org.finos.legend.engine.external.format.json.compile.JsonSchemaCompiler.class)
                .with(org.finos.legend.engine.language.deephaven.compiler.DeephavenCompilerExtension.class)
                ;
    }

    protected Iterable<? extends Class<? extends LegendPureCoreExtension>> getExpectedPlanGeneratorExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends LegendPureCoreExtension>>empty()
                .with(JSONJavaBindingLegendPureCoreExtension.class)
                .with(MongoDBLegendPureCoreExtension.class)
                .with(FlatDataLegendPureCoreExtension.class)
                .with(DeephavenLegendPureCoreExtension.class)
                .with(SQLLegendPureCoreExtension.class)
                .with(DeephavenJavaBindingLegendPureCoreExtension.class)
                .with(ElasticsearchLegendPureCoreExtension.class)
                .with(CoreLegendPureCoreExtension.class)
                .with(JSONLegendPureCoreExtension.class)
                .with(RelationalLegendPureCoreExtension.class)
                .with(PostgresSqlDialectTranslationPureCoreExtension.class)
                .with(DuckDbSqlDialectTranslationPureCoreExtension.class)
                .with(H2SqlDialectTranslationPureCoreExtension.class)
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
                .with(SnowflakeM2MUdfArtifactGenerationExtension.class)
                .with(HostedServiceArtifactGenerationExtension.class)
                .with(FunctionActivatorArtifactGenerationExtension.class)
                .with(PowerBIArtifactGenerationExtension.class);
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
                .with("core_external_extensions")
                .with("core_external_query_sql")
                .with("core_functions_unclassified")
                .with("core_functions_variant")
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
                .with("core_external_language_java_feature_based_generation")
                .with("core_external_language_morphir")
                .with("core_external_format_flatdata")
                .with("core_external_format_json")
                .with("core_external_format_openapi")
                .with("core_external_format_powerbi")
                .with("core_external_format_protobuf")
                .with("core_external_format_xml")
                .with("core_external_format_arrow")
                .with("core_external_query_graphql")
                .with("core_external_query_graphql_metamodel")
                .with("core_external_store_relational_postgres_sql_model")
                .with("core_external_store_relational_postgres_sql_model_extensions")
                .with("core_external_query_sql_expression")
                .with("core_external_compiler")
                .with("core_external_execution")
                .with("core_external_language_daml")
                .with("core_external_language_haskell")
                .with("core_function_activator")
                .with("core_functions_standard")
                .with("core_functions_relation")
                .with("core_functions_json")
                .with("core_persistence")
                .with("core_persistence_cloud")
                .with("core_persistence_relational")
                .with("core_dataquality")
                .with("core_relational")
                .with("core_relational_bigquery")
                .with("core_relational_spanner")
                .with("core_relational_trino")
                .with("core_relational_snowflake")
                .with("core_relational_redshift")
                .with("core_relational_clickhouse")
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
                .with("core_snowflake")
                .with("core_bigqueryfunction")
                .with("core_memsqlfunction")
                .with("core_hostedservice")
                .with("core_functionjar")
                .with("core_text_metamodel")
                .with("core_external_language_java")
                .with("core_external_language_java_conventions_essential")
                .with("core_external_language_java_conventions_standard")
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
                .with("core_external_store_relational_sql_planning")
                .with("core_external_store_relational_sql_dialect_translation")
                .with("core_external_store_relational_sql_dialect_translation_duckdb")
                .with("core_external_store_relational_postgres_sql_parser")
                .with("core_deephaven_pure")
                .with("core_deephaven_java_platform_binding")
                .with("core_external_store_relational_sql_dialect_translation_h2")
                .with("core_external_store_relational_sql_dialect_translation_snowflake")
                ;
    }
}
