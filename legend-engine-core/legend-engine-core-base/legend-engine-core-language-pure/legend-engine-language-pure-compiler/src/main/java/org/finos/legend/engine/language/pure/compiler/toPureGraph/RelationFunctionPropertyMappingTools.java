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

import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

/**
 * Engine-side mirror of the legend-pure helper of the same name — recognises the
 * bare-column fast path on a relation property mapping's {@code _valueFn} lambda.
 * <p>
 * The bare-column source form ({@code firstName: FIRSTNAME}) is lowered at SecondPass
 * to the lambda {@code { $src.FIRSTNAME}}.  Downstream consumers (SQL push-down,
 * IDE display, debug) often need to recognise that fast path; this class encapsulates
 * the pattern match so each consumer doesn't have to reimplement it.
 * <p>
 * The matcher is deliberately conservative: a complex expression that happens to
 * evaluate to a single column reference at runtime is NOT matched here.  Only the
 * literal one-step column accessor pattern.
 */
public final class RelationFunctionPropertyMappingTools
{
    private RelationFunctionPropertyMappingTools()
    {
    }

    /**
     * If the lambda body of the given mapping is exactly a single column accessor on
     * {@code $src} (i.e. it was authored as the bare-column form or as the equivalent
     * {@code $src.<col>} expression), returns the column name.  Returns
     * {@link Optional#empty()} for anything else (arithmetic, function calls,
     * conditionals, multi-step expressions, missing {@code _valueFn}).
     */
    public static Optional<String> asColumnRef(RelationFunctionPropertyMapping pm)
    {
        if (pm == null || pm._valueFn() == null)
        {
            return Optional.empty();
        }
        LambdaFunction<?> valueFn = pm._valueFn();
        ListIterable<? extends ValueSpecification> body = valueFn._expressionSequence().toList();
        if (body.size() != 1)
        {
            return Optional.empty();
        }
        ValueSpecification only = body.get(0);
        if (!(only instanceof SimpleFunctionExpression))
        {
            return Optional.empty();
        }
        SimpleFunctionExpression sfe = (SimpleFunctionExpression) only;
        // Post-processed `$src.<col>` shape:
        //   SimpleFunctionExpression {
        //     propertyName: InstanceValue("<col>"),
        //     parametersValues: [VariableExpression("src")],
        //     func: Column            // resolved column accessor
        //   }
        MutableList<? extends ValueSpecification> args = Lists.mutable.withAll(sfe._parametersValues());
        if (args.size() != 1 || !(args.get(0) instanceof VariableExpression))
        {
            return Optional.empty();
        }
        VariableExpression srcRef = (VariableExpression) args.get(0);
        if (!"src".equals(srcRef._name()))
        {
            return Optional.empty();
        }
        CoreInstance propNameInstance = sfe.getValueForMetaPropertyToOne("propertyName");
        if (propNameInstance == null)
        {
            return Optional.empty();
        }
        ListIterable<? extends CoreInstance> nameVals = propNameInstance.getValueForMetaPropertyToMany("values").toList();
        if (nameVals.size() != 1)
        {
            return Optional.empty();
        }
        return Optional.of(nameVals.get(0).getName());
    }
}

