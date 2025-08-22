// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.sql.compiler;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.sql.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_expression_AccessorTableFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_TableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

public class ModifiedTranslator extends ProtocolToMetamodelTranslator
{
    @Override
    public Root_meta_external_query_sql_metamodel_TableFunction translate(TableFunction tablefunction, PureModel pureModel)
    {
        if ("tb".equals(tablefunction.functionCall.name.parts.get(0)))
        {
            String param = ((StringLiteral) tablefunction.functionCall.arguments.get(0)).value;
            String[] val = param.split("\\.");
            Store store = (Store) pureModel.getPackageableElement(val[0]);
            return new Root_meta_external_query_sql_expression_AccessorTableFunction_Impl("", null, pureModel.getType("meta::external::query::sql::expression::AccessorTableFunction"))
                    ._functionCall(this.translate(tablefunction.functionCall, pureModel))
                    ._store(store);
        }
        return super.translate(tablefunction, pureModel);
    }
}
