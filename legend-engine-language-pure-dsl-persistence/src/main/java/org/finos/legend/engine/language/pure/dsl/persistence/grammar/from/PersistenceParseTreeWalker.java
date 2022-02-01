package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.ServicePersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.function.Consumer;

public class PersistenceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public PersistenceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(PersistenceParserGrammar.DefinitionContext ctx)
    {
        ctx.servicePersistence().stream().map(this::visitServicePersistence).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private ServicePersistence visitServicePersistence(PersistenceParserGrammar.ServicePersistenceContext ctx)
    {
        ServicePersistence servicePersistence = new ServicePersistence();
        return servicePersistence;
    }
}
