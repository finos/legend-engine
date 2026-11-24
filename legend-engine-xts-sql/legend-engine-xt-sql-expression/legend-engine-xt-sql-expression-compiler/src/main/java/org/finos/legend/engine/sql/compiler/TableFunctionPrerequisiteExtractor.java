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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;

import java.util.Set;

class TableFunctionPrerequisiteExtractor extends TableFunctionCollector<PackageableElementPointer>
{
    private final CompileContext context;

    public TableFunctionPrerequisiteExtractor(CompileContext context)
    {
        this.context = context;
    }

    @Override
    protected Set<? extends PackageableElementPointer> collectFrom(TableFunction tableFunction)
    {
        return TableFunctionCompilerExtensionLoader.extensions().flatCollect(x -> x.extractPrerequisiteElements(tableFunction, context)).toSet();
    }
}
