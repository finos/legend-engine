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

package org.finos.legend.engine.language.sql.grammar.to;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.sql.metamodel.*;

import java.util.List;
import java.util.stream.Collectors;

public class SQLGrammarComposer
{
    private final MutableMap<ComparisonOperator, String> COMPARATOR = UnifiedMap.newMapWith(
            Tuples.pair(ComparisonOperator.EQUAL, "="),
            Tuples.pair(ComparisonOperator.NOT_EQUAL, "!="),
            Tuples.pair(ComparisonOperator.GREATER_THAN, ">"),
            Tuples.pair(ComparisonOperator.LESS_THAN, "<"),
            Tuples.pair(ComparisonOperator.LESS_THAN_OR_EQUAL, "<="),
            Tuples.pair(ComparisonOperator.GREATER_THAN_OR_EQUAL, ">="),
            Tuples.pair(ComparisonOperator.IS_DISTINCT_FROM, "IS DISTINCT FROM"),
            Tuples.pair(ComparisonOperator.IS_NOT_DISTINCT_FROM, "IS NOT DISTINCT FROM"),
            Tuples.pair(ComparisonOperator.REGEX_MATCH, "~"),
            Tuples.pair(ComparisonOperator.REGEX_MATCH_CI, "~*"),
            Tuples.pair(ComparisonOperator.REGEX_NO_MATCH, "!~"),
            Tuples.pair(ComparisonOperator.REGEX_NO_MATCH_CI, "!~*"),
            Tuples.pair(ComparisonOperator.LIKE, "~~"),
            Tuples.pair(ComparisonOperator.ILIKE, "~~*"),
            Tuples.pair(ComparisonOperator.NOT_LIKE, "!~~"),
            Tuples.pair(ComparisonOperator.NOT_ILIKE, "!~~*")
    );
    private final MutableMap<LogicalBinaryType, String> BINARY_COMPARATOR = UnifiedMap.newMapWith(
            Tuples.pair(LogicalBinaryType.AND, "AND"),
            Tuples.pair(LogicalBinaryType.OR, "OR")
    );

    private final MutableMap<ArithmeticType, String> ARITHMETIC_OPERATORS = UnifiedMap.newMapWith(
            Tuples.pair(ArithmeticType.ADD, "+"),
            Tuples.pair(ArithmeticType.SUBTRACT, "-"),
            Tuples.pair(ArithmeticType.MULTIPLY, "*"),
            Tuples.pair(ArithmeticType.DIVIDE, "/"),
            Tuples.pair(ArithmeticType.MODULUS, "%"),
            Tuples.pair(ArithmeticType.POWER, "^")
    );

    private final MutableMap<JoinType, String> JOIN_KIND = UnifiedMap.newMapWith(
            Tuples.pair(JoinType.LEFT, "LEFT OUTER"),
            Tuples.pair(JoinType.RIGHT, "RIGHT OUTER"),
            Tuples.pair(JoinType.INNER, "INNER"),
            Tuples.pair(JoinType.CROSS, "CROSS")
    );

    private final MutableMap<CurrentTimeType, String> CURRENT_TIME = UnifiedMap.newMapWith(
            Tuples.pair(CurrentTimeType.TIME, "CURRENT_TIME"),
            Tuples.pair(CurrentTimeType.TIMESTAMP, "CURRENT_TIMESTAMP"),
            Tuples.pair(CurrentTimeType.DATE, "CURRENT_DATE")
    );

    private final MutableMap<FrameBoundType, String> FRAME_BOUND_TYPES = UnifiedMap.newMapWith(
            Tuples.pair(FrameBoundType.CURRENT_ROW, "CURRENT ROW"),
            Tuples.pair(FrameBoundType.FOLLOWING, "FOLLOWING"),
            Tuples.pair(FrameBoundType.PRECEDING, "PRECEDING"),
            Tuples.pair(FrameBoundType.UNBOUNDED_FOLLOWING, "UNBOUNDED FOLLOWING"),
            Tuples.pair(FrameBoundType.UNBOUNDED_PRECEDING, "UNBOUNDED PRECEDING")
    );

    private final MutableMap<JSONOperator, String> JSON_OPERATORS = UnifiedMap.newMapWith(
            Tuples.pair(JSONOperator.JSON_EXTRACT, "->"),
            Tuples.pair(JSONOperator.JSON_EXTRACT_TEXT, "->>"),
            Tuples.pair(JSONOperator.JSON_PATH_EXTRACT, "#>"),
            Tuples.pair(JSONOperator.JSON_PATH_EXTRACT_TEXT, "#>>"),
            Tuples.pair(JSONOperator.JSONB_CONTAIN_RIGHT, "@>"),
            Tuples.pair(JSONOperator.JSONB_CONTAIN_LEFT, "<@"),
            Tuples.pair(JSONOperator.JSONB_CONTAIN_ALL_TOP_KEY, "?&"),
            Tuples.pair(JSONOperator.JSONB_PATH_DELETE, "#-"),
            Tuples.pair(JSONOperator.JSONB_PATH_CONTAIN_ANY_VALUE, "@?"),
            Tuples.pair(JSONOperator.JSONB_PATH_PREDICATE_CHECK, "@@")
    );

    private final MutableMap<BitwiseBinaryOperator, String> BITWISE_OPERATORS = UnifiedMap.newMapWith(
            Tuples.pair(BitwiseBinaryOperator.OR, "|"),
            Tuples.pair(BitwiseBinaryOperator.AND, "&"),
            Tuples.pair(BitwiseBinaryOperator.XOR, "#")
    );

    private final MutableMap<BitwiseShiftDirection, String> BITWISE_SHIFT_DIRECTIONS = UnifiedMap.newMapWith(
            Tuples.pair(BitwiseShiftDirection.LEFT, "<<"),
            Tuples.pair(BitwiseShiftDirection.RIGHT, ">>")
    );


    private SQLGrammarComposer()
    {
    }

    public static SQLGrammarComposer newInstance()
    {
        return new SQLGrammarComposer();
    }

    public String renderNode(Node node)
    {
        return node.accept(new NodeVisitor<String>()
        {
            @Override
            public String visit(AliasedRelation val)
            {
                String columns = val.columnNames.isEmpty() ? "" : FastList.newList(val.columnNames).makeString(" (", ", ",  ")");

                return visit(val.relation) + " AS " + quoteIfNeeded(val.alias) + columns;
            }

            @Override
            public String visit(AllColumns val)
            {
                return val.prefix != null ? val.prefix + ".*" : "*";
            }

            @Override
            public String visit(FunctionCall val)
            {
                String args = visit(val.arguments, ", ");
                String window = val.window != null ? " OVER (" + visit(val.window) + ")" : "";
                String group = val.group != null ? " " + visit(val.group) : "";
                String orderBy = val.orderBy != null && !val.orderBy.isEmpty()
                        ? " ORDER BY " + visit(val.orderBy, ", ")
                        : "";

                return String.join(".", val.name.parts) + "(" + args + orderBy + ")" + group + window;
            }

            @Override
            public String visit(Group group)
            {
                return "WITHIN GROUP (ORDER BY " + visit(group.orderBy) + ")";
            }

            @Override
            public String visit(SimpleCaseExpression val)
            {
                String operand = val.operand.accept(this);
                String when = visit(val.whenClauses, " ");
                String def = val.defaultValue == null ? "" :  " ELSE " + val.defaultValue.accept(this);

                return "CASE " + operand + when + def + " END";
            }

            @Override
            public String visit(SearchedCaseExpression val)
            {
                String when = visit(val.whenClauses, " ");
                String def = val.defaultValue == null ? "" :  " ELSE " + val.defaultValue.accept(this);
                return "CASE " + when + def + " END";
            }

            @Override
            public String visit(WhenClause val)
            {
                return "WHEN " + val.operand.accept(this) + " THEN " + val.result.accept(this);
            }

            @Override
            public String visit(Window val)
            {
                if (val.windowRef != null)
                {
                    return val.windowRef;
                }

                String partitions = val.partitions != null && !val.partitions.isEmpty()
                        ? "PARTITION BY " + visit(val.partitions, ", ")
                        : "";
                String orderBy = val.orderBy != null && !val.orderBy.isEmpty()
                        ? " ORDER BY " + visit(val.orderBy, ", ")
                        : "";

                String frame = val.windowFrame != null ? visit(val.windowFrame) : "";

                return partitions + orderBy + frame;
            }

            @Override
            public String visit(WindowFrame val)
            {
                return val.mode.toString() + " BETWEEN " + visit(val.start) + " AND " + visit(val.end);
            }

            @Override
            public String visit(ComparisonExpression val)
            {
                String left = val.left.accept(this);
                String right = val.right.accept(this);
                String operator = COMPARATOR.get(val.operator);

                if (operator == null)
                {
                    throw new IllegalArgumentException("Unknown operator: " + val.operator);
                }
                return left + " " + operator + " " + right;
            }

            @Override
            public String visit(CurrentTime val)
            {
                String params = val.precision != null ? "(" + val.precision + ")" : "";
                return CURRENT_TIME.get(val.type) + params;
            }

            @Override
            public String visit(LogicalBinaryExpression val)
            {
                String left = val.left.accept(this);
                String right = val.right.accept(this);
                String operator = BINARY_COMPARATOR.get(val.type);
                if (operator == null)
                {
                    throw new IllegalArgumentException("Unknown operator: " + val.type);
                }
                return left + " " + operator + " " + right;
            }

            @Override
            public String visit(NotExpression val)
            {
                String value = val.value.accept(this);

                return "not " + value;
            }

            @Override
            public String visit(NullLiteral val)
            {
                return "NULL";
            }

            @Override
            public String visit(PositionalParameterExpression val)
            {
                return "$" + val.index;
            }

            @Override
            public String visit(ParameterPlaceholderExpression val)
            {
                return "?";
            }

            @Override
            public String visit(NegativeExpression val)
            {
                String value = val.value.accept(this);

                return "-" + value;
            }

            @Override
            public String visit(ArithmeticExpression val)
            {
                String type = ARITHMETIC_OPERATORS.get(val.type);
                return "(" + val.left.accept(this) + " " + type + " " + val.right.accept(this) + ")";
            }

            @Override
            public String visit(Expression val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(Extract val)
            {
                String field = val.field.name();
                String source = val.expression.accept(this);

                return "EXTRACT(" + field + " FROM " + source + ")";
            }

            @Override
            public String visit(FrameBound val)
            {
                String expression = val.value != null ? visit(val.value) + " " : "";

                return expression + FRAME_BOUND_TYPES.get(val.type);
            }

            @Override
            public String visit(InListExpression val)
            {
                return visit(val.values, ", ");
            }

            @Override
            public String visit(InPredicate val)
            {
                return val.value.accept(this) + " IN (" + val.valueList.accept(this) + ")";
            }

            @Override
            public String visit(IntegerLiteral val)
            {
                return String.valueOf(val.value);
            }

            @Override
            public String visit(IntervalLiteral val)
            {
                List<String> parts = FastList.newList();
                addIntervalPart(val.years, "YEARS", parts);
                addIntervalPart(val.months, "MONTHS", parts);
                addIntervalPart(val.weeks, "WEEKS", parts);
                addIntervalPart(val.days, "DAYS", parts);
                addIntervalPart(val.hours, "HOURS", parts);
                addIntervalPart(val.minutes, "MINUTES", parts);
                addIntervalPart(val.seconds, "SECONDS", parts);
                addIntervalPart(val.milliseconds, "MILLISECONDS", parts);
                addIntervalPart(val.microseconds, "MICROSECONDS", parts);
                return "INTERVAL '" + String.join(" ", parts) + "'";
            }

            private void addIntervalPart(Long value, String part, List<String> parts)
            {
                if (value != null)
                {
                    String partName = value == 1L ? part.replaceAll("S$", "") : part;
                    parts.add(value + " " + partName);
                }
            }

            @Override
            public String visit(IsNotNullPredicate val)
            {
                return val.value.accept(this) + " IS NOT NULL";
            }

            @Override
            public String visit(IsNullPredicate val)
            {
                return val.value.accept(this) + " IS NULL";
            }

            @Override
            public String visit(JSONExpression val)
            {
                String operator = JSON_OPERATORS.get(val.operator);
                return val.left.accept(this) + " " + operator + " " + val.right.accept(this);
            }

            @Override
            public String visit(Join val)
            {
                NodeVisitor<String> visitor = this;
                String type = JOIN_KIND.get(val.type);
                String left = val.left.accept(this);
                String right = val.right.accept(this);
                String natural = val.criteria instanceof NaturalJoin ? "NATURAL " : "";

                String criteria = val.criteria != null ? val.criteria.accept(new JoinCriteriaVisitor<String>()
                {
                    @Override
                    public String visit(JoinOn val)
                    {
                        return "ON (" + val.expression.accept(visitor) + ")";
                    }

                    @Override
                    public String visit(JoinUsing val)
                    {
                        return "USING (" + String.join(", ", val.columns) + ")";
                    }

                    @Override
                    public String visit(NaturalJoin val)
                    {
                        return "";
                    }
                }) : "";

                return left + " " + natural + type + " JOIN " + right + " " + criteria;
            }

            @Override
            public String visit(LikePredicate val)
            {
                String like = (val.ignoreCase ? " ILIKE " : " LIKE ");
                String escape = val.escape != null ? " ESCAPE " + visit(val.escape) : "";

                return val.value.accept(this) + like + val.pattern.accept(this) + escape;
            }

            @Override
            public String visit(Literal val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(LongLiteral val)
            {
                return Long.toString(val.value);
            }

            @Override
            public String visit(NamedArgumentExpression val)
            {
                return val.name + " => " + val.expression.accept(this);
            }

            @Override
            public String visit(DoubleLiteral val)
            {
                return Double.toString(val.value);
            }

            @Override
            public String visit(BooleanLiteral val)
            {
                return Boolean.toString(val.value);
            }

            @Override
            public String visit(Cast val)
            {
                String value = val.expression.accept(this);
                String type = val.type.accept(this);
                return "CAST(" + value + " AS " + type + ")";
            }

            @Override
            public String visit(CollectionColumnType val)
            {
                return val.innerType.accept(this) + "[]";
            }

            @Override
            public String visit(ColumnType val)
            {
                List<String> parameters = ListIterate.collect(val.parameters, Object::toString);
                String parameterString = parameters.isEmpty() ? "" : parameters.stream().collect(Collectors.joining(", ", "(", ")"));
                return val.name + parameterString;
            }

            @Override
            public String visit(WithQuery val)
            {
                String columns = val.columns.isEmpty() ? "" : FastList.newList(val.columns).makeString(" (", ", ",  ")");
                return val.name + columns + " AS (" + visit(val.query) + ")";
            }

            @Override
            public String visit(With val)
            {
                return !val.withQueries.isEmpty() ? "WITH " + FastList.newList(val.withQueries).collect(this::visit).makeString("", ", ", " ") : "";
            }

            @Override
            public String visit(ArrayLiteral val)
            {
                return "[" + visit(val.values, ", ") + "]";
            }

            @Override
            public String visit(ArraySliceExpression val)
            {
                String start = val.from != null ? visit(val.from) : "";
                String end = val.to != null ? visit(val.to) : "";

                return visit(val.value) + "[" + start + ":" + end + "]";
            }

            @Override
            public String visit(BetweenPredicate val)
            {
                String value = val.value.accept(this);
                String min = val.min.accept(this);
                String max = val.max.accept(this);

                return value + " BETWEEN " + min + " AND " +  max;
            }

            @Override
            public String visit(BitwiseBinaryExpression val)
            {
                return visit(val.left) + " " + BITWISE_OPERATORS.get(val.operator) + " " + visit(val.right);
            }

            @Override
            public String visit(BitwiseShiftExpression val)
            {
                return visit(val.value) + " " + BITWISE_SHIFT_DIRECTIONS.get(val.direction) + " " + visit(val.shift);
            }

            @Override
            public String visit(QualifiedNameReference val)
            {
                String value = String.join(".", val.name.parts);
                return quoteIfNeeded(value);
            }

            @Override
            public String visit(Query val)
            {
                return (val.with != null ? visit(val.with) : "") + val.queryBody.accept(this)
                        + (val.orderBy.isEmpty() ? "" : " order by " + visit(val.orderBy, ", "))
                        + (val.limit == null ? "" : " limit " + val.limit.accept(this))
                        + (val.offset == null ? "" : " offset " + val.offset.accept(this));
            }

            @Override
            public String visit(QueryBody val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(QuerySpecification val)
            {
                return val.select.accept(this)
                        + (val.from == null ? "" : " from " + visit(val.from, ""))
                        + (val.where == null ? "" : " where " + val.where.accept(this))
                        + (val.groupBy == null || val.groupBy.isEmpty() ? "" : " group by " + visit(val.groupBy, ", "))
                        + (val.having == null ? "" : " having " + visit(val.having))
                        + (val.orderBy.isEmpty() ? "" : " order by " + visit(val.orderBy, ", "))
                        + (val.limit == null ? "" : " limit " + visit(val.limit))
                        + (val.offset == null ? "" : " offset " + visit(val.offset));
            }

            @Override
            public String visit(Relation val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(Select val)
            {
                return "select " + (val.distinct ? "distinct " : "") + visit(val.selectItems, ", ");
            }

            @Override
            public String visit(SelectItem val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(SetOperation val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(SingleColumn val)
            {
                return val.expression.accept(this) + (val.alias == null ? "" : " AS " + quoteIfNeeded(val.alias));
            }

            @Override
            public String visit(SortItem val)
            {
                String sortItem = "";
                sortItem += val.sortKey.accept(this);
                switch (val.ordering)
                {
                    case ASCENDING:
                        sortItem += " ASC";
                        break;
                    case DESCENDING:
                        sortItem += " DESC";
                        break;
                }
                switch (val.nullOrdering)
                {
                    case FIRST:
                        sortItem += " NULLS FIRST";
                        break;
                    case LAST:
                        sortItem += " NULLS LAST";
                        break;
                }
                return sortItem;
            }

            @Override
            public String visit(Statement val)
            {
                return val.accept(this);
            }

            @Override
            public String visit(StringLiteral val)
            {
                return "'" + val.value + "'";
            }

            @Override
            public String visit(SubqueryExpression val)
            {
                return "(" + val.query.accept(this) + ")";
            }

            @Override
            public String visit(SubscriptExpression val)
            {
                return visit(val.value) + "[" + visit(val.index) + "]";
            }

            @Override
            public String visit(Table val)
            {
                return String.join(".", val.name.parts);
            }

            @Override
            public String visit(TableFunction val)
            {
                return visit(val.functionCall);
            }

            @Override
            public String visit(TableSubquery val)
            {
                return "(" + visit(val.query) + ")";
            }

            @Override
            public String visit(Trim val)
            {
                String chars = val.characters != null ? " " + visit(val.characters) : "";
                return "trim(" + val.mode.name() + chars + " FROM " + visit(val.value) + ")";
            }

            @Override
            public String visit(Union val)
            {
                String operator = val.distinct ? " UNION " : " UNION ALL ";
                return val.left.accept(this) + operator + val.right.accept(this);
            }

            @Override
            public String visit(Values val)
            {
                return "VALUES " + visit(val.rows, ", ");
            }

            @Override
            public String visit(ValuesList val)
            {
                return "(" + visit(val.values, ", ") + ")";
            }

            private String visit(List<? extends Node> nodes, String delimiter)
            {
                return nodes.stream()
                        .map(node -> node.accept(this))
                        .collect(Collectors.joining(delimiter));
            }

            private String quoteIfNeeded(String value)
            {
                return value.contains(" ") ? "\"" + value + "\"" : value;
            }
        });
    }
}
