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

package org.finos.legend.engine.api.analytics.model;

import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;

import java.util.List;

public class EntitlementReportAnalyticsInput
{
    private StoreEntitlementAnalyticsInput storeEntitlementAnalyticsInput;
    private List<DatasetSpecification> reports;

    public EntitlementReportAnalyticsInput(StoreEntitlementAnalyticsInput storeEntitlementAnalyticsInput, List<DatasetSpecification> reports)
    {
        this.reports = reports;
        this.storeEntitlementAnalyticsInput = storeEntitlementAnalyticsInput;
    }

    public List<DatasetSpecification> getReports()
    {
        return reports;
    }

    public StoreEntitlementAnalyticsInput getStoreEntitlementAnalyticsInput()
    {
        return storeEntitlementAnalyticsInput;
    }

    public EntitlementReportAnalyticsInput()
    {

    }
}
