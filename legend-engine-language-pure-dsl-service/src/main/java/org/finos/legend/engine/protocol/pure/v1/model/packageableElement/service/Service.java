// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.Authorizer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service extends PackageableElement
{
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();

    public String pattern;
    public List<String> owners;
    public String documentation;
    public boolean autoActivateUpdates = true;
    public Authorizer authorizer;
    public Execution execution;
    public ServiceTest test;
    public List<ServiceTag> tags;

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}