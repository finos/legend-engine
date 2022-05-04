// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.LocalMappingPropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.EmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.FilterMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.FilterPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.InlineEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.OtherwiseEmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.ColumnMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Filter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Join;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.MultiGrainFilter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.BigInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Binary;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Char;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Date;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Decimal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Numeric;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Other;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Real;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SemiStructured;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Varbinary;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.DynaFunc;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.ElementWithJoins;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.JoinPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.Literal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.LiteralList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.BindingTransformer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RelationalParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    private static final ImmutableSet<String> JOIN_TYPES = Sets.immutable.with("INNER", "OUTER");

    public RelationalParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this(walkerSourceInformation, null, null);
    }

    public RelationalParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(RelationalParserGrammar.DefinitionContext ctx)
    {
        ctx.database().stream().map(this::visitDatabase).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private Database visitDatabase(RelationalParserGrammar.DatabaseContext ctx)
    {
        Database database = new Database();
        database.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        database._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        database.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        ScopeInfo scopeInfo = ScopeInfo.Builder.newInstance().withDatabase(PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier())).build();
        database.includedStores = ListIterate.collect(ctx.include(), includeContext -> PureGrammarParserUtility.fromQualifiedName(includeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : includeContext.qualifiedName().packagePath().identifier(), includeContext.qualifiedName().identifier()));
        database.schemas = ListIterate.collect(ctx.schema(), schemaCtx -> this.visitSchema(schemaCtx, scopeInfo));
        // NOTE: if tables and views are defined without a schema, create a default schema to hold these
        List<Table> tables = ListIterate.collect(ctx.table(), this::visitTable);
        List<View> views = ListIterate.collect(ctx.view(), viewCtx -> this.visitView(viewCtx, scopeInfo));
        if (!tables.isEmpty() || !views.isEmpty())
        {
            Schema schema = new Schema();
            schema.sourceInformation = database.sourceInformation;
            schema.name = "default";
            schema.tables = tables;
            schema.views = views;
            database.schemas.add(schema);
        }
        database.joins = ListIterate.collect(ctx.join(), joinCtx -> this.visitJoin(joinCtx, scopeInfo));
        MutableList<Filter> filters = ListIterate.collect(ctx.filter(), filterCtx -> this.visitFilter(filterCtx, scopeInfo));
        if (!ctx.multiGrainFilter().isEmpty())
        {
            filters.addAll(ListIterate.collect(ctx.multiGrainFilter(), filterCtx -> this.visitMultiGrainFilter(filterCtx, scopeInfo)));
        }
        database.filters = filters;
        return database;
    }


    // ----------------------------------------------- SCHEMA -----------------------------------------------

    public Schema visitSchema(RelationalParserGrammar.SchemaContext ctx, ScopeInfo scopeInfo)
    {
        Schema schema = new Schema();
        schema.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        schema.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        schema.tables = ListIterate.collect(ctx.table(), this::visitTable);
        schema.views = ListIterate.collect(ctx.view(), viewCtx -> this.visitView(viewCtx, ScopeInfo.Builder.newInstance(scopeInfo).withSchemaToken(ctx.identifier().getStart()).build()));
        return schema;
    }


    // ----------------------------------------------- TABLE -----------------------------------------------

    private Table visitTable(RelationalParserGrammar.TableContext ctx)
    {
        Table table = new Table();
        table.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        table.name = ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().unquotedIdentifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText();
        List<String> primaryKeys = new ArrayList<>();
        table.columns = ListIterate.collect(ctx.columnDefinition(), columnDefinitionContext -> this.visitColumnDefinition(columnDefinitionContext, primaryKeys));
        table.primaryKey = primaryKeys;
        if (ctx.milestoneSpec() != null)
        {
            table.milestoning = ListIterate.collect(ctx.milestoneSpec().milestoning(), this::visitMilestoning);
        }
        return table;
    }

    private Column visitColumnDefinition(RelationalParserGrammar.ColumnDefinitionContext ctx, List<String> primaryKeys)
    {
        Column column = new Column();
        column.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        column.name = ctx.relationalIdentifier().getText();
        boolean nullable = true;
        if (ctx.PRIMARY_KEY() != null)
        {
            nullable = false;
            primaryKeys.add(column.name);
        }
        else if (ctx.NOT_NULL() != null)
        {
            nullable = false;
        }
        column.nullable = nullable;
        String dataType = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        switch (dataType.toUpperCase())
        {
            // String
            case "CHAR":
            {
                if (ctx.INTEGER().size() == 1)
                {
                    Char type = new Char();
                    type.size = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type CHAR requires 1 parameter (size) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "VARCHAR":
            {
                if (ctx.INTEGER().size() == 1)
                {
                    VarChar type = new VarChar();
                    type.size = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type VARCHAR requires 1 parameter (size) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            // Binary
            case "BINARY":
            {
                if (ctx.INTEGER().size() == 1)
                {
                    Binary type = new Binary();
                    type.size = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type BINARY requires 1 parameter (size) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "VARBINARY":
            {
                if (ctx.INTEGER().size() == 1)
                {
                    Varbinary type = new Varbinary();
                    type.size = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type VARBINARY requires 1 parameter (size) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "BIT":
            {
                column.type = new Bit();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type BIT does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            // Integer
            case "INT":
            case "INTEGER":
            {
                column.type = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type INTEGER does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "BIGINT":
            {
                column.type = new BigInt();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type BIGINT does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "SMALLINT":
            {
                column.type = new SmallInt();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type SMALLINT does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "TINYINT":
            {
                column.type = new TinyInt();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type TINYINT does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            // Timestamp
            case "TIMESTAMP":
            {
                column.type = new Timestamp();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type TIMESTAMP does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "DATE":
            {
                column.type = new Date();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type DATE does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            // Numeric
            case "NUMERIC":
            {
                if (ctx.INTEGER().size() == 2)
                {
                    Numeric type = new Numeric();
                    type.precision = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    type.scale = Integer.parseInt(ctx.INTEGER().get(1).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type NUMERIC requires 2 parameters (precision, scale) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "DECIMAL":
            {
                if (ctx.INTEGER().size() == 2)
                {
                    Decimal type = new Decimal();
                    type.precision = Integer.parseInt(ctx.INTEGER().get(0).getText());
                    type.scale = Integer.parseInt(ctx.INTEGER().get(1).getText());
                    column.type = type;
                }
                else
                {
                    throw new EngineException("Column data type DECIMAL requires 2 parameters (precision, scale) in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "FLOAT":
            {
                column.type = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Float();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type FLOAT does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "DOUBLE":
            {
                column.type = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type DOUBLE does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "REAL":
            {
                column.type = new Real();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type REAL does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            // Other
            case "ARRAY":
            {
                column.type = new Other();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type ARRAY does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "OTHER":
            {
                column.type = new Other();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type OTHER does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            case "SEMISTRUCTURED":
            {
                column.type = new SemiStructured();
                if (!ctx.INTEGER().isEmpty())
                {
                    throw new EngineException("Column data type SEMISTRUCTURED does not expect any parameters in declaration", this.walkerSourceInformation.getSourceInformation(ctx.identifier().getStart(), ctx.PAREN_CLOSE() != null ? ctx.PAREN_CLOSE().getSymbol() : ctx.identifier().getStop()), EngineErrorType.PARSER);
                }
                break;
            }
            default:
            {
                throw new EngineException("Unsupported column data type '" + dataType + "'", this.walkerSourceInformation.getSourceInformation(ctx.identifier()), EngineErrorType.PARSER);
            }
        }
        return column;
    }


    // -------------------------------------- MILESTONING --------------------------------------

    private Milestoning visitMilestoning(RelationalParserGrammar.MilestoningContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        MilestoningSpecificationSourceCode code = new MilestoningSpecificationSourceCode(
                ctx.start.getInputStream().getText(Interval.of(ctx.start.getStartIndex(), ctx.stop.getStopIndex())),
                ctx.milestoningType().getText(),
                sourceInformation,
                new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation)
                        .withLineOffset(sourceInformation.startLine - 1)
                        .withColumnOffset(sourceInformation.startColumn)
                        .build()
        );

        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        Milestoning milestoning = IRelationalGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraMilestoningParsers));

        if (milestoning == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return milestoning;
    }

    // ----------------------------------------------- VIEW -----------------------------------------------

    private View visitView(RelationalParserGrammar.ViewContext ctx, ScopeInfo scopeInfo)
    {
        View view = new View();
        view.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        view.name = ctx.relationalIdentifier().getText();
        view.distinct = ctx.DISTINCT_CMD() != null;
        if (ctx.viewFilterMapping() != null)
        {
            view.filter = this.visitViewFilterMapping(ctx.viewFilterMapping());
        }
        if (ctx.viewGroupBy() != null)
        {
            view.groupBy = ListIterate.collect(ctx.viewGroupBy().operation(), opCtx -> this.visitOperation(opCtx, scopeInfo));
        }
        List<String> primaryKeys = FastList.newList();
        view.columnMappings = ListIterate.collect(ctx.viewColumnMapping(), viewColumnMappingCtx -> this.visitViewColumnMapping(viewColumnMappingCtx, primaryKeys, scopeInfo));
        view.primaryKey = primaryKeys;
        // TODO? mainTable: we might not need this while parsing
        return view;
    }

    private FilterMapping visitViewFilterMapping(RelationalParserGrammar.ViewFilterMappingContext ctx)
    {
        FilterMapping filterMapping = new FilterMapping();
        filterMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        FilterPointer filterPointer = new FilterPointer();
        filterPointer.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        filterMapping.filter = filterPointer;
        if(ctx.viewFilterMappingJoin() != null)
        {
            this.visitViewFilterMappingJoin(ctx.viewFilterMappingJoin(), filterMapping, filterPointer);
        }
        else
        {
            filterPointer.db = ctx.databasePointer() == null ? null : this.visitDatabasePointer(ctx.databasePointer());
        }
        return filterMapping;
    }

    private void visitViewFilterMappingJoin(RelationalParserGrammar.ViewFilterMappingJoinContext ctx, FilterMapping filterMapping, FilterPointer filterPointer)
    {
        String database = this.visitDatabasePointer(ctx.databasePointer(0));
        if (ctx.joinSequence() != null) {
            filterMapping.joins = this.visitJoinSequence(ctx.joinSequence(), database, ScopeInfo.Builder.newInstance().withDatabase(database).build());
        }
        filterPointer.db = this.visitDatabasePointer(ctx.databasePointer(1));
    }

    public ColumnMapping visitViewColumnMapping(RelationalParserGrammar.ViewColumnMappingContext ctx, List<String> primaryKeys, ScopeInfo scopeInfo)
    {
        ColumnMapping columnMapping = new ColumnMapping();
        columnMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        columnMapping.name = ctx.identifier(0).getText();
        columnMapping.operation = this.visitOperation(ctx.operation(), scopeInfo);
        if (ctx.PRIMARY_KEY() != null)
        {
            primaryKeys.add(columnMapping.name);
        }
        return columnMapping;
    }


    // ----------------------------------------------- JOIN -----------------------------------------------

    private Join visitJoin(RelationalParserGrammar.JoinContext ctx, ScopeInfo scopeInfo)
    {
        Join join = new Join();
        join.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        join.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        join.operation = visitOperation(ctx.operation(), scopeInfo);
        // TODO? join.target - should we handle it here in the parser?
        return join;
    }


    // ----------------------------------------------- FILTER -----------------------------------------------

    private Filter visitFilter(RelationalParserGrammar.FilterContext ctx, ScopeInfo scopeInfo)
    {
        Filter filter = new Filter();
        filter._type = "filter";
        filter.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        filter.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        filter.operation = visitOperation(ctx.operation(), scopeInfo);
        return filter;
    }


    // ----------------------------------------------- MULTIGRAIN_FILTER -----------------------------------------------

    private MultiGrainFilter visitMultiGrainFilter(RelationalParserGrammar.MultiGrainFilterContext ctx, ScopeInfo scopeInfo)
    {
        MultiGrainFilter filter = new MultiGrainFilter();
        filter._type = "multigrain";
        filter.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        filter.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        filter.operation = visitOperation(ctx.operation(), scopeInfo);
        return filter;
    }


    // ----------------------------------------------- OPERATIONS -----------------------------------------------

    public RelationalOperationElement visitOperation(RelationalParserGrammar.OperationContext ctx, ScopeInfo scopeInfo)
    {
        if (ctx.booleanOperation() != null)
        {
            return this.visitBooleanOperation(ctx.booleanOperation(), scopeInfo);
        }
        else if (ctx.joinOperation() != null)
        {
            return this.visitJoinOperation(ctx.joinOperation(), scopeInfo);
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private RelationalOperationElement visitBooleanOperation(RelationalParserGrammar.BooleanOperationContext ctx, ScopeInfo scopeInfo)
    {
        RelationalOperationElement operationElement = this.visitAtomicOperation(ctx.atomicOperation(), scopeInfo);
        if (ctx.booleanOperationRight() != null)
        {
            return this.visitBooleanOperation(ctx.booleanOperationRight(), operationElement, scopeInfo);
        }
        return operationElement;
    }

    private RelationalOperationElement visitAtomicOperation(RelationalParserGrammar.AtomicOperationContext ctx, ScopeInfo scopeInfo)
    {
        RelationalOperationElement operationElement = null;
        if (ctx.groupOperation() != null)
        {
            operationElement = this.visitGroupOperation(ctx.groupOperation(), scopeInfo);
        }
        else if (ctx.functionOperation() != null)
        {
            String database = ctx.databasePointer() != null ? this.visitDatabasePointer(ctx.databasePointer()) : null;
            operationElement = this.visitFunctionOperation(ctx.functionOperation(), database != null ? ScopeInfo.Builder.newInstance(scopeInfo).withDatabase(database).build() : scopeInfo);
        }
        else if (ctx.constant() != null)
        {
            operationElement = this.visitConstant(ctx.constant());
        }
        else if (ctx.columnOperation() != null)
        {
            operationElement = this.visitColumnOperation(ctx.columnOperation(), scopeInfo);
        }
        else if (ctx.joinOperation() != null)
        {
            return this.visitJoinOperation(ctx.joinOperation(), scopeInfo);
        }
        if (operationElement == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        operationElement.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.atomicOperationRight() != null)
        {
            return this.visitAtomicOperation(ctx.atomicOperationRight(), operationElement, scopeInfo);
        }
        return operationElement;
    }

    private RelationalOperationElement visitGroupOperation(RelationalParserGrammar.GroupOperationContext ctx, ScopeInfo scopeInfo)
    {
        DynaFunc operation = new DynaFunc();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        operation.funcName = "group";
        RelationalOperationElement operationElement = this.visitOperation(ctx.operation(), scopeInfo);
        operation.parameters = new ArrayList<>();
        operation.parameters.add(operationElement);
        return operation;
    }

    private RelationalOperationElement visitBooleanOperation(RelationalParserGrammar.BooleanOperationRightContext ctx, RelationalOperationElement booleanOperationLeft, ScopeInfo scopeInfo)
    {
        DynaFunc operation = new DynaFunc();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        String booleanOperator = ctx.booleanOperator().AND() != null ? "and" : "or";
        String inverseOperator = ctx.booleanOperator().AND() != null ? "or" : "and";
        operation.funcName = booleanOperator;
        RelationalOperationElement booleanOperationRight = this.visitOperation(ctx.operation(), scopeInfo);
        operation.parameters = new ArrayList<>();
        operation.parameters.add(booleanOperationLeft);
        // NOTE: we are trying to be clever here and extract params of right DynaFunction if it's
        // of the same boolean operator type (and/or) to avoid creating nested DynaFunctions of same operation
        if (booleanOperationRight instanceof DynaFunc && ((DynaFunc) booleanOperationRight).funcName.equals(booleanOperator))
        {
            operation.parameters.addAll(((DynaFunc) booleanOperationRight).parameters);
        }
        // if root level operator on right hand side is opposite boolean operation wrap in group to enforce precedence
        // e.g. (bool1 AND bool2 OR bool3) -> (bool1 AND (bool2 OR bool3))
        else if (booleanOperationRight instanceof DynaFunc && ((DynaFunc) booleanOperationRight).funcName.equals(inverseOperator))
        {
            DynaFunc groupOperation = new DynaFunc();
            groupOperation.sourceInformation = booleanOperationRight.sourceInformation;
            groupOperation.funcName = "group";
            groupOperation.parameters = new ArrayList<>();
            groupOperation.parameters.add(booleanOperationRight);
            operation.parameters.add(groupOperation);
        }
        else
        {
            operation.parameters.add(booleanOperationRight);
        }
        return operation;
    }

    private RelationalOperationElement visitAtomicOperation(RelationalParserGrammar.AtomicOperationRightContext ctx, RelationalOperationElement atomicOperationLeft, ScopeInfo scopeInfo)
    {
        DynaFunc operation = new DynaFunc();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        operation.parameters = new ArrayList<>();
        operation.parameters.add(atomicOperationLeft);
        if (ctx.atomicOperator() != null)
        {
            operation.funcName = visitAtomicOperator(ctx.atomicOperator());
            operation.parameters.add(this.visitAtomicOperation(ctx.atomicOperation(), scopeInfo));
            return operation;
        }
        else if (ctx.atomicSelfOperator() != null)
        {
            operation.funcName = this.visitAtomicSelfOperator(ctx.atomicSelfOperator());
            return operation;
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private String visitAtomicSelfOperator(RelationalParserGrammar.AtomicSelfOperatorContext ctx)
    {
        if (ctx.IS_NULL() != null)
        {
            return "isNull";
        }
        else if (ctx.IS_NOT_NULL() != null)
        {
            return "isNotNull";
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private String visitAtomicOperator(RelationalParserGrammar.AtomicOperatorContext ctx)
    {
        if (ctx.EQUAL() != null)
        {
            return "equal";
        }
        else if (ctx.GREATER_THAN() != null)
        {
            return "greaterThan";
        }
        else if (ctx.LESS_THAN() != null)
        {
            return "lessThan";
        }
        else if (ctx.GREATER_OR_EQUAL() != null)
        {
            return "greaterThanEqual";
        }
        else if (ctx.LESS_OR_EQUAL() != null)
        {
            return "lessThanEqual";
        }
        else if (ctx.TEST_NOT_EQUAL() != null)
        {
            return "notEqual";
        }
        else if (ctx.NOT_EQUAL() != null)
        {
            return "notEqualAnsi";
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private RelationalOperationElement visitFunctionOperation(RelationalParserGrammar.FunctionOperationContext ctx, ScopeInfo scopeInfo)
    {
        DynaFunc operation = new DynaFunc();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        operation.funcName = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        operation.parameters = ListIterate.collect(ctx.functionOperationArgument(), argCtx -> this.visitFunctionOperationArgument(argCtx, scopeInfo));
        return operation;
    }

    private RelationalOperationElement visitFunctionOperationArgument(RelationalParserGrammar.FunctionOperationArgumentContext ctx, ScopeInfo scopeInfo)
    {
        if (ctx.operation() != null)
        {
            return this.visitOperation(ctx.operation(), scopeInfo);
        }
        LiteralList literalList = new LiteralList();
        literalList.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        literalList.values = ListIterate.collect(ctx.functionOperationArgumentArray().functionOperationArgument(), argCtx ->
        {
            Literal literal = new Literal();
            literal.value = this.visitFunctionOperationArgument(argCtx, scopeInfo);
            return literal;
        });
        return literalList;
    }

    private RelationalOperationElement visitConstant(RelationalParserGrammar.ConstantContext ctx)
    {
        Literal constant = new Literal();
        constant.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.STRING() != null)
        {
            constant.value = PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true);
            return constant;
        }
        else if (ctx.INTEGER() != null)
        {
            constant.value = Integer.parseInt(ctx.INTEGER().getText());
            return constant;
        }
        else if (ctx.FLOAT() != null)
        {
            constant.value = Double.parseDouble(ctx.FLOAT().getText());
            return constant;
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private RelationalOperationElement visitColumnOperation(RelationalParserGrammar.ColumnOperationContext ctx, ScopeInfo scopeInfo)
    {
        String database = ctx.databasePointer() != null ? this.visitDatabasePointer(ctx.databasePointer()) : (scopeInfo != null ? scopeInfo.database : null);
        return this.visitTableAliasColumnOperation(ctx.tableAliasColumnOperation(), database, scopeInfo);
    }

    private String visitDatabasePointer(RelationalParserGrammar.DatabasePointerContext ctx)
    {
        return ctx != null ? PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier()) : null;
    }

    private RelationalOperationElement visitTableAliasColumnOperation(RelationalParserGrammar.TableAliasColumnOperationContext ctx, String database, ScopeInfo scopeInfo)
    {
        if (ctx.tableAliasColumnOperationWithTarget() != null)
        {
            return this.visitTableAliasColumnOperationWithTarget(ctx.tableAliasColumnOperationWithTarget(), database);
        }
        else if (ctx.tableAliasColumnOperationWithScopeInfo() != null)
        {
            return this.visitTableAliasColumnOperationWithScopeInfo(ctx.tableAliasColumnOperationWithScopeInfo(), database, scopeInfo);
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private RelationalOperationElement visitTableAliasColumnOperationWithTarget(RelationalParserGrammar.TableAliasColumnOperationWithTargetContext ctx, String database)
    {
        TableAliasColumn operation = new TableAliasColumn();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        operation.column = ctx.relationalIdentifier().QUOTED_STRING() == null ? ctx.relationalIdentifier().unquotedIdentifier().getText() : ctx.relationalIdentifier().QUOTED_STRING().getText();
        TablePtr tablePtr = this.generateTableAlias(ScopeInfo.Builder.newInstance()
                .withDatabase(database)
                .withTableAliasToken(ctx.TARGET().getSymbol())
                .withColumnToken(ctx.relationalIdentifier().getStart())
                .build(), operation.sourceInformation
        );
        operation.table = tablePtr;
        operation.tableAlias = tablePtr.table;
        return operation;
    }

    private RelationalOperationElement visitTableAliasColumnOperationWithScopeInfo(RelationalParserGrammar.TableAliasColumnOperationWithScopeInfoContext ctx, String database, ScopeInfo scopeInfo)
    {
        Token first = ctx.relationalIdentifier().getStart();
        RelationalParserGrammar.ScopeInfoContext scopeInfoContext = ctx.scopeInfo();
        if (scopeInfoContext != null)
        {
            Token second = scopeInfoContext.relationalIdentifier(0).getStart();
            Token third = scopeInfoContext.relationalIdentifier().size() > 1 ? scopeInfoContext.relationalIdentifier(1).getStart() : null;
            Token schema = null;
            Token alias = null;
            Token column;
            SourceInformation columnSourceInformation;
            if (second == null && third == null)
            {
                column = first;
                columnSourceInformation = this.walkerSourceInformation.getSourceInformation(first);
            }
            else if (third == null)
            {
                alias = first;
                column = second;
                columnSourceInformation = this.walkerSourceInformation.getSourceInformation(second);
            }
            else
            {
                schema = first;
                alias = second;
                column = third;
                columnSourceInformation = this.walkerSourceInformation.getSourceInformation(third);
            }
            return this.generateTableAliasColumn(ScopeInfo.Builder.newInstance(scopeInfo)
                            .withNonEmptyDatabase(database)
                            .withNonNullableSchemaToken(schema)
                            .withNonNullableTableAliasToken(alias)
                            .withColumnToken(column)
                            .build(),
                    columnSourceInformation);
        }
        return this.generateTableAliasColumn(ScopeInfo.Builder.newInstance(scopeInfo)
                .withNonEmptyDatabase(database)
                .withColumnToken(first)
                .build(), this.walkerSourceInformation.getSourceInformation(first));
    }

    private TableAliasColumn generateTableAliasColumn(ScopeInfo info, SourceInformation sourceInformation)
    {
        TableAliasColumn operation = new TableAliasColumn();
        operation.sourceInformation = sourceInformation;
        operation.column = info.columnToken != null ? info.columnToken.getText() : null;
        TablePtr tablePtr = this.generateTableAlias(info, sourceInformation);
        operation.table = tablePtr;
        operation.tableAlias = tablePtr.table;
        return operation;
    }

    private TablePtr generateTableAlias(ScopeInfo scopeInfo, SourceInformation sourceInformation)
    {
        Token schema = scopeInfo.schemaToken;
        Token alias = scopeInfo.tableAliasToken;
        if (alias == null)
        {
            if (schema == null)
            {
                throw new EngineException("Missing table or alias" + (scopeInfo.columnToken == null ? "" : (" for column '" + scopeInfo.columnToken.getText() + "'")), sourceInformation, EngineErrorType.PARSER);
            }
            alias = schema;
            schema = null;
        }
        TablePtr tablePtr = new TablePtr();
        tablePtr._type = "Table";
        String database = scopeInfo.database;
        tablePtr.database = database;
        tablePtr.mainTableDb = database;
        // NOTE: this is a minor inference for the schema name, we might consider moving this to the compiler instead
        tablePtr.schema = schema != null ? schema.getText() : "default";
        tablePtr.table = alias.getText();
        tablePtr.sourceInformation = schema == null ? this.walkerSourceInformation.getSourceInformation(alias) : this.walkerSourceInformation.getSourceInformation(schema, alias);
        return tablePtr;
    }

    private RelationalOperationElement visitJoinOperation(RelationalParserGrammar.JoinOperationContext ctx, ScopeInfo scopeInfo)
    {
        String database = ctx.databasePointer() != null ? this.visitDatabasePointer(ctx.databasePointer()) : (scopeInfo != null ? scopeInfo.database : null);
        ElementWithJoins operation = new ElementWithJoins();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.booleanOperation() != null)
        {
            operation.relationalElement = this.visitBooleanOperation(ctx.booleanOperation(), ScopeInfo.Builder.newInstance(scopeInfo).withDatabase(database).build());
        }
        else if (ctx.tableAliasColumnOperation() != null)
        {
            operation.relationalElement = this.visitTableAliasColumnOperation(ctx.tableAliasColumnOperation(), database, scopeInfo);
        }
        operation.joins = this.visitJoinSequence(ctx.joinSequence(), database, scopeInfo);
        return operation;
    }

    // NOTE: right now, for join we only support sequence (line), not tree
    private List<JoinPointer> visitJoinSequence(RelationalParserGrammar.JoinSequenceContext ctx, String database, ScopeInfo scopeInfo)
    {
        ParserRuleContext joinType = ctx.identifier() != null ? ctx.identifier() : null;
        if (joinType != null && !JOIN_TYPES.contains(PureGrammarParserUtility.fromIdentifier(joinType)))
        {
            throw new EngineException("Unsupported join type '" + joinType.getText() + "'. The supported join types are: " + JOIN_TYPES.toString(), this.walkerSourceInformation.getSourceInformation(ctx.identifier()), EngineErrorType.PARSER);
        }
        List<JoinPointer> joins = new ArrayList<>();
        joins.add(visitJoinPointer(ctx.joinPointer(), joinType == null ? null : joinType.getText(), scopeInfo, database));
        for (RelationalParserGrammar.JoinPointerFullContext joinPointerFullContext : ctx.joinPointerFull())
        {
            if (joinPointerFullContext.identifier() != null && !JOIN_TYPES.contains(PureGrammarParserUtility.fromIdentifier(joinPointerFullContext.identifier())))
            {
                throw new EngineException("Unsupported join type '" + joinPointerFullContext.identifier().getText() + "'", this.walkerSourceInformation.getSourceInformation(joinPointerFullContext.identifier()), EngineErrorType.PARSER);
            }
            JoinPointer joinPointer = visitJoinPointer(joinPointerFullContext.joinPointer(),
                    joinPointerFullContext.identifier() != null ? PureGrammarParserUtility.fromIdentifier(joinPointerFullContext.identifier()) : null,
                    scopeInfo,
                    joinPointerFullContext.databasePointer() != null ? visitDatabasePointer(joinPointerFullContext.databasePointer()) : database
            );
            joins.add(joinPointer);
        }
        return joins;
    }

    private JoinPointer visitJoinPointer(RelationalParserGrammar.JoinPointerContext ctx, String joinType, ScopeInfo scopeInfo, String database)
    {
        JoinPointer joinPointer = new JoinPointer();
        joinPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        joinPointer.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        joinPointer.joinType = joinType;
        joinPointer.db = database == null || "".equals(database)
                ? scopeInfo == null || scopeInfo.database == null || "".equals(scopeInfo.database) ? null : scopeInfo.database
                : database;
        return joinPointer;
    }


    // -------------------------------------- ASSOCIATION MAPPING --------------------------------------

    public void visitRelationalAssociationMapping(RelationalParserGrammar.AssociationMappingContext ctx, RelationalAssociationMapping relationalAssociationMapping)
    {
        relationalAssociationMapping.propertyMappings = ctx.propertyMapping().stream()
                .flatMap(propertyMappingContext -> this.visitPropertyMapping(propertyMappingContext, null, null).stream())
                .collect(Collectors.toList());
    }


    // -------------------------------------- CLASS MAPPING --------------------------------------

    public void visitRootRelationalClassMapping(RelationalParserGrammar.ClassMappingContext ctx, RootRelationalClassMapping rootRelationalClassMapping, String _class)
    {
        rootRelationalClassMapping.distinct = ctx.DISTINCT_CMD() != null;
        String database = null;
        if (ctx.mappingFilter() != null)
        {
            rootRelationalClassMapping.filter = this.visitMappingFilter(ctx.mappingFilter());
            database = rootRelationalClassMapping.filter.filter.db;
        }
        if (ctx.mappingMainTable() != null)
        {
            ScopeInfo scopeInfo = this.visitMappingScopeInfo(ctx.mappingMainTable().mappingScopeInfo(), visitDatabasePointer(ctx.mappingMainTable().databasePointer()));
            rootRelationalClassMapping.mainTable = this.generateTableAlias(scopeInfo, this.walkerSourceInformation.getSourceInformation(ctx.mappingMainTable().mappingScopeInfo()));
        }
        if (ctx.mappingPrimaryKey() != null)
        {
            String _database = database;
            rootRelationalClassMapping.primaryKey = ListIterate.collect(ctx.mappingPrimaryKey().operation(), opCtx -> this.visitOperation(opCtx, ScopeInfo.Builder.newInstance().withDatabase(_database).build()));
        }
        if (ctx.mappingGroupBy() != null)
        {
            String _database = database;
            rootRelationalClassMapping.groupBy = ListIterate.collect(ctx.mappingGroupBy().operation(), opCtx -> this.visitOperation(opCtx, ScopeInfo.Builder.newInstance().withDatabase(_database).build()));
        }
        if (ctx.propertyMapping() != null)
        {
            rootRelationalClassMapping.propertyMappings = ctx.propertyMapping().stream()
                    .flatMap(propertyMappingContext -> this.visitPropertyMapping(propertyMappingContext, rootRelationalClassMapping.id, _class).stream())
                    .collect(Collectors.toList());
        }
    }

    private FilterMapping visitMappingFilter(RelationalParserGrammar.MappingFilterContext ctx)
    {
        FilterMapping filterMapping = new FilterMapping();
        filterMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        FilterPointer filterPointer = new FilterPointer();
        filterMapping.filter = filterPointer;
        filterPointer.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        String database = visitDatabasePointer(ctx.databasePointer(0));
        if (ctx.joinSequence() != null)
        {
            filterMapping.joins = this.visitJoinSequence(ctx.joinSequence(), database, ScopeInfo.Builder.newInstance().withDatabase(database).build());
            database = visitDatabasePointer(ctx.databasePointer(1));
        }
        filterPointer.db = database;
        return filterMapping;
    }

    private ScopeInfo visitMappingScopeInfo(RelationalParserGrammar.MappingScopeInfoContext ctx, String database)
    {
        Token first = ctx.relationalIdentifier().getStart();
        RelationalParserGrammar.ScopeInfoContext scopeInfoContext = ctx.scopeInfo();
        if (scopeInfoContext != null)
        {
            Token second = scopeInfoContext.relationalIdentifier(0).getStart();
            Token third = scopeInfoContext.relationalIdentifier().size() > 1 ? scopeInfoContext.relationalIdentifier(1).getStart() : null;
            Token schema;
            Token alias = null;
            Token column = null;
            if (second == null && third == null)
            {
                schema = first;
            }
            else if (third == null)
            {
                schema = first;
                alias = second;
            }
            else
            {
                schema = first;
                alias = second;
                column = third;
            }
            return ScopeInfo.Builder.newInstance().withDatabase(database).withSchemaToken(schema).withTableAliasToken(alias).withColumnToken(column).build();
        }
        return ScopeInfo.Builder.newInstance().withDatabase(database).withSchemaToken(first).build();
    }


    // -------------------------------------- PROPERTY MAPPING --------------------------------------

    private List<PropertyMapping> visitPropertyMapping(RelationalParserGrammar.PropertyMappingContext ctx, String classMappingId, String _class)
    {
        if (ctx.propertyMappingWithScope() != null)
        {
            return this.visitPropertyMappingWithScope(ctx.propertyMappingWithScope(), classMappingId, _class);
        }
        else if (ctx.singlePropertyMapping() != null)
        {
            return FastList.newListWith(this.visitSinglePropertyMapping(ctx.singlePropertyMapping(), null, classMappingId, _class));
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private List<PropertyMapping> visitPropertyMappingWithScope(RelationalParserGrammar.PropertyMappingWithScopeContext ctx, String classMappingId, String _class)
    {
        ScopeInfo _scopeInfo = null;
        String database = visitDatabasePointer(ctx.databasePointer());
        if (ctx.mappingScopeInfo() != null)
        {
            _scopeInfo = this.visitMappingScopeInfo(ctx.mappingScopeInfo(), database);
        }
        _scopeInfo = _scopeInfo == null ?
                ScopeInfo.Builder.newInstance().withDatabase(database).build()
                : ScopeInfo.Builder.newInstance(_scopeInfo).withDatabase(database).build();
        ScopeInfo scopeInfo = _scopeInfo;
        return ListIterate.collect(ctx.singlePropertyMapping(), singlePropertyMappingContext -> this.visitSinglePropertyMapping(singlePropertyMappingContext, scopeInfo, classMappingId, _class));
    }

    private PropertyMapping visitSinglePropertyMapping(RelationalParserGrammar.SinglePropertyMappingContext ctx, ScopeInfo scopeInfo, String classMappingId, String _class)
    {
        if (ctx.singlePropertyMappingWithPlus() != null)
        {
            return visitPropertyMappingWithLocalMappingProperty(ctx.singlePropertyMappingWithPlus(), scopeInfo, classMappingId);
        }
        else if (ctx.singlePropertyMappingWithoutPlus() != null)
        {
            RelationalParserGrammar.SinglePropertyMappingWithoutPlusContext propertyMappingCtx = ctx.singlePropertyMappingWithoutPlus();
            PropertyPointer propertyPointer = new PropertyPointer();
            propertyPointer.property = PureGrammarParserUtility.fromIdentifier(propertyMappingCtx.identifier());
            propertyPointer._class = _class;
            propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx.identifier());
            String sourceId = null;
            String targetId = null;
            if (propertyMappingCtx.sourceAndTargetMappingId() != null)
            {
                sourceId = propertyMappingCtx.sourceAndTargetMappingId().targetId() == null ? null : propertyMappingCtx.sourceAndTargetMappingId().sourceId().getText();
                targetId = propertyMappingCtx.sourceAndTargetMappingId().targetId() == null ? propertyMappingCtx.sourceAndTargetMappingId().sourceId().getText() : propertyMappingCtx.sourceAndTargetMappingId().targetId().getText();
            }
            if (propertyMappingCtx.relationalPropertyMapping() != null)
            {
                return this.visitRelationalPropertyMapping(propertyMappingCtx.relationalPropertyMapping(), propertyPointer, null, scopeInfo, sourceId != null ? sourceId : classMappingId, targetId);
            }
            else if (propertyMappingCtx.embeddedPropertyMapping() != null)
            {
                return this.visitEmbeddedPropertyMapping(propertyMappingCtx.embeddedPropertyMapping(), propertyPointer, scopeInfo, classMappingId, targetId);
            }
            else if (propertyMappingCtx.inlineEmbeddedPropertyMapping() != null)
            {
                return this.visitInlineEmbeddedPropertyMapping(propertyMappingCtx.inlineEmbeddedPropertyMapping(), propertyPointer, targetId);
            }
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private PropertyMapping visitPropertyMappingWithLocalMappingProperty(RelationalParserGrammar.SinglePropertyMappingWithPlusContext ctx, ScopeInfo scopeInfo, String classMappingId)
    {
        RelationalParserGrammar.LocalMappingPropertyContext localMappingPropertyContext = ctx.localMappingProperty();
        LocalMappingPropertyInfo localMappingPropertyInfo = new LocalMappingPropertyInfo();
        localMappingPropertyInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(localMappingPropertyContext);
        localMappingPropertyInfo.type = PureGrammarParserUtility.fromQualifiedName(localMappingPropertyContext.qualifiedName().packagePath() == null ? Collections.emptyList() : localMappingPropertyContext.qualifiedName().packagePath().identifier(), localMappingPropertyContext.qualifiedName().identifier());
        Multiplicity multiplicity = new Multiplicity();
        localMappingPropertyInfo.multiplicity = multiplicity;
        RelationalParserGrammar.LocalMappingPropertyFromMultiplicityContext fromMultiplicityContext = localMappingPropertyContext.localMappingPropertyFromMultiplicity();
        RelationalParserGrammar.LocalMappingPropertyToMultiplicityContext toMultiplicityContext = localMappingPropertyContext.localMappingPropertyToMultiplicity();
        multiplicity.lowerBound = fromMultiplicityContext == null
                ? Integer.parseInt("*".equals(toMultiplicityContext.getText()) ? "0" : toMultiplicityContext.getText())
                : Integer.parseInt(fromMultiplicityContext.getText());
        multiplicity.setUpperBound("*".equals(toMultiplicityContext.getText()) ? null : Integer.parseInt(toMultiplicityContext.getText()));
        PropertyPointer propertyPointer = new PropertyPointer();
        propertyPointer.property = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        propertyPointer._class = null;
        propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.identifier());
        return visitRelationalPropertyMapping(ctx.relationalPropertyMapping(), propertyPointer, localMappingPropertyInfo, scopeInfo, classMappingId, null);
    }

    private PropertyMapping visitRelationalPropertyMapping(RelationalParserGrammar.RelationalPropertyMappingContext ctx, PropertyPointer propertyPointer, LocalMappingPropertyInfo localMappingPropertyInfo, ScopeInfo scopeInfo, String classMappingId, String targetId)
    {
        RelationalPropertyMapping relationalPropertyMapping = new RelationalPropertyMapping();
        relationalPropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        relationalPropertyMapping.localMappingProperty = localMappingPropertyInfo;
        relationalPropertyMapping.property = propertyPointer;
        relationalPropertyMapping.source = classMappingId;
        relationalPropertyMapping.target = targetId;
        relationalPropertyMapping.enumMappingId = ctx.transformer() != null && ctx.transformer().enumTransformer() != null ? PureGrammarParserUtility.fromIdentifier(ctx.transformer().enumTransformer().identifier()) : null;
        relationalPropertyMapping.bindingTransformer = ctx.transformer() != null && ctx.transformer().bindingTransformer() != null ? buildBindingTransformer(ctx.transformer().bindingTransformer()) : null;
        relationalPropertyMapping.relationalOperation = this.visitOperation(ctx.operation(), scopeInfo);
        return relationalPropertyMapping;
    }

    private BindingTransformer buildBindingTransformer(RelationalParserGrammar.BindingTransformerContext ctx)
    {
        String binding = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        BindingTransformer bindingTransformer = new BindingTransformer();
        bindingTransformer.binding = binding;
        return bindingTransformer;
    }

    // -------------------------------------- EMBEDDED PROPERTY MAPPING --------------------------------------

    private PropertyMapping visitEmbeddedPropertyMapping(RelationalParserGrammar.EmbeddedPropertyMappingContext ctx, PropertyPointer propertyPointer, ScopeInfo scopeInfo, String classMappingId, String targetId)
    {
        RelationalClassMapping relationalClassMapping = new RelationalClassMapping();
        relationalClassMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        relationalClassMapping.id = targetId;
        relationalClassMapping.root = false;
        relationalClassMapping.primaryKey = ctx.mappingPrimaryKey() != null
                ? ListIterate.collect(ctx.mappingPrimaryKey().operation(), opCtx -> this.visitOperation(opCtx, ScopeInfo.Builder.newInstance().build()))
                : new ArrayList<>();
        relationalClassMapping.propertyMappings = ListIterate.collect(ctx.singlePropertyMapping(), singlePropertyMappingContext -> this.visitSinglePropertyMapping(singlePropertyMappingContext, scopeInfo, classMappingId, null));
        if (ctx.otherwiseEmbeddedPropertyMapping() != null)
        {
            OtherwiseEmbeddedRelationalPropertyMapping otherwiseEmbeddedRelationalPropertyMapping = new OtherwiseEmbeddedRelationalPropertyMapping();
            otherwiseEmbeddedRelationalPropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.otherwiseEmbeddedPropertyMapping());
            otherwiseEmbeddedRelationalPropertyMapping.id = targetId;
            otherwiseEmbeddedRelationalPropertyMapping.target = targetId;
            otherwiseEmbeddedRelationalPropertyMapping.property = propertyPointer;
            otherwiseEmbeddedRelationalPropertyMapping.classMapping = relationalClassMapping;
            otherwiseEmbeddedRelationalPropertyMapping.otherwisePropertyMapping = this.visitOtherwisePropertyMapping(ctx.otherwiseEmbeddedPropertyMapping().otherwisePropertyMapping(), propertyPointer, scopeInfo);
            return otherwiseEmbeddedRelationalPropertyMapping;
        }
        EmbeddedRelationalPropertyMapping embeddedRelationalPropertyMapping = new EmbeddedRelationalPropertyMapping();
        embeddedRelationalPropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        embeddedRelationalPropertyMapping.id = targetId;
        embeddedRelationalPropertyMapping.target = targetId;
        embeddedRelationalPropertyMapping.property = propertyPointer;
        embeddedRelationalPropertyMapping.classMapping = relationalClassMapping;
        return embeddedRelationalPropertyMapping;
    }

    private RelationalPropertyMapping visitOtherwisePropertyMapping(RelationalParserGrammar.OtherwisePropertyMappingContext ctx, PropertyPointer propertyPointer, ScopeInfo scopeInfo)
    {
        RelationalPropertyMapping relationalPropertyMapping = new RelationalPropertyMapping();
        relationalPropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        relationalPropertyMapping.property = propertyPointer;
        relationalPropertyMapping.target = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        String database = ctx.databasePointer() != null ? this.visitDatabasePointer(ctx.databasePointer()) : null;
        ElementWithJoins operation = new ElementWithJoins();
        operation.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        operation.joins = this.visitJoinSequence(ctx.joinSequence(), database, scopeInfo);
        relationalPropertyMapping.relationalOperation = operation;
        return relationalPropertyMapping;
    }

    private PropertyMapping visitInlineEmbeddedPropertyMapping(RelationalParserGrammar.InlineEmbeddedPropertyMappingContext ctx, PropertyPointer propertyPointer, String targetId)
    {
        InlineEmbeddedPropertyMapping inlineEmbeddedPropertyMapping = new InlineEmbeddedPropertyMapping();
        inlineEmbeddedPropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        inlineEmbeddedPropertyMapping.id = targetId;
        inlineEmbeddedPropertyMapping.target = targetId;
        inlineEmbeddedPropertyMapping.property = propertyPointer;
        inlineEmbeddedPropertyMapping.setImplementationId = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        return inlineEmbeddedPropertyMapping;
    }


    // -------------------------------------- SCOPE INFO --------------------------------------

    /**
     * NOTE: ScopeInfo is a way to handle scope while parsing relational mapping
     * This is an inference, we could have done this in the compiler
     * instead of the parser as now this makes the parsing logic a lot more complicated than it should be
     * The reason why we cannot do this in the compiler now is that there is no part of he protocol model designated
     * to store this scope information. As such in the protocol -> grammar transformation, we will not be able to
     * generate scope; i.e. grammar to protocol is not bijective in this case.
     */
    static class ScopeInfo
    {
        protected final String database;
        protected final Token schemaToken;
        protected final Token tableAliasToken;
        protected final Token columnToken;

        private ScopeInfo(Builder builder)
        {
            this.database = builder.database;
            this.schemaToken = builder.schemaToken;
            this.tableAliasToken = builder.tableAliasToken;
            this.columnToken = builder.columnToken;
        }

        static class Builder
        {
            private String database;
            // NOTE: we leave these as Token instead of string so we can manage source information better
            private Token schemaToken;
            private Token tableAliasToken;
            private Token columnToken;

            private Builder()
            {
                // hide constructor
            }

            public static Builder newInstance(ScopeInfo scopeInfo)
            {
                Builder builder = new Builder();
                builder.database = scopeInfo != null ? scopeInfo.database : null;
                builder.schemaToken = scopeInfo != null ? scopeInfo.schemaToken : null;
                builder.tableAliasToken = scopeInfo != null ? scopeInfo.tableAliasToken : null;
                builder.columnToken = scopeInfo != null ? scopeInfo.columnToken : null;
                return builder;
            }

            public static Builder newInstance()
            {
                return new Builder();
            }

            public Builder withDatabase(String database)
            {
                this.database = database;
                return this;
            }

            public Builder withNonEmptyDatabase(String database)
            {
                this.database = database == null || database.isEmpty() ? this.database : database;
                return this;
            }

            public Builder withSchemaToken(Token schemaToken)
            {
                this.schemaToken = schemaToken;
                return this;
            }

            public Builder withNonNullableSchemaToken(Token schemaToken)
            {
                this.schemaToken = schemaToken == null ? this.schemaToken : schemaToken;
                return this;
            }

            public Builder withTableAliasToken(Token tableAliasToken)
            {
                this.tableAliasToken = tableAliasToken;
                return this;
            }

            public Builder withNonNullableTableAliasToken(Token tableAliasToken)
            {
                this.tableAliasToken = tableAliasToken == null ? this.tableAliasToken : tableAliasToken;
                return this;
            }

            public Builder withColumnToken(Token columnToken)
            {
                this.columnToken = columnToken;
                return this;
            }

            public ScopeInfo build()
            {
                return new ScopeInfo(this);
            }
        }
    }
}