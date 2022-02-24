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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.Mapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.SchemaNameMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.TableNameMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.EmbeddedH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.EmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.FilterMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.InlineEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.OtherwiseEmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.ColumnMapping;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Varbinary;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.DynaFunc;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.ElementWithJoins;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.JoinPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.Literal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.LiteralList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperRelationalGrammarComposer
{
    private static final String SELF_JOIN_TABLE_NAME = "{target}";

    public static String renderRelationalOperationElement(RelationalOperationElement op, RelationalGrammarComposerContext context)
    {
        if (op instanceof DynaFunc)
        {
            return renderDynaFunc((DynaFunc)op, context);
        }
        else if (op instanceof TableAliasColumn)
        {
            return renderTableAliasColumn((TableAliasColumn)op, context);
        }
        else if (op instanceof ElementWithJoins)
        {
            return renderElementWithJoins((ElementWithJoins)op, context);
        }
        else if (op instanceof LiteralList)
        {
            return renderLiteralList((LiteralList)op, context);
        }
        else if (op instanceof Literal)
        {
            return renderLiteral((Literal)op, context);
        }
        return PureGrammarComposerUtility.unsupported(op.getClass(), "relational operation element type");
    }

    private static String renderDynaFunc(DynaFunc dynaFunc, RelationalGrammarComposerContext context)
    {
        switch (dynaFunc.funcName)
        {
            case "group":
            {
                if (dynaFunc.parameters.size() != 1)
                {
                    return "/* Unable to transform operation: exactly 1 parameter is expected for '(group)' operation */";
                }
                return "(" + renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + ")";
            }
            case "or":
            case "and":
            {
                return LazyIterate.collect(dynaFunc.parameters, param -> renderRelationalOperationElement(param, context)).makeString(" " + PureGrammarComposerUtility.convertIdentifier(dynaFunc.funcName) + " ");
            }
            case "isNull":
            {
                if (dynaFunc.parameters.size() != 1)
                {
                    return "/* Unable to transform operation: exactly 1 parameter is expected for 'is null' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " is null";
            }
            case "isNotNull":
            {
                if (dynaFunc.parameters.size() != 1)
                {
                    return "/* Unable to transform operation: exactly 1 parameter is expected for 'is not null' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " is not null";
            }
            case "equal":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '=' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " = " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "greaterThan":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '>' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " > " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "lessThan":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '<' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " < " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "greaterThanEqual":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '>=' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " >= " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "lessThanEqual":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '<=' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " <= " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "notEqual":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '!=' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " != " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            case "notEqualAnsi":
            {
                if (dynaFunc.parameters.size() != 2)
                {
                    return "/* Unable to transform operation: exactly 2 parameters are expected for '<>' operation */";
                }
                return renderRelationalOperationElement(dynaFunc.parameters.get(0), context) + " <> " + renderRelationalOperationElement(dynaFunc.parameters.get(1), context);
            }
            default:
            {
                return PureGrammarComposerUtility.convertIdentifier(dynaFunc.funcName) + "(" + LazyIterate.collect(dynaFunc.parameters, param -> renderRelationalOperationElement(param, context)).makeString(", ") + ")";
            }
        }
    }

    private static boolean isSelfJoin(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn tableAliasColumn)
    {
        return tableAliasColumn.table.table.equals(SELF_JOIN_TABLE_NAME) && tableAliasColumn.tableAlias.equals(SELF_JOIN_TABLE_NAME);
    }

    private static String renderTableAliasColumn(TableAliasColumn tableAliasColumn, RelationalGrammarComposerContext context)
    {
        return (tableAliasColumn.table.getDb() != null ? (context.getCurrentDatabase() != null && context.getCurrentDatabase().equals(tableAliasColumn.table.getDb()) ? "" : renderDatabasePointer(tableAliasColumn.table.getDb())) : "") +
                ((tableAliasColumn.table.schema != null && !tableAliasColumn.table.schema.equals("default") && !isSelfJoin(tableAliasColumn)) ? (tableAliasColumn.table.schema + ".") : "") +
                tableAliasColumn.table.table +
                (tableAliasColumn.column != null ? ("." + tableAliasColumn.column) : "");
    }

    private static String renderElementWithJoins(ElementWithJoins elementWithJoins, RelationalGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        if (!elementWithJoins.joins.isEmpty())
        {
            builder.append(LazyIterate.collect(elementWithJoins.joins, HelperRelationalGrammarComposer::renderJoinPointer).makeString(" > "));
        }
        if (elementWithJoins.relationalElement != null)
        {
            builder.append(!elementWithJoins.joins.isEmpty() ? " | " : "");
            builder.append(renderRelationalOperationElement(elementWithJoins.relationalElement, context));
        }
        return builder.toString();
    }

    private static String renderLiteralList(LiteralList literalList, RelationalGrammarComposerContext context)
    {
        return LazyIterate.collect(literalList.values, value -> renderRelationalOperationElement(value, context)).makeString("[", ", ", "]");
    }

    private static String renderLiteral(Literal literal, RelationalGrammarComposerContext context)
    {
        if (literal.value instanceof RelationalOperationElement)
        {
            return renderRelationalOperationElement(((RelationalOperationElement)literal.value), context);
        }
        else if (literal.value instanceof String)
        {
            return convertString(literal.value.toString(), true);
        }
        else if (literal.value instanceof Float || literal.value instanceof Double || literal.value instanceof Integer)
        {
            return literal.value.toString();
        }
        return unsupported(literal.getClass(), "relational operation literal value type");
    }

    public static String renderDatabaseSchema(Schema schema, RelationalGrammarComposerContext context)
    {
        int baseIndentation = 1;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("Schema ").append(PureGrammarComposerUtility.convertIdentifier(schema.name)).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        boolean nonEmpty = false;
        if (!schema.tables.isEmpty())
        {
            builder.append(LazyIterate.collect(schema.tables, table -> renderDatabaseTable(table, baseIndentation + 1, context)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (!schema.views.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(schema.views, view -> renderDatabaseView(view, baseIndentation + 1, context)).makeString("\n"));
            builder.append("\n");
        }
        builder.append(getTabString(baseIndentation)).append(")");
        return builder.toString();
    }

    public static String renderDatabaseTable(Table table, int baseIndentation, RelationalGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("Table ").append(table.name).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        boolean nonEmpty = false;
        if (!table.milestoning.isEmpty())
        {
            builder.append(getTabString(baseIndentation + 1)).append("milestoning\n");
            builder.append(getTabString(baseIndentation + 1)).append("(\n");
            builder.append(LazyIterate.collect(table.milestoning, milestoning -> renderDatabaseTableMilestoning(milestoning, baseIndentation + 2, context)).makeString(",\n"));
            builder.append("\n");
            builder.append(getTabString(baseIndentation + 1)).append(")");
            builder.append("\n");
            nonEmpty = true;
        }
        if (!table.columns.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(table.columns, column -> renderDatabaseTableColumn(column, table.primaryKey, baseIndentation + 1)).makeString(",\n"));
            builder.append("\n");
        }
        builder.append(getTabString(baseIndentation)).append(")");
        return builder.toString();
    }

    private static String renderDatabaseTableColumn(Column column, List<String> primaryKeys, int baseIndentation)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append(column.name).append(" ");
        if (column.type instanceof Char)
        {
            builder.append("CHAR(").append(((Char)column.type).size).append(")");
        }
        else if (column.type instanceof VarChar)
        {
            builder.append("VARCHAR(").append(((VarChar)column.type).size).append(")");
        }
        else if (column.type instanceof Numeric)
        {
            builder.append("NUMERIC(").append(((Numeric)column.type).precision).append(", ").append(((Numeric)column.type).scale).append(")");
        }
        else if (column.type instanceof Decimal)
        {
            builder.append("DECIMAL(").append(((Decimal)column.type).precision).append(", ").append(((Decimal)column.type).scale).append(")");
        }
        else if (column.type instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Float)
        {
            builder.append("FLOAT");
        }
        else if (column.type instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double)
        {
            builder.append("DOUBLE");
        }
        else if (column.type instanceof Real)
        {
            builder.append("REAL");
        }
        else if (column.type instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer)
        {
            builder.append("INTEGER");
        }
        else if (column.type instanceof BigInt)
        {
            builder.append("BIGINT");
        }
        else if (column.type instanceof SmallInt)
        {
            builder.append("SMALLINT");
        }
        else if (column.type instanceof TinyInt)
        {
            builder.append("TINYINT");
        }
        else if (column.type instanceof Date)
        {
            builder.append("DATE");
        }
        else if (column.type instanceof Timestamp)
        {
            builder.append("TIMESTAMP");
        }
        else if (column.type instanceof Binary)
        {
            builder.append("BINARY(").append(((Binary)column.type).size).append(")");
        }
        else if (column.type instanceof Varbinary)
        {
            builder.append("VARBINARY(").append(((Varbinary)column.type).size).append(")");
        }
        else if (column.type instanceof Bit)
        {
            builder.append("BIT");
        }
        else if (column.type instanceof Other)
        {
            builder.append("OTHER");
        }
        else
        {
            builder.append(unsupported(column.type.getClass(), "database table column type"));
        }
        // primary key and NON NULL
        if (primaryKeys.contains(column.name))
        {
            builder.append(" PRIMARY KEY");
        }
        else if (!column.nullable)
        {
            builder.append(" NOT NULL");
        }
        return builder.toString();
    }

    private static String renderDatabaseTableMilestoning(Milestoning milestoning, int baseIndentation, RelationalGrammarComposerContext context)
    {
        PureGrammarComposerContext pureGrammarComposerContext = context.toPureGrammarComposerContext();
        List<IRelationalGrammarComposerExtension> extensions = IRelationalGrammarComposerExtension.getExtensions(pureGrammarComposerContext);

        return IRelationalGrammarComposerExtension.process(milestoning,
                ListIterate.flatCollect(extensions, IRelationalGrammarComposerExtension::getExtraMilestoningComposers),
                pureGrammarComposerContext, baseIndentation);
    }

    public static String visitMilestoning(Milestoning milestoning, Integer baseIndentation, RelationalGrammarComposerContext context)
    {
        if (milestoning instanceof BusinessMilestoning)
        {
            BusinessMilestoning businessMilestoning = (BusinessMilestoning)milestoning;
            return getTabString(baseIndentation) + "business(" +
                    "BUS_FROM = " + businessMilestoning.from + ", " +
                    "BUS_THRU = " + businessMilestoning.thru +
                    (businessMilestoning.thruIsInclusive != null && businessMilestoning.thruIsInclusive ? ", THRU_IS_INCLUSIVE = true" : "") +
                    (businessMilestoning.infinityDate != null ? (", INFINITY_DATE = " + businessMilestoning.infinityDate.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context.toPureGrammarComposerContext()).build())) : "") +
                    ")";
        }
        else if (milestoning instanceof BusinessSnapshotMilestoning)
        {
            BusinessSnapshotMilestoning businessSnapshotMilestoning = (BusinessSnapshotMilestoning)milestoning;
            return getTabString(baseIndentation) + "business(BUS_SNAPSHOT_DATE = " + businessSnapshotMilestoning.snapshotDate + ")";
        }
        else if (milestoning instanceof ProcessingMilestoning)
        {
            ProcessingMilestoning processingMilestoning = (ProcessingMilestoning)milestoning;
            return getTabString(baseIndentation) + "processing(" +
                    "PROCESSING_IN = " + processingMilestoning.in + ", " +
                    "PROCESSING_OUT = " + processingMilestoning.out +
                    (processingMilestoning.outIsInclusive != null && processingMilestoning.outIsInclusive ? ", OUT_IS_INCLUSIVE = true" : "") +
                    (processingMilestoning.infinityDate != null ? (", INFINITY_DATE = " + processingMilestoning.infinityDate.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context.toPureGrammarComposerContext()).build())) : "") +
                    ")";
        }
        return null;
    }

    public static String renderDatabaseView(View view, int baseIndentation, RelationalGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("View ").append(view.name).append("\n");
        builder.append(getTabString(baseIndentation)).append("(\n");
        builder.append(view.filter != null ? (getTabString(baseIndentation + 1) + renderFilterMapping(view.filter) + "\n") : "");
        builder.append(!view.groupBy.isEmpty()
                ? (getTabString(baseIndentation + 1) + "~groupBy\n" +
                getTabString(baseIndentation + 1) + "(\n" +
                LazyIterate.collect(view.groupBy, op -> getTabString(baseIndentation + 2) + renderRelationalOperationElement(op, context)).makeString(",\n") +
                "\n" + getTabString(baseIndentation + 1) + ")\n")
                : ""
        );
        builder.append(view.distinct ? (getTabString(baseIndentation + 1) + "~distinct\n") : "");
        if (!view.columnMappings.isEmpty())
        {
            builder.append(LazyIterate.collect(view.columnMappings, columnMapping -> renderViewColumnMapping(columnMapping, view.primaryKey, baseIndentation + 1, context)).makeString(",\n"));
            builder.append("\n");
        }
        builder.append(getTabString(baseIndentation)).append(")");
        return builder.toString();
    }

    private static String renderViewColumnMapping(ColumnMapping columnMapping, List<String> primaryKeys, int baseIndentation, RelationalGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append(columnMapping.name).append(": ");
        builder.append(renderRelationalOperationElement(columnMapping.operation, context));
        // primary key
        if (primaryKeys.contains(columnMapping.name))
        {
            builder.append(" PRIMARY KEY");
        }
        return builder.toString();
    }

    private static String renderJoinPointer(JoinPointer joinPointer)
    {
        return (joinPointer.joinType != null ? ("(" + (joinPointer.joinType.equals("LEFT_OUTER") ? "OUTER" : joinPointer.joinType) + ") ") : "") +
                (joinPointer.db != null ? renderDatabasePointer(joinPointer.db) : "") +
                "@" + PureGrammarComposerUtility.convertIdentifier(joinPointer.name);
    }

    private static String renderDatabasePointer(String database)
    {
        return "[" + database + "]";
    }

    private static String renderJoinPointerForTheFirstFilterJoin(JoinPointer joinPointer)
    {
        return (joinPointer.db != null ? renderDatabasePointer(joinPointer.db) : "") + " " +
                (joinPointer.joinType != null ? ("(" + (joinPointer.joinType.equals("LEFT_OUTER") ? "OUTER" : joinPointer.joinType) + ") ") : "") +
                "@" + PureGrammarComposerUtility.convertIdentifier(joinPointer.name);
    }

    public static String renderFilterMapping(FilterMapping filterMapping)
    {
        int joinSize = filterMapping.joins.size();
        String firstJoin = joinSize > 0 ? renderJoinPointerForTheFirstFilterJoin(filterMapping.joins.get(0)) : "";
        List<JoinPointer> otherJoins = joinSize > 1 ? filterMapping.joins.subList(1, joinSize) : null;
        String body = firstJoin +
                (otherJoins != null ? " > " + LazyIterate.collect(otherJoins, HelperRelationalGrammarComposer::renderJoinPointer).makeString(" > "): "" ) +
                (!filterMapping.joins.isEmpty() ? " | " : "") + renderDatabasePointer(filterMapping.filter.db);

        return "~filter " + (filterMapping.filter.db != null ? body : "") +
                PureGrammarComposerUtility.convertIdentifier(filterMapping.filter.name);
    }

    private static boolean checkNullOrEmpty(String string)
    {
        return string == null || string.isEmpty();
    }

    public static String renderAbstractRelationalPropertyMapping(PropertyMapping propertyMapping, RelationalGrammarComposerContext context, Boolean renderSourceId)
    {
        if (propertyMapping instanceof RelationalPropertyMapping)
        {
            return renderRelationalPropertyMapping((RelationalPropertyMapping)propertyMapping, context, renderSourceId);
        }
        else if (propertyMapping instanceof EmbeddedRelationalPropertyMapping)
        {
            return renderEmbeddedRelationalPropertyMapping((EmbeddedRelationalPropertyMapping)propertyMapping, context);
        }
        else if (propertyMapping instanceof InlineEmbeddedPropertyMapping)
        {
            return renderInlineEmbeddedPropertyMapping((InlineEmbeddedPropertyMapping)propertyMapping, context);
        }
        return unsupported(propertyMapping.getClass(), "relational property mapping type");
    }

    private static String renderRelationalPropertyMapping(RelationalPropertyMapping relationalPropertyMapping, RelationalGrammarComposerContext context, Boolean renderSourceId)
    {
        String propertyString = context.getIndentationString() + (relationalPropertyMapping.localMappingProperty != null
                ? ("+" + PureGrammarComposerUtility.convertIdentifier(relationalPropertyMapping.property.property) + ": " + relationalPropertyMapping.localMappingProperty.type + "[" + HelperDomainGrammarComposer.renderMultiplicity(relationalPropertyMapping.localMappingProperty.multiplicity) + "]")
                : PureGrammarComposerUtility.convertIdentifier(relationalPropertyMapping.property.property) + (checkNullOrEmpty(relationalPropertyMapping.target) ? "" : "[" + (renderSourceId ? (checkNullOrEmpty(relationalPropertyMapping.source) ? "" : (relationalPropertyMapping.source + ",")) : "") + relationalPropertyMapping.target + "]")
        ) + ": ";
        String enumMappingValue = relationalPropertyMapping.enumMappingId != null ? "EnumerationMapping " + PureGrammarComposerUtility.convertIdentifier(relationalPropertyMapping.enumMappingId) + ": " : "";
        return propertyString + enumMappingValue + renderRelationalOperationElement(relationalPropertyMapping.relationalOperation, context);
    }

    private static String renderEmbeddedRelationalPropertyMapping(EmbeddedRelationalPropertyMapping embeddedRelationalPropertyMapping, RelationalGrammarComposerContext context)
    {
        if (embeddedRelationalPropertyMapping instanceof OtherwiseEmbeddedRelationalPropertyMapping)
        {
            return renderOtherwiseEmbeddedRelationalPropertyMapping((OtherwiseEmbeddedRelationalPropertyMapping)embeddedRelationalPropertyMapping, context);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(context.getIndentationString()).append(PureGrammarComposerUtility.convertIdentifier(embeddedRelationalPropertyMapping.property.property)).append("\n");
        builder.append(context.getIndentationString()).append("(\n");
        if (!embeddedRelationalPropertyMapping.classMapping.propertyMappings.isEmpty())
        {
            RelationalGrammarComposerContext indentedContext = RelationalGrammarComposerContext.Builder.newInstance(context).withIndentation(2).build();
            builder.append(LazyIterate.collect(embeddedRelationalPropertyMapping.classMapping.propertyMappings, propertyMapping -> renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext, false)).makeString(",\n"));
            builder.append("\n");
        }
        builder.append(context.getIndentationString()).append(")");
        return builder.toString();
    }

    private static String renderInlineEmbeddedPropertyMapping(InlineEmbeddedPropertyMapping inlineEmbeddedPropertyMapping, RelationalGrammarComposerContext context)
    {
        return context.getIndentationString() + PureGrammarComposerUtility.convertIdentifier(inlineEmbeddedPropertyMapping.property.property) + "() Inline[" + PureGrammarComposerUtility.convertIdentifier(inlineEmbeddedPropertyMapping.setImplementationId) + "]";
    }

    private static String renderOtherwiseEmbeddedRelationalPropertyMapping(OtherwiseEmbeddedRelationalPropertyMapping otherwiseEmbeddedRelationalPropertyMapping, RelationalGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(context.getIndentationString()).append(PureGrammarComposerUtility.convertIdentifier(otherwiseEmbeddedRelationalPropertyMapping.property.property)).append("\n");
        builder.append(context.getIndentationString()).append("(\n");
        if (!otherwiseEmbeddedRelationalPropertyMapping.classMapping.propertyMappings.isEmpty())
        {
            RelationalGrammarComposerContext indentedContext = RelationalGrammarComposerContext.Builder.newInstance(context).withIndentation(2).build();
            builder.append(LazyIterate.collect(otherwiseEmbeddedRelationalPropertyMapping.classMapping.propertyMappings, propertyMapping -> renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext, false)).makeString(",\n"));
            builder.append("\n");
        }
        builder.append(context.getIndentationString()).append(") Otherwise (").append(renderOtherwisePropertyMapping(otherwiseEmbeddedRelationalPropertyMapping.otherwisePropertyMapping, context)).append(")");
        return builder.toString();
    }

    private static String renderOtherwisePropertyMapping(RelationalPropertyMapping otherwisePropertyMapping, RelationalGrammarComposerContext context)
    {
        return "[" + (otherwisePropertyMapping.target == null ? "" : PureGrammarComposerUtility.convertIdentifier(otherwisePropertyMapping.target)) + "]: " + renderRelationalOperationElement(otherwisePropertyMapping.relationalOperation, context);
    }


    public static String visitRelationalDatabaseConnectionDatasourceSpecification(DatasourceSpecification _spec, RelationalGrammarComposerContext context)
    {
        if (_spec instanceof LocalH2DatasourceSpecification)
        {
            LocalH2DatasourceSpecification spec = (LocalH2DatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "LocalH2\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    (spec.testDataSetupCsv != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "testDataSetupCSV: " + convertString(spec.testDataSetupCsv, true) + ";\n" : "") +
                    (spec.testDataSetupSqls != null && !spec.testDataSetupSqls.isEmpty() ? context.getIndentationString() + getTabString(baseIndentation + 1) + "testDataSetupSqls: [\n" + ListIterate.collect(spec.testDataSetupSqls, s -> context.getIndentationString() + getTabString(baseIndentation + 2) + convertString(s, true)).makeString(",\n") + "\n" + context.getIndentationString() + getTabString(baseIndentation + 2) + "];\n" : "") +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        else if (_spec instanceof EmbeddedH2DatasourceSpecification)
        {
            EmbeddedH2DatasourceSpecification spec = (EmbeddedH2DatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "EmbeddedH2\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.databaseName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "directory: " + convertString(spec.directory, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "autoServerMode: " + spec.autoServerMode + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        else if (_spec instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification spec = (StaticDatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "Static\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.databaseName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        else if (_spec instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification spec = (SnowflakeDatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "Snowflake\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.databaseName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "account: " + convertString(spec.accountName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "warehouse: " + convertString(spec.warehouseName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "region: " + convertString(spec.region, true) + ";\n" +
                    (spec.cloudType != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "cloudType: " + convertString(spec.cloudType, true) + ";\n" : "") +
                    (spec.quotedIdentifiersIgnoreCase != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "quotedIdentifiersIgnoreCase: " + spec.quotedIdentifiersIgnoreCase + ";\n" : "") +

                    (spec.proxyHost != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyHost: " + convertString(spec.proxyHost, true) + ";\n" : "") +
                    (spec.proxyPort != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyPort: " + convertString(spec.proxyPort, true) + ";\n" : "") +
                    (spec.nonProxyHosts != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "nonProxyHosts: " + convertString(spec.nonProxyHosts, true) + ";\n" : "") +
                    (spec.accountType != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "accountType: " + spec.accountType + ";\n" : "") +
                    (spec.organization != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "organization: " + convertString(spec.organization, true) + ";\n" : "") +

                    (spec.role != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "role: " + convertString(spec.role, true) + ";\n" : "") +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        else if (_spec instanceof BigQueryDatasourceSpecification)
        {
            BigQueryDatasourceSpecification spec = (BigQueryDatasourceSpecification)_spec;
            int baseIndentation = 1;
            return "BigQuery\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "projectId: " + convertString(spec.projectId, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "defaultDataset: " + convertString(spec.defaultDataset, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }

        return null;
    }

    public static String visitRelationalDatabaseConnectionAuthenticationStrategy(AuthenticationStrategy _auth, RelationalGrammarComposerContext context)
    {
        if (_auth instanceof TestDatabaseAuthenticationStrategy)
        {
            return "Test";
        }
        else if (_auth instanceof DefaultH2AuthenticationStrategy)
        {
            return "DefaultH2";
        }
        else if (_auth instanceof DelegatedKerberosAuthenticationStrategy)
        {
            DelegatedKerberosAuthenticationStrategy auth = (DelegatedKerberosAuthenticationStrategy)_auth;
            int baseIndentation = 1;
            return "DelegatedKerberos" +
                    (auth.serverPrincipal != null
                            ? ("\n" +
                            context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                            context.getIndentationString() + getTabString(baseIndentation + 1) + "serverPrincipal: " + convertString(auth.serverPrincipal, true) + ";\n" +
                            context.getIndentationString() + getTabString(baseIndentation) + "}")
                            : ""
                    );
        }
        else if (_auth instanceof UserNamePasswordAuthenticationStrategy)
        {
            UserNamePasswordAuthenticationStrategy auth = (UserNamePasswordAuthenticationStrategy) _auth;
            int baseIndentation = 1;
            return "UserNamePassword" +
                    "\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    (auth.baseVaultReference == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "baseVaultReference: " + convertString(auth.baseVaultReference, true) + ";\n") +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "userNameVaultReference: " + convertString(auth.userNameVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "passwordVaultReference: " + convertString(auth.passwordVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        else if (_auth instanceof SnowflakePublicAuthenticationStrategy)
        {
            SnowflakePublicAuthenticationStrategy auth = (SnowflakePublicAuthenticationStrategy)_auth;
            int baseIndentation = 1;
            return "SnowflakePublic" +
                    "\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "publicUserName: " + convertString(auth.publicUserName, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "privateKeyVaultReference: " + convertString(auth.privateKeyVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "passPhraseVaultReference: " + convertString(auth.passPhraseVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";

        }
        else if (_auth instanceof GCPApplicationDefaultCredentialsAuthenticationStrategy)
        {
            GCPApplicationDefaultCredentialsAuthenticationStrategy auth = (GCPApplicationDefaultCredentialsAuthenticationStrategy)_auth;
            return "GCPApplicationDefaultCredentials";
        }
        else if (_auth instanceof GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy)
        {
            GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy auth = (GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy) _auth;
            int baseIndentation = 1;
            return "GCPWorkloadIdentityFederationWithAWS" +
                    "\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "workloadProjectNumber: " + convertString(auth.workloadProjectNumber, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "serviceAccountEmail: " + convertString(auth.serviceAccountEmail, true) + ";\n" +
                    (!auth.additionalGcpScopes.isEmpty() ? context.getIndentationString() + getTabString(baseIndentation + 1) + "additionalGcpScopes: [\n" + ListIterate.collect(auth.additionalGcpScopes, s -> context.getIndentationString() + getTabString(baseIndentation + 2) + convertString(s, true)).makeString(",\n") + "\n" + context.getIndentationString() + getTabString(baseIndentation + 2) + "];\n" : "") +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "workloadPoolId: " + convertString(auth.workloadPoolId, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "workloadProviderId: " + convertString(auth.workloadProviderId, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "awsAccountId: " + convertString(auth.awsAccountId, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "awsRegion: " + convertString(auth.awsRegion, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "awsRole: " + convertString(auth.awsRole, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "awsAccessKeyIdVaultReference: " + convertString(auth.awsAccessKeyIdVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation + 1) + "awsSecretAccessKeyVaultReference: " + convertString(auth.awsSecretAccessKeyVaultReference, true) + ";\n" +
                    context.getIndentationString() + getTabString(baseIndentation) + "}";
        }
        return null;
    }

    public static String writeMappersPostProcessor(String mapperName, List<Mapper> mappers, PureGrammarComposerContext context)
    {
        List<String> mapperStrings = ListIterate.collect(mappers, mapper -> visitMapper(mapper, context));

        int baseIndent = 2;

        return getTabString(baseIndent) + mapperName + "\n" +
                getTabString(baseIndent) + "{\n" +
                getTabString(baseIndent + 1) + "mappers:\n" +
                getTabString(baseIndent + 1) + "[\n" +
                String.join(",\n" + context.getIndentationString(), mapperStrings) +
                "\n" + getTabString(baseIndent + 1) + "];\n" +
                getTabString(baseIndent) + "}";
    }

    public static String visitMapperPostProcessor(MapperPostProcessor mapperPostProcessor, PureGrammarComposerContext context)
    {
        return writeMappersPostProcessor("mapper", mapperPostProcessor.mappers, context);
    }

    private static String visitMapper(Mapper mapper, PureGrammarComposerContext context)
    {
        if (mapper instanceof TableNameMapper)
        {
            return visitTableMapper((TableNameMapper)mapper);
        }
        else if (mapper instanceof SchemaNameMapper)
        {
            return visitSchemaMapper((SchemaNameMapper)mapper);
        }

        return unsupported(mapper.getClass(), "mapper type");
    }

    private static String visitTableMapper(TableNameMapper nameMapper)
    {
        int baseIndent = 4;

        return getTabString(baseIndent) + "table {" +
                "from: '" + nameMapper.from + "'; " +
                "to: '" + nameMapper.to + "'; " +
                "schemaFrom: '" + nameMapper.schema.from + "';" +
                " schemaTo: '" + nameMapper.schema.to + "';" +
                "}";
    }

    private static String visitSchemaMapper(SchemaNameMapper nameMapper)
    {
        int baseIndent = 4;

        return getTabString(baseIndent) + "schema {from: '" + nameMapper.from + "'; to: '" + nameMapper.to + "';}";
    }
}
