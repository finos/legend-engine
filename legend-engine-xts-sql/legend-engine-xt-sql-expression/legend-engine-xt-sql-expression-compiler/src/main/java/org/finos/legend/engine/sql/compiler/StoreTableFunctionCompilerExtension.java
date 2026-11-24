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

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Set;

public class StoreTableFunctionCompilerExtension extends TableFunctionSimpleElementExtractor<Store>
{
    public StoreTableFunctionCompilerExtension()
    {
        super(Store.class, "tb");
    }

    @Override
    public Set<PackageableElementPointer> extractPrerequisiteElements(TableFunction tableFunction, CompileContext context)
    {
        Store store = findPrerequisiteElement(tableFunction, context);
        return store == null ? Sets.mutable.empty() : Sets.mutable.with(new PackageableElementPointer(PackageableElementType.STORE, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(store)));
    }
}
