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

import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

import java.util.List;
import java.util.Map;

public class DataSpaceAnalysisResult
{
    public PureModelContextData diagramsModel;
    public Map<String, DataSpaceExecutionContextAnalysisResult> executionContexts;

    public DataSpaceAnalysisResult(PureModelContextData diagramsModel, Map<String, DataSpaceExecutionContextAnalysisResult> executionContexts)
    {
        this.diagramsModel = diagramsModel;
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
}
