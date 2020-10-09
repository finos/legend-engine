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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.List;

import static org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData.uniqueUnion;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Domain
{
    public List<PackageableElement> classes = Lists.mutable.empty();
    public List<PackageableElement> associations = Lists.mutable.empty();
    public List<PackageableElement> enums = Lists.mutable.empty();
    public List<PackageableElement> profiles = Lists.mutable.empty();
    public List<PackageableElement> functions = Lists.mutable.empty();
    public List<PackageableElement> measures = Lists.mutable.empty();

    @JsonIgnore
    public Domain combine(Domain data)
    {
        if (data == null)
        {
            return this;
        }
        Domain result = new Domain();
        result.classes = uniqueUnion(this.classes, data.classes);
        result.associations = uniqueUnion(this.associations, data.associations);
        result.enums = uniqueUnion(this.enums, data.enums);
        result.profiles = uniqueUnion(this.profiles, data.profiles);
        result.functions = uniqueUnion(this.functions, data.functions);
        result.measures = uniqueUnion(this.measures, data.measures);
        return result;
    }

    @JsonIgnore
    public boolean isEmpty()
    {
        return this.classes.isEmpty()
                && this.associations.isEmpty()
                && this.enums.isEmpty()
                && this.profiles.isEmpty()
                && this.functions.isEmpty()
                && this.measures.isEmpty();
    }

    @JsonIgnore
    public static List<Domain> partition(Domain inputModel, int parts)
    {
        List<Domain> result = org.eclipse.collections.api.factory.Lists.mutable.empty();
        for (int i = 0; i < parts; i++)
        {
            result.add(new Domain());
        }
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.classes).forEach(c -> result.get(inputModel.classes.indexOf(c) % parts).classes.add(c));
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.associations).forEach(c -> result.get(inputModel.associations.indexOf(c) % parts).associations.add(c));
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.enums).forEach(c -> result.get(inputModel.enums.indexOf(c) % parts).enums.add(c));
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.profiles).forEach(c -> result.get(inputModel.profiles.indexOf(c) % parts).profiles.add(c));
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.functions).forEach(c -> result.get(inputModel.functions.indexOf(c) % parts).functions.add(c));
        org.eclipse.collections.api.factory.Lists.mutable.withAll(inputModel.measures).forEach(c -> result.get(inputModel.measures.indexOf(c) % parts).measures.add(c));
        return result;
    }
}
