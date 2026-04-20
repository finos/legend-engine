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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataQualitySampleValuesInput
{
    public String clientVersion;
    @JsonProperty(required = true)
    public PureModelContext model;

    /**
     * Inline relation query. Mutually exclusive with {@link #functionPath}.
     * The lambda must return a Relation (i.e. end with a project/extend/filter/etc.).
     */
    @JsonProperty
    public org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction query;

    /**
     * Fully-qualified path to a ConcreteFunctionDefinition that returns a Relation.
     * Mutually exclusive with {@link #query}.
     * Example: "demo::myRelationFunction__Relation_1_"
     */
    @JsonProperty
    public String functionPath;

    /**
     * Maximum number of top-ranked values to return per column.
     * Optional - when absent a sensible default from within the PURE layer is used.
     */
    @JsonProperty
    public Integer maxNumberOfSampleValues;

    public List<ParameterValue> lambdaParameterValues;
}
