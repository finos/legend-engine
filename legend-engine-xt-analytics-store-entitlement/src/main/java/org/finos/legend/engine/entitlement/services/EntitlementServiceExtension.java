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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.pac4j.core.profile.CommonProfile;

import java.util.Collections;
import java.util.List;

public interface EntitlementServiceExtension
{
   default List<DatasetSpecification> generateDatasetSpecifications(Lambda query, Runtime runtime, String mapping, PureModelContext model, ModelManager modelManager, MutableList<CommonProfile> profiles)
   {
      return Collections.emptyList();
   }

   default List<DatasetEntitlementReport> generateDatasetEntitlementReports(Lambda query, Runtime runtime, String mapping, PureModelContext model, ModelManager modelManager, MutableList<CommonProfile> profiles)
   {
      return Collections.emptyList();
   }
}