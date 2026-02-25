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

package org.finos.legend.engine.application.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.shared.mongo.model.StoredAuditInformation;
import org.finos.legend.engine.shared.mongo.model.StoredVersionedAssetContent;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationStoredQuery implements StoredVersionedAssetContent<String>
{
    public String id;
    public String name;
    public String description;
    public String groupId;
    public String artifactId;
    public String versionId;
    public String originalVersionId;
    public QueryExecutionContext executionContext;
    public String content;
    public List<TaggedValue> taggedValues;
    public List<StereotypePtr> stereotypes;
    public List<QueryParameterValue> defaultParameterValues;
    public Map<String, ?> gridConfig;
    public Long lastOpenAt;
    public StoredAuditInformation audit;

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public StoredAuditInformation getAudit()
    {
        return audit;
    }

    @Override
    public void setAudit(StoredAuditInformation audit)
    {
        this.audit = audit;
    }

    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
