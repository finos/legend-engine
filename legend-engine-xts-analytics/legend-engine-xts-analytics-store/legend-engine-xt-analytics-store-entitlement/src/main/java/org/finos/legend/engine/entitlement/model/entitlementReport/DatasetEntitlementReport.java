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

package org.finos.legend.engine.entitlement.model.entitlementReport;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = DatasetEntitlementUnsupportedReport.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DatasetEntitlementAccessGrantedReport.class, name = "accessGranted"),
        @JsonSubTypes.Type(value = DatasetEntitlementAccessNotGrantedReport.class, name = "accessNotGranted"),
        @JsonSubTypes.Type(value = DatasetEntitlementUnsupportedReport.class, name = "unsupported"),
        @JsonSubTypes.Type(value = DatasetEntitlementAccessApprovedReport.class, name = "accessApproved"),
        @JsonSubTypes.Type(value = DatasetEntitlementAccessRequestedReport.class, name = "accessRequested"),
        @JsonSubTypes.Type(value = DatasetEntitlementAccessErrorReport.class, name = "errorReport")
})
public abstract class DatasetEntitlementReport
{
    private DatasetSpecification dataset;

    public DatasetEntitlementReport(DatasetSpecification dataset)
    {
        this.dataset = dataset;
    }

    public DatasetEntitlementReport()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public DatasetSpecification getDataset()
    {
        return dataset;
    }
}
