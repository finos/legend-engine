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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;

public class MostCommonMultiplicity
{
    public static Multiplicity mostCommon(MutableList<Multiplicity> multiplicities, PureModel pureModel)
    {
        return multiplicities.size() == 1 ? multiplicities.get(0) : multiplicities.take(multiplicities.size() - 1).injectInto(multiplicities.get(multiplicities.size() - 1), (a, b) -> mostCommon(a, b, pureModel));
    }

    public static Multiplicity mostCommon(Multiplicity a, Multiplicity b, PureModel pureModel)
    {
        if (a == b)
        {
            return a;
        }
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity multiplicity = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity();
        multiplicity.lowerBound = (int) Math.min(a._lowerBound()._value(), b._lowerBound()._value());
        multiplicity.setUpperBound(a._upperBound()._value() == null || b._upperBound()._value() == null ? Integer.MAX_VALUE : (int) Math.max(a._upperBound()._value(), b._upperBound()._value()));
        return pureModel.getMultiplicity(multiplicity);
    }
}
