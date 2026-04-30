// Copyright 2026 Goldman Sachs
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import java.util.List;
import java.util.Set;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DataQualityReconInput
{
    @JsonProperty(required = true)
    public PureModelContext model;
    public String clientVersion = "vX_X_X";
    public LambdaFunction source; //query pointing to source dataset
    public LambdaFunction target; //query pointing to target dataset
    public String packagePath; //optional package path to DataQualityRelationComparison element to get source/target lambdas if not provided directly in the input
    public Set<String> keys; //these must exist on both source and target dataset - can either be primary keys or grouping keys if aggregated hash required. If empty then hash column will be used.
    public boolean aggregatedHash = false; //whether aggregated hash should be created based on the keys provided
    public Set<String> colsForHash; //which columns you want the hash to be calculated on, these columns must exist on both source and target dataset. If empty then will calculate hash on all columns.
    public String sourceHashCol; //if there already exists a column on source that contains the hash that you want to use in recon
    public String targetHashCol; //if there already exists a column on target that contains the hash that you want to use in recon
    public boolean includeColumnValues = false; //whether to include the compared column values in the output alongside keys and digest. Only applies when aggregatedHash is false.
    public boolean runSourceQuery = false;
    public boolean runTargetQuery = false;
    public Long defectLimit; //optional limit on the number of defect rows returned
    public List<ParameterValue> sourceLambdaParameterValues; //parameter values for the source lambda
    public List<ParameterValue> targetLambdaParameterValues; //parameter values for the target lambda

}
