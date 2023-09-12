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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordSource
{
    public String parseService;
    public String transformService;
    public List<String> tags = new ArrayList<String>();
    public List<RecordSourcePartition> partitions = Collections.emptyList();
    public String id;
    public String description;
    public RecordSourceStatus status;
    public Boolean sequentialData;
    public Boolean stagedLoad;
    public Boolean createPermitted;
    public Boolean createBlockedException;
    public Boolean allowFieldDelete;
    public RecordService recordService;
    public String dataProvider;
    public Trigger trigger;
    public Authorization authorization;
    public Boolean raiseExceptionWorkflow;
    public Profile runProfile;
    public Integer timeoutInMinutes;
    public List<RecordSourceDependency> dependencies;
    public SourceInformation sourceInformation;

    public List<String> getTags()
    {
        return tags;
    }

    public <T> T accept(RecordSourceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
