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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_metamodel_TableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;
import java.util.Set;

public interface TableFunctionCompilerExtension
{
    @Deprecated
    default Root_meta_external_query_sql_metamodel_TableFunction translate(TableFunction tablefunction, PureModel pureModel)
    {
        return null;
    }

    //TODO make non default once dependencies updated
    default List<PackageableElement> extractElements(TableFunction tableFunction, PureModel pureModel)
    {
        return Lists.mutable.empty();
    }

    /**
     * Elements whose compiled state this table function reads, so they are compiled before the function holding the
     * #SQL. Same contract as CompilerExtension#getExtraClassInstancePrerequisiteElementsPassProcessorsWithContext: runs
     * in the prerequisite elements pass, may read only first and second pass state, must be thread-safe, and must not
     * throw for references it cannot resolve.
     */
    default Set<PackageableElementPointer> extractPrerequisiteElements(TableFunction tableFunction, CompileContext context)
    {
        return Sets.mutable.empty();
    }
}