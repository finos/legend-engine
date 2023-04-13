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

package org.finos.legend.engine.generation.analytics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportInfo;

import java.util.Collections;
import java.util.List;

public class DataSpaceAnalysisResult
{
    public String name;
    @JsonProperty(value = "package")
    public String _package;
    public String path;
    public List<DataSpaceTaggedValueInfo> taggedValues = Collections.emptyList();
    public List<DataSpaceStereotypeInfo> stereotypes = Collections.emptyList();

    public String title;
    public String description;
    public DataSpaceSupportInfo supportInfo;

    public List<DataSpaceExecutionContextAnalysisResult> executionContexts = Collections.emptyList();
    public String defaultExecutionContext;

    public PureModelContextData model;

    public List<String> elements = Collections.emptyList();

    public List<DataSpaceExecutableAnalysisResult> executables = Collections.emptyList();
    public List<DataSpaceDiagramAnalysisResult> diagrams = Collections.emptyList();
    public List<DataSpaceModelDocumentationEntry> elementDocs = Collections.emptyList();
}
