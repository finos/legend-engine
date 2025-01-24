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

package org.finos.legend.engine.application.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query
{
    public String id;
    public String name;
    public String description;
    public String groupId;
    public String artifactId;
    public String versionId;
    public String originalVersionId;
    @Deprecated
    public String mapping;
    @Deprecated
    public String runtime;
    public QueryExecutionContext executionContext;
    public String content;
    public Long lastUpdatedAt;
    public Long createdAt;
    public Long lastOpenAt;

    public List<TaggedValue> taggedValues;
    public List<StereotypePtr> stereotypes;

    public List<QueryParameterValue> defaultParameterValues;

    // TODO: this is a very simple ownership mechanism
    // where we only allow a single owner
    public String owner;

    public Map<String, ?> gridConfig;
}
