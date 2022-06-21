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

package org.finos.legend.engine.api.analytics.model;

import java.util.List;
import java.util.Map;

public class DataSpaceAnalysisResult
{
    public Map<String, DataSpaceDiagramAnalysisResult> diagrams;
    public Map<String, DataSpaceExecutionContextAnalysisResult> executionContexts;

    public DataSpaceAnalysisResult(Map<String, DataSpaceDiagramAnalysisResult> diagrams, Map<String, DataSpaceExecutionContextAnalysisResult> executionContexts)
    {
        this.diagrams = diagrams;
        this.executionContexts = executionContexts;
    }

    public static class DataSpaceExecutionContextAnalysisResult
    {
        public List<String> runtimes;

        public DataSpaceExecutionContextAnalysisResult(List<String> runtimes)
        {
            this.runtimes = runtimes;
        }
    }

    public static class DataSpaceDiagramAnalysisResult
    {
        public List<String> profiles;
        public List<String> enumerations;
        public List<String> classes;

        public DataSpaceDiagramAnalysisResult(List<String> profiles, List<String> enumerations, List<String> classes)
        {
            this.profiles = profiles;
            this.enumerations = enumerations;
            this.classes = classes;
        }
    }
}
