// Copyright 2023 Goldman Sachs
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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.relation.Column;
import org.finos.legend.engine.protocol.pure.m3.relation.RelationType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;

public class RelationTypeHelper
{
    public static RelationType convert(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> src)
    {
        RelationType res = new RelationType();
        res.columns = src._columns().collect(c ->
        {
            Column col = new Column();
            col.name = c._name();
            col.genericType = CompileContext.convertGenericType(_Column.getColumnType(c));
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity m = _Column.getColumnMultiplicity(c);
            col.multiplicity = new Multiplicity(m._lowerBound()._value().intValue(), m._upperBound()._value() == null ? null : m._upperBound()._value().intValue());
            return col;
        }).toList();
        return res;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> convert(RelationType src, CompileContext ctx)
    {
        ProcessorSupport processorSupport = ctx.pureModel.getExecutionSupport().getProcessorSupport();
        return _RelationType.build(
                ListIterate.collect(src.columns, c ->
                        _Column.getColumnInstance(
                                c.name,
                                false,
                                ctx.newGenericType(c.genericType),
                                ctx.pureModel.getMultiplicity(c.multiplicity),
                                SourceInformationHelper.toM3SourceInformation(c.sourceInformation),
                                processorSupport
                        )
                ),
                SourceInformationHelper.toM3SourceInformation(src.sourceInformation),
                processorSupport
        );
    }
}
