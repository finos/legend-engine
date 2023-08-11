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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class Domain
{
    public List<PackageableElement> classes = Lists.mutable.empty();
    public List<PackageableElement> associations = Lists.mutable.empty();
    public List<PackageableElement> enums = Lists.mutable.empty();
    public List<PackageableElement> profiles = Lists.mutable.empty();
    public List<PackageableElement> functions = Lists.mutable.empty();
    public List<PackageableElement> measures = Lists.mutable.empty();
}
