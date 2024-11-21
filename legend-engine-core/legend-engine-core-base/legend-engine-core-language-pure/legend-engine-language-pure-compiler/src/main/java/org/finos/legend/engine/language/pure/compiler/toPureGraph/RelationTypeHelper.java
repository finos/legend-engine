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
import org.finos.legend.engine.protocol.pure.v1.model.type.relationType.Column;
import org.finos.legend.engine.protocol.pure.v1.model.type.relationType.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class RelationTypeHelper
{
    public static RelationType convert(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> src)
    {
        RelationType res = new RelationType();
        res.columns = src._columns().collect(c ->
        {
            Column col = new Column();
            col.name = c._name();
            col.type = _Column.getColumnType(c)._rawType().getName();
            return col;
        }).toList();
        return res;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> convert(RelationType src, ProcessorSupport processorSupport, SourceInformation sourceInformation)
    {
        return _RelationType.build(ListIterate.collect(src.columns, c -> (CoreInstance) _Column.getColumnInstance(c.name, false, (GenericType) processorSupport.type_wrapGenericType(_Package.getByUserPath(c.type, processorSupport)), (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), sourceInformation, processorSupport)).toList(), sourceInformation, processorSupport);
    }
}
