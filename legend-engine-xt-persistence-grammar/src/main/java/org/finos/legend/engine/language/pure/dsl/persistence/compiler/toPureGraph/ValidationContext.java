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
