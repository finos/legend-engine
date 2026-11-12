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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

/**
 * Warns that the arguments of a {@code join} do not agree on the kind of join it is.
 *
 * <p>The first argument decides the kind. A join of two relations takes a {@code JoinKind} and a join of two
 * tabular datasets a {@code JoinType}, and both sides must be of the same kind. Compilation has always resolved
 * a call that mixes them up, so such a call is reported here rather than rejected. A call whose arguments
 * cannot be told apart at all, or that has the wrong number of them, is still an error. An argument whose type
 * could not be determined is not reported.
 */
final class JoinArgumentsDetector
{
    private static final String RELATION_JOIN_PREFIX = "meta::pure::functions::relation::join_";
    private static final String TDS_JOIN_PREFIX = "meta::pure::tds::join_";
    private static final String TABULAR_DATA_SET = "meta::pure::tds::TabularDataSet";
    private static final String JOIN_KIND = "meta::pure::functions::relation::JoinKind";
    private static final String JOIN_TYPE = "meta::relational::metamodel::join::JoinType";
    private static final String SUFFIX = " This is accepted for now and will be rejected in a future release.";

    private JoinArgumentsDetector()
    {
    }

    static void check(SimpleFunctionExpression expression, PureModel pureModel)
    {
        if (!(expression._func() instanceof PackageableElement) || !((PackageableElement) expression._func())._name().startsWith("join_"))
        {
            return;
        }
        String path = HelperModelBuilder.getElementFullPath((PackageableElement) expression._func(), pureModel.getExecutionSupport());
        MutableList<? extends ValueSpecification> arguments = Lists.mutable.withAll(expression._parametersValues());
        if ((!path.startsWith(RELATION_JOIN_PREFIX) && !path.startsWith(TDS_JOIN_PREFIX)) || arguments.size() < 3)
        {
            return;
        }
        ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
        Type left = rawType(arguments.get(0));
        Type relation = pureModel.getType(M3Paths.Relation);
        Type tabularDataSet = pureModel.getType(TABULAR_DATA_SET);
        boolean leftIsRelation = left != null && processorSupport.type_subTypeOf(left, relation);
        if (!leftIsRelation && (left == null || !processorSupport.type_subTypeOf(left, tabularDataSet)))
        {
            return;
        }
        String kind = leftIsRelation ? "Relation" : "TabularDataSet";
        Type expectedSide = leftIsRelation ? relation : tabularDataSet;
        Type expectedJoin = pureModel.getType(leftIsRelation ? JOIN_KIND : JOIN_TYPE);

        Type right = rawType(arguments.get(1));
        if (right != null && !processorSupport.type_subTypeOf(right, expectedSide))
        {
            warn(pureModel, arguments.get(1), expression, "Argument 2 of join() is a " + right.getName() + ", but the first argument is a " + kind + ", so a " + kind + " is expected.");
        }
        Type join = rawType(arguments.get(2));
        if (join != null && !processorSupport.type_subTypeOf(join, expectedJoin))
        {
            warn(pureModel, arguments.get(2), expression, "Argument 3 of join() is a " + join.getName() + ", but the first argument is a " + kind + ", so a " + expectedJoin.getName() + " is expected.");
        }
    }

    private static Type rawType(ValueSpecification valueSpecification)
    {
        return valueSpecification._genericType() == null ? null : valueSpecification._genericType()._rawType();
    }

    private static void warn(PureModel pureModel, ValueSpecification argument, SimpleFunctionExpression expression, String message)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation located = argument.getSourceInformation();
        SourceInformation sourceInformation = SourceInformationHelper.fromM3SourceInformation((located == null || located.getLine() == 0) ? expression.getSourceInformation() : located);
        pureModel.addDefects(Lists.mutable.with(new Warning(sourceInformation, message + SUFFIX)));
    }
}
