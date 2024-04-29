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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

public class StoreEntitlementAnalyticsInput
{
    @JsonProperty
    public Lambda query;
    @JsonProperty
    public String runtimePath;
    @JsonProperty
    public String mappingPath;
    @JsonProperty
    public PureModelContext model;
    @JsonProperty
    public String clientVersion;

    @JsonCreator
    public StoreEntitlementAnalyticsInput(@JsonProperty("query") Lambda query, @JsonProperty("runtime") String runtimePath, @JsonProperty("mapping") String mappingPath, @JsonProperty("model") PureModelContext model, @JsonProperty("clientVersion") String clientVersion)
    {
        this.query = query;
        this.runtimePath = runtimePath;
        this.mappingPath = mappingPath;
        this.model = model;
        this.clientVersion = clientVersion;
    }
}
