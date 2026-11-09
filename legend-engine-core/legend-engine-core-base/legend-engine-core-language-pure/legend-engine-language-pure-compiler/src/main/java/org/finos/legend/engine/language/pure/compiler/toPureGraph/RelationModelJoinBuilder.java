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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;

import java.util.List;

final class RelationModelJoinBuilder
{
    private RelationModelJoinBuilder()
    {
    }

    /**
     * The join condition of a property whose entire value is {@code modelJoin(<condition lambda>)}, or null.
     * The rules for where and how modelJoin may be written are legend-pure's.
     */
    static LambdaFunction resolveJoinCondition(RelationFunctionPropertyMapping propertyMapping)
    {
        if (propertyMapping.valueFn == null || propertyMapping.valueFn.body.size() != 1 || !(propertyMapping.valueFn.body.get(0) instanceof AppliedFunction))
        {
            return null;
        }
        AppliedFunction call = (AppliedFunction) propertyMapping.valueFn.body.get(0);
        List<ValueSpecification> parameters = call.parameters;
        boolean isModelJoin = "modelJoin".equals(call.function) || "meta::pure::functions::relation::modelJoin".equals(call.function);
        return isModelJoin && parameters.size() == 1 && parameters.get(0) instanceof LambdaFunction ? (LambdaFunction) parameters.get(0) : null;
    }
}
