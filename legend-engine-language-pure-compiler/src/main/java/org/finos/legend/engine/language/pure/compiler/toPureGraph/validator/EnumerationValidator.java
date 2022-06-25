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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnumerationValidator
{
    public void validate(PureModel pureModel, PureModelContextData pureModelContextData)
    {
        // Create map of elements in V1 since when we try to resolve the generalization in M3, we will get stuffs like Annotated Element, PackageableElement
        // Also using V1 element is more convenient for extracting the source information purpose
        Map<String, Enumeration> elements = new LinkedHashMap<>(); // ensure we validate element in order they are inserted in the pure model context data
        LazyIterate.selectInstancesOf(pureModelContextData.getElements(), Enumeration.class)
                .forEach(_class -> elements.put(pureModel.buildPackageString(_class._package, _class.name), _class));
        this.validateEnumValues(pureModel, elements);
    }

    private void validateEnumValues(PureModel pureModel, Map<String, Enumeration> elements)
    {
        elements.values().forEach(el ->
        {
            MutableMultimap<String, EnumValue> val = Iterate.groupBy(el.values, p -> p.value);
            pureModel.addWarnings(val.multiValuesView().flatCollect(a -> a.size() > 1 ? Lists.mutable.with(new Warning(a.getFirst().sourceInformation, "Found duplicated value '" + a.getFirst().value + "' in enumeration '" + pureModel.buildPackageString(el._package, el.name) + "'")) : Lists.mutable.empty()));
        });
    }
}
