// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;

import java.util.List;

public class DataQualityExecuteTrialInput
{
    public String clientVersion;
    @JsonProperty(required = true)
    public PureModelContext model;
    @JsonProperty
    public String packagePath;
    public Integer defectsLimit;
    public List<ParameterValue> lambdaParameterValues;
    public String validationName;
    public Boolean runQuery;
}
