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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourcePartition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolutionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_RecordSource;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_RecordSourcePartition;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_RecordSourcePartition_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_RecordSource_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_IdentityResolution;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_IdentityResolution_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_ResolutionQuery;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_ResolutionQuery_Impl;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HelperMasterRecordDefinitionBuilder
{
    private static final String MASTERY_PACKAGE_PREFIX = "meta::pure::mastery::metamodel";
    private static final IdentityResolutionBuilder IDENTITY_RESOLUTION_BUILDER = new IdentityResolutionBuilder();

    private HelperMasterRecordDefinitionBuilder()
    {
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class buildModelClass(MasterRecordDefinition val, CompileContext context)
    {
        Class<?> modelClass = context.resolveClass(val.modelClass);
        return modelClass;
    }

    private static String determineFullPath(Type type)
    {
        Deque<String> deque = new ArrayDeque<>();
        Package currentPackage = ((PackageableElement) type)._package();
        while (!currentPackage._name().equals("Root"))
        {
            deque.push(currentPackage._name());
            currentPackage = currentPackage._package();
        }

        return Iterate.makeString(deque, "", "::", "::" + type._name());
    }

    public static Root_meta_pure_mastery_metamodel_identity_IdentityResolution buildIdentityResolution(IdentityResolution identityResolution, CompileContext context)
    {
        IDENTITY_RESOLUTION_BUILDER.context = context;
        return identityResolution.accept(IDENTITY_RESOLUTION_BUILDER);
    }

    private static class IdentityResolutionBuilder implements IdentityResolutionVisitor<Root_meta_pure_mastery_metamodel_identity_IdentityResolution>
    {
        private CompileContext context;

        @Override
        public Root_meta_pure_mastery_metamodel_identity_IdentityResolution visit(IdentityResolution protocolVal)
        {
            Root_meta_pure_mastery_metamodel_identity_IdentityResolution_Impl resImpl = new Root_meta_pure_mastery_metamodel_identity_IdentityResolution_Impl("");
            resImpl._modelClass(context.resolveClass(protocolVal.modelClass));
            resImpl._resolutionQueriesAddAll(ListIterate.flatCollect(protocolVal.resolutionQueries, this::visitResolutionQuery));
            return resImpl;
        }

        private Iterable<Root_meta_pure_mastery_metamodel_identity_ResolutionQuery> visitResolutionQuery(ResolutionQuery protocolQuery)
        {
            ArrayList<Root_meta_pure_mastery_metamodel_identity_ResolutionQuery> list = new ArrayList<>();
            Root_meta_pure_mastery_metamodel_identity_ResolutionQuery resQuery = new Root_meta_pure_mastery_metamodel_identity_ResolutionQuery_Impl("");

            String KEY_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::identity::ResolutionKeyType";
            resQuery._keyType(context.resolveEnumValue(KEY_TYPE_FULL_PATH, protocolQuery.keyType.name()));
            resQuery._precedence(protocolQuery.precedence);

            ListIterate.forEachWithIndex(protocolQuery.queries, (lambda, i) ->
            {
                resQuery._queriesAdd(HelperValueSpecificationBuilder.buildLambda(lambda, context));
            });

            list.add(resQuery);
            return list;
        }
    }

    public static RichIterable<Root_meta_pure_mastery_metamodel_RecordSource> buildRecordSources(List<RecordSource> recordSources, CompileContext context)
    {
        return ListIterate.collect(recordSources, n -> n.accept(new RecordSourceBuilder(context)));
    }

    private static class RecordSourceBuilder implements RecordSourceVisitor<Root_meta_pure_mastery_metamodel_RecordSource>
    {
        private CompileContext context;

        public RecordSourceBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_mastery_metamodel_RecordSource visit(RecordSource protocolSource)
        {
            String KEY_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::RecordSourceStatus";
            Root_meta_pure_mastery_metamodel_RecordSource pureSource = new Root_meta_pure_mastery_metamodel_RecordSource_Impl("");
            pureSource._id(protocolSource.id);
            pureSource._description(protocolSource.description);
            pureSource._status(context.resolveEnumValue(KEY_TYPE_FULL_PATH, protocolSource.status.name()));
            pureSource._parseService(buildService(protocolSource.parseService, protocolSource, context));
            pureSource._transformService(buildService(protocolSource.transformService, protocolSource, context));
            pureSource._sequentialData(protocolSource.sequentialData);
            pureSource._stagedLoad(protocolSource.stagedLoad);
            pureSource._createPermitted(protocolSource.createPermitted);
            pureSource._createBlockedException(protocolSource.createBlockedException);
            pureSource._tags(ListIterate.collect(protocolSource.tags, n -> n.toString()));
            pureSource._partitions(ListIterate.collect(protocolSource.partitions, n -> this.visitPartition(n)));
            return pureSource;
        }

        public static Root_meta_legend_service_metamodel_Service buildService(String service, RecordSource protocolSource, CompileContext context)
        {
            String servicePath = service.substring(0, service.lastIndexOf("::"));
            String serviceName = service.substring(service.lastIndexOf("::") + 2);

            PackageableElement packageableElement = context.pureModel.getOrCreatePackage(servicePath)._children().detect(c -> serviceName.equals(c._name()));
            if (packageableElement instanceof Root_meta_legend_service_metamodel_Service)
            {
                return (Root_meta_legend_service_metamodel_Service) packageableElement;
            }
            throw new EngineException(String.format("Service '%s' is not defined", service), protocolSource.sourceInformation, EngineErrorType.COMPILATION);
        }

        private Root_meta_pure_mastery_metamodel_RecordSourcePartition visitPartition(RecordSourcePartition protocolPartition)
        {
            Root_meta_pure_mastery_metamodel_RecordSourcePartition purePartition = new Root_meta_pure_mastery_metamodel_RecordSourcePartition_Impl("");
            purePartition._id(protocolPartition.id);
            purePartition._tags(ListIterate.collect(protocolPartition.tags, n -> n.toString()));
            return purePartition;
        }
    }
}