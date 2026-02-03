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

package org.finos.legend.engine.sql.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

public class TableFunctionSimpleElementExtractor<T extends PackageableElement> implements TableFunctionCompilerExtension
{
    private Class<T> type;
    private String typeName;

    public TableFunctionSimpleElementExtractor(Class<T> type, String typeName)
    {
        this.type = type;
        this.typeName = typeName;
    }

    protected String extractElementPath(TableFunction tableFunction)
    {
        return ((StringLiteral) tableFunction.functionCall.arguments.get(0)).value;
    }

    @Override
    public List<PackageableElement> extractElements(TableFunction tableFunction, PureModel pureModel)
    {
        if (typeName.equals(tableFunction.functionCall.name.parts.get(0)))
        {
            String param = extractElementPath(tableFunction);
            String[] val = param.split("\\.");
            PackageableElement element = pureModel.getPackageableElement(val[0]);

            if (!type.isInstance(element))
            {
                throw new EngineException("The element for type " + typeName + " should be of type " + type.getName() + ", instead got " + element.getClass().getName(), null, EngineErrorType.COMPILATION);
            }

            return Lists.mutable.with(element);
        }

        return Lists.mutable.empty();
    }
}
