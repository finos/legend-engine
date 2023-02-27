// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.mongodb.schema.grammar.to;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpressionVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComparisonOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComputedFieldValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.JsonSchemaExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LogicalOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NorOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NotOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectQueryExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.QueryExprKeyValue;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.convertToStringWithQuotes;

public class ArgumentExpressionVisitorImpl implements ArgumentExpressionVisitor<String>
{

    private final int indentLevel;

    public ArgumentExpressionVisitorImpl(int tabIndex)
    {
        indentLevel = tabIndex;
    }

    public ArgumentExpressionVisitorImpl()
    {
        this(0);
    }


    @Override
    public String visit(AndOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "]";
        //return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "] }";
    }


    @Override
    public String visit(ComparisonOperatorExpression val)
    {
        return null;
    }

    @Override
    public String visit(ComputedFieldValue val)
    {
        return convertToStringWithQuotes(val.value);
    }

    @Override
    public String visit(EqOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.EQ) + "\" : " + expString;
    }

    @Override
    public String visit(FieldPathExpression val)
    {
        return convertToStringWithQuotes(val.fieldPath);
    }

    @Override
    public String visit(GTOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GT) + "\" : " + expString;
    }

    @Override
    public String visit(GTEOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GTE) + "\" : " + expString + " }";
    }

    @Override
    public String visit(InOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.IN) + "\" : " + expString + " }";
    }

    @Override
    public String visit(JsonSchemaExpression val)
    {
        return val.schemaExpression.accept(new BaseTypeVisitorImpl(this.indentLevel));
        //return null;
    }

    @Override
    public String visit(LTEOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LTE) + "\" : " + expString;
    }

    @Override
    public String visit(LTOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LT) + "\" : " + expString;
    }

    @Override
    public String visit(LiteralValue val)
    {
        return val.value.accept(new BaseTypeValueVisitorImpl());
    }

    @Override
    public String visit(LogicalOperatorExpression val)
    {
        return null;
    }

    @Override
    public String visit(NEOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NE) + "\" : " + expString;
    }

    @Override
    public String visit(NinOperatorExpression val)
    {
        String expString = visit(val.expression);
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NIN) + "\" : " + expString + " }";
    }

    @Override
    public String visit(NorOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NOR) + "\" : [" + String.join(",", expressionsString) + "] }";
    }

    @Override
    public String visit(NotOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NOT) + "\" : [" + String.join(",", expressionsString) + "] }";
    }

    @Override
    public String visit(ObjectExpression val)
    {
        return null;
    }

    @Override
    public String visit(ObjectQueryExpression val)
    {
        List<String> objPairString = val.keyValues.stream()
                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
        return "{" + String.join(",", objPairString) + "}";
    }

    @Override
    public String visit(OrOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.OR) + "\" : [" + String.join(",", expressionsString) + "] ";
    }


    @Override
    public String visit(QueryExprKeyValue pair)
    {
        String field = visitArgumentExpression(pair.key);
        String value = visitArgumentExpression(pair.value);
        return field + " : " + value;
    }

    private String visitArgumentExpression(ArgumentExpression argumentExpression)
    {
        if (argumentExpression == null)
        {
            return "{}";
        }

        return argumentExpression.accept(new ArgumentExpressionVisitorImpl());
    }

}