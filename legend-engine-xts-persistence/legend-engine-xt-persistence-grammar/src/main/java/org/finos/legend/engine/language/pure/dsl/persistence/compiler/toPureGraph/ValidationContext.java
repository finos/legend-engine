// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;

public class ValidationContext
{
    final Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContextPure;
    final PersistenceContext persistenceContextProtocol;
    final CompileContext compileContext;

    public ValidationContext(Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContextPure,
                             PersistenceContext persistenceContextProtocol,
                             CompileContext compileContext)
    {
        this.persistenceContextPure = persistenceContextPure;
        this.persistenceContextProtocol = persistenceContextProtocol;
        this.compileContext = compileContext;
    }

    public Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContextPure()
    {
        return persistenceContextPure;
    }

    public PersistenceContext persistenceContextProtocol()
    {
        return persistenceContextProtocol;
    }

    public CompileContext compileContext()
    {
        return compileContext;
    }
}
