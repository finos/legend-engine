// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.dataProvider.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_DataProvider_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_Trigger;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MasteryCompilerExtension implements IMasteryCompilerExtension
{
    public static final String AGGREGATOR = "Aggregator";
    public static final String REGULATOR = "Regulator";
    public static final String EXCHANGE = "Exchange";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mastery");
    }

    @Override
    public CompilerExtension build()
    {
        return new MasteryCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                MasterRecordDefinition.class,
                Lists.fixedSize.with(Service.class, Mapping.class, Binding.class, PackageableConnection.class, DataProvider.class, Connection.class),
                //First Pass instantiate - does not include Pure class properties on classes
                (masterRecordDefinition, context) -> new Root_meta_pure_mastery_metamodel_MasterRecordDefinition_Impl(masterRecordDefinition.name)
                        ._name(masterRecordDefinition.name)
                        ._modelClass(HelperMasterRecordDefinitionBuilder.buildModelClass(masterRecordDefinition, context)),
                //Second Pass stitched together - can access Pure class properties now.
                (masterRecordDefinition, context) ->
                {
                    Root_meta_pure_mastery_metamodel_MasterRecordDefinition pureMasteryMetamodelMasterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) context.pureModel.getOrCreatePackage(masterRecordDefinition._package)._children().detect(c -> masterRecordDefinition.name.equals(c._name()));
                    pureMasteryMetamodelMasterRecordDefinition._identityResolution(HelperMasterRecordDefinitionBuilder.buildIdentityResolution(masterRecordDefinition.identityResolution, masterRecordDefinition.modelClass, context));
                    pureMasteryMetamodelMasterRecordDefinition._sources(HelperMasterRecordDefinitionBuilder.buildRecordSources(masterRecordDefinition.sources, context));
                    pureMasteryMetamodelMasterRecordDefinition._postCurationEnrichmentService(BuilderUtil.buildService(masterRecordDefinition.postCurationEnrichmentService, context, masterRecordDefinition.sourceInformation));
                    pureMasteryMetamodelMasterRecordDefinition._exceptionWorkflowTransformService(BuilderUtil.buildService(masterRecordDefinition.exceptionWorkflowTransformService, context, masterRecordDefinition.sourceInformation));
                    pureMasteryMetamodelMasterRecordDefinition._elasticSearchTransformService(BuilderUtil.buildService(masterRecordDefinition.elasticSearchTransformService, context, masterRecordDefinition.sourceInformation));
                    pureMasteryMetamodelMasterRecordDefinition._publishToElasticSearch(masterRecordDefinition.publishToElasticSearch);
                    pureMasteryMetamodelMasterRecordDefinition._collectionEqualities(HelperMasterRecordDefinitionBuilder.buildCollectionEqualities(masterRecordDefinition.collectionEqualities, context));
                    if (masterRecordDefinition.precedenceRules != null)
                    {
                        List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
                        Set<String> dataProviderTypes = Iterate.flatCollect(extensions, IMasteryCompilerExtension::getValidDataProviderTypes, Sets.mutable.of());
                        pureMasteryMetamodelMasterRecordDefinition._precedenceRules(HelperMasterRecordDefinitionBuilder.buildPrecedenceRules(masterRecordDefinition, context, dataProviderTypes));
                    }
                }),

                Processor.newProcessor(
                        DataProvider.class,
                        Lists.fixedSize.empty(),
                        (dataProvider, context) -> new Root_meta_pure_mastery_metamodel_DataProvider_Impl(dataProvider.name)
                                ._name(dataProvider.name)
                                ._dataProviderId(dataProvider.dataProviderId)
                                ._dataProviderType(dataProvider.dataProviderType)
                       ),

                Processor.newProcessor(
                        MasteryRuntime.class,
                        Lists.fixedSize.with(MasterRecordDefinition.class),
                        (masteryRuntime, context) ->
                        {
                            List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
                            List<Function2<MasteryRuntime, CompileContext, Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime>> processors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraMasteryRuntimeProcessors);
                            return IMasteryCompilerExtension.process(masteryRuntime, processors, context);
                        }),

                Processor.newProcessor(
                        Connection.class,
                        Lists.fixedSize.with(Service.class, Mapping.class, Binding.class, PackageableConnection.class),
                        (connection, context) ->
                        {
                            List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
                            List<Function2<Connection, CompileContext, Root_meta_pure_mastery_metamodel_connection_Connection>> processors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraMasteryConnectionProcessors);
                            return IMasteryCompilerExtension.process(connection, processors, context);
                        }));
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_pure_mastery_metamodel_connection_Connection>> getExtraMasteryConnectionProcessors()
    {
        return Collections.singletonList(HelperConnectionBuilder::buildConnection);
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Collections.singletonList(HelperAuthenticationBuilder::buildAuthentication);
    }

    @Override
    public List<Function2<Trigger, CompileContext, Root_meta_pure_mastery_metamodel_trigger_Trigger>> getExtraTriggerProcessors()
    {
        return Collections.singletonList(HelperTriggerBuilder::buildTrigger);
    }

    @Override
    public List<Function2<AcquisitionProtocol, CompileContext, Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol>> getExtraAcquisitionProtocolProcessors()
    {
        return Collections.singletonList(HelperAcquisitionBuilder::buildAcquisition);
    }

    @Override
    public Set<String> getValidDataProviderTypes()
    {
        return Sets.fixedSize.of(AGGREGATOR, REGULATOR, EXCHANGE);
    }

    @Override
    public List<Function3<String, SourceInformation, CompileContext, PackageableElement>> getExtraModelGenerationSpecificationResolvers()
    {
        return Collections.singletonList((path, sourceInformation, context) ->
        {
            PackageableElement element = context.resolvePackageableElement(path);
            return element instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition ? element : null;
        });
    }
}
