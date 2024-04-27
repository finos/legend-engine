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

import org.eclipse.collections.api.block.function.Function0;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.extension.LegendModuleSpecificExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.Collections;
import java.util.List;

public interface EntitlementServiceExtension extends LegendModuleSpecificExtension
{
   default List<DatasetSpecification> generateDatasetSpecifications(Lambda query, String runtimePath, Root_meta_core_runtime_Runtime runtime, String mappingPath, Mapping mapping, PureModelContext model, PureModel pureModel)
   {
      return Collections.emptyList();
   }

   default List<DatasetEntitlementReport> generateDatasetEntitlementReports(List<DatasetSpecification> datasets, Lambda query, String runtimePath, Root_meta_core_runtime_Runtime runtime, String mappingPath, Mapping mapping, PureModelContext model, PureModel pureModel, Identity identity)
   {
      return Collections.emptyList();
   }

   default List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
   {
      return Collections.emptyList();
   }
}
