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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementAccessApprovedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementAccessGrantedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementAccessNotGrantedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementAccessRequestedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementUnsupportedReport;
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;

import java.util.List;

public class BaseEntitlementServiceExtension implements EntitlementServiceExtension
{
    @Override
    public List<Function0<List<EntitlementSubtypeInfo<?>>>> getExtraEntitlementSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                EntitlementSubtypeInfo.newBuilder(DatasetEntitlementReport.class)
                        .withSubtype(DatasetEntitlementAccessGrantedReport.class, "accessGranted")
                        .withSubtype(DatasetEntitlementAccessNotGrantedReport.class, "accessNotGranted")
                        .withSubtype(DatasetEntitlementUnsupportedReport.class, "unsupported")
                        .withSubtype(DatasetEntitlementAccessApprovedReport.class, "accessApproved")
                        .withSubtype(DatasetEntitlementAccessRequestedReport.class, "accessRequested")
                        .build()
        ));
    }
}
