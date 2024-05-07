// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.mapper;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapper.RelationalMapperParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

import java.util.function.Consumer;

public class RelationalMapperParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    public RelationalMapperParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(RelationalMapperParserGrammar.DefinitionContext ctx)
    {
        if (ctx.relationalMapper() != null && !ctx.relationalMapper().isEmpty())
        {
            this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
            ctx.relationalMapper().stream().map(this::visitRelationalMapper).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    public RelationalMapper visitRelationalMapper(RelationalMapperParserGrammar.RelationalMapperContext ctx)
    {
        RelationalMapper relationalMapper = new RelationalMapper();
        relationalMapper.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        relationalMapper._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        relationalMapper.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.databaseMapperSection() != null)
        {
            relationalMapper.databaseMappers = ListIterate.collect(ctx.databaseMapperSection().databaseMapper(), dbMapper -> visitDatabaseMapper(dbMapper));
        }
        if (ctx.schemaMapperSection() != null)
        {
            relationalMapper.schemaMappers = ListIterate.collect(ctx.schemaMapperSection().schemaMapper(), schMapper -> visitSchemaMapper(schMapper));
        }
        if (ctx.tableMapperSection() != null)
        {
            relationalMapper.tableMappers = ListIterate.collect(ctx.tableMapperSection().tableMapper(), tblMapper -> visitTableMapper(tblMapper));
        }
        return relationalMapper;
    }

    private DatabaseMapper visitDatabaseMapper(RelationalMapperParserGrammar.DatabaseMapperContext ctx)
    {
        DatabaseMapper databaseMapper = new DatabaseMapper();
        databaseMapper.databaseName = PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true);
        databaseMapper.schemas = ListIterate.collect(ctx.schemaReference(), schemaRef -> visitDatabaseSchema(schemaRef));
        return databaseMapper;
    }

    private SchemaMapper visitSchemaMapper(RelationalMapperParserGrammar.SchemaMapperContext ctx)
    {
        SchemaMapper schemaMapper = new SchemaMapper();
        schemaMapper.from = visitDatabaseSchema(ctx.schemaReference());
        schemaMapper.to = PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true);
        return schemaMapper;
    }

    private SchemaPtr visitDatabaseSchema(RelationalMapperParserGrammar.SchemaReferenceContext ctx)
    {
        SchemaPtr schemaPtr = new SchemaPtr();
        schemaPtr._type = "Schema";
        schemaPtr.schema = ctx.schema() == null ? "default" : PureGrammarParserUtility.fromIdentifier(ctx.schema());
        schemaPtr.database = ctx.database().qualifiedName().getText();
        schemaPtr.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return schemaPtr;
    }

    private TableMapper visitTableMapper(RelationalMapperParserGrammar.TableMapperContext ctx)
    {
        TableMapper tableMapper = new TableMapper();
        tableMapper.from = visitSchemaTable(ctx.tableReference());
        tableMapper.to = PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true);
        return tableMapper;
    }

    private TablePtr visitSchemaTable(RelationalMapperParserGrammar.TableReferenceContext ctx)
    {
        TablePtr tablePtr = new TablePtr();
        tablePtr._type = "Table";
        tablePtr.table = PureGrammarParserUtility.fromIdentifier(ctx.table());
        tablePtr.schema = ctx.schema() == null ? "default" : PureGrammarParserUtility.fromIdentifier(ctx.schema());
        tablePtr.database = ctx.database().qualifiedName().getText();
        tablePtr.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return tablePtr;
    }
}
