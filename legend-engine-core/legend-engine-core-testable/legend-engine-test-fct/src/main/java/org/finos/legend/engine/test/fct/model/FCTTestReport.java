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


public class FCTTestReport
{
    public FCTTestReport(org.finos.legend.engine.test.fct.model.FCTTestResult testResult)
    {
            this.store = testResult.store;
            this.testType = testResult.testType;
            this.queryFeature = testResult.queryFeature != null ? String.join("_", testResult.queryFeature) : null;
            this.sourceTypeFeature = testResult.sourceTypeFeature != null ? String.join("_", testResult.sourceTypeFeature) : null;
            this.mappingFeature = testResult.mappingFeature != null ? String.join("_", testResult.mappingFeature) : null;
            this.querySubFeature = testResult.querySubFeature != null ? String.join("_", testResult.querySubFeature) : null;
            this.testCollection = testResult.testCollection;

    }

        @JsonProperty("testCollection")
        public String testCollection;

        @JsonProperty("store")
        public String store;

        @JsonProperty("sourceTypeFeature")
        public String sourceTypeFeature;

        @JsonProperty("queryFeature")
        public String  queryFeature;

        @JsonProperty("querySubFeature")
        public String  querySubFeature;

        @JsonProperty("mappingFeature")
        public String mappingFeature;

        @JsonProperty("testType")
        public String testType;

        @JsonProperty("functionName")
        public String  functionName;

        @JsonProperty("assertionType")
        public String  assertionType;

        @JsonProperty("status")
        public String  status;

        @JsonProperty("errorMessage")
        public String  errorMessage;


}
