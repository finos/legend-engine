// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.test.fct.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FCTTestResult
{
    @JsonProperty("store")
    public String store;


    @JsonProperty("testType")
    public String testType;

    @JsonProperty("sourceTypeFeature")
    public List<String> sourceTypeFeature;

    @JsonProperty("queryFeature")
    public List<String> queryFeature;

    @JsonProperty("mappingFeature")
    public List<String> mappingFeature;

    @JsonProperty("querySubFeature")
    public List<String> querySubFeature;

    @JsonProperty("featureTests")
    public List<FeatureTest> featureTests;
    @JsonProperty("testCollection")
    public String testCollection;
}