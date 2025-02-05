//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.entitlement.services;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementAccessGrantedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementUnsupportedReport;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.entitlement.model.specification.RelationalDatabaseTableSpecification;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.pure.generated.Root_meta_analytics_store_entitlements_TableWithType;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.core_relational_store_entitlement_relational_relationalAnalyzer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RelationalDatabaseEntitlementServiceExtension implements EntitlementServiceExtension
{
    public RelationalDatabaseEntitlementServiceExtension()
    {
    }

    @Override
    public List<DatasetSpecification> generateDatasetSpecifications(Lambda query, String runtimePath, Root_meta_core_runtime_Runtime runtime, String mappingPath, Mapping mapping, PureModelContext model, PureModel pureModel)
    {
        RichIterable<? extends Root_meta_analytics_store_entitlements_TableWithType> tablesWithType =
                query == null ?
                        core_relational_store_entitlement_relational_relationalAnalyzer.Root_meta_analytics_store_entitlements_getGeneralRelationalTablesFromMapping_Mapping_1__Runtime_1__TableWithType_MANY_(mapping, runtime, pureModel.getExecutionSupport())
                        : core_relational_store_entitlement_relational_relationalAnalyzer.Root_meta_analytics_store_entitlements_getGeneralRelationalTablesFromQuery_FunctionDefinition_1__Mapping_1__Runtime_1__TableWithType_MANY_(buildPureLambda(query, pureModel), mapping, runtime, pureModel.getExecutionSupport());
        RichIterable<? extends RelationalDatabaseTableSpecification> specifications = tablesWithType.collect(tableWithType ->
                new RelationalDatabaseTableSpecification(tableWithType._table()._schema()._name() + "." + tableWithType._table()._name(), tableWithType._type(), tableWithType._table()._schema()._database()._name(), tableWithType._table()._schema()._name(), tableWithType._table()._name())
        );
        return new ArrayList<>(specifications.toList());
    }

    @Override
    public List<DatasetEntitlementReport> generateDatasetEntitlementReports(List<DatasetSpecification> datasets, Lambda query, String runtimePath, Root_meta_core_runtime_Runtime runtime, String mappingPath, Mapping mapping, PureModelContext model, PureModel pureModel, Identity identity)
    {
        return datasets.stream().filter(specification -> specification instanceof RelationalDatabaseTableSpecification).map(specification -> specification.getType().equals(DatabaseType.H2.name()) ? new DatasetEntitlementAccessGrantedReport(specification) : new DatasetEntitlementUnsupportedReport(specification)).collect(Collectors.toList());
    }

    private LambdaFunction<?> buildPureLambda(Lambda lambda, PureModel pureModel)
    {
        return HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(DatasetSpecification.class)
                        .withSubtype(RelationalDatabaseTableSpecification.class, "relationalDatabaseTable")
                        .build()
        ));
    }
}
