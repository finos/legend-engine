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

package org.finos.legend.engine.language.pure.grammar.to.visitors;

import org.finos.legend.engine.language.pure.grammar.to.ComposerUtility;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDBOperationElement;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDBOperationElementVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AggregationPipeline;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComparisonOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComputedFieldValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ExprQueryExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.JsonSchemaExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LogicalOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NorOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NotOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectQueryExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ProjectStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.QueryExprKeyValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ViewPipeline;

import java.util.List;
import java.util.stream.Collectors;

public class MongoDBOperationElementVisitorImpl implements MongoDBOperationElementVisitor<String>
{
    private final int indentLevel;

    public MongoDBOperationElementVisitorImpl(int tabIndex)
    {
        indentLevel = tabIndex;
    }

    public MongoDBOperationElementVisitorImpl()
    {
        this(0);
    }


    @Override
    public String visit(AggregationPipeline val)
    {
        return null;
    }

    @Override
    public String visit(AndOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "] }";
    }

    @Override
    public String visit(ArgumentExpression val)
    {
        if (val != null)
        {
            return "{}";
        }
        else
        {
            return "null";
        }
    }


    @Override
    public String visit(ComparisonOperatorExpression val)
    {
        return null;
    }

    @Override
    public String visit(ComputedFieldValue val)
    {
        return ComposerUtility.convertToStringWithQuotes(val.value);
    }

    @Override
    public String visit(DatabaseCommand val)
    {
        return null;
    }

    @Override
    public String visit(EqOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.EQ) + "\" : " + expString + " }";
    }

    @Override
    public String visit(ExprQueryExpression val)
    {
        return "{ \"$expr\" " + ": " + visitMongoDBOperationElement(val.expression) + " }";
    }

    private List<String> getExpressionStrings(List<ArgumentExpression> argumentExpressions)
    {
        List<String> exprStrings = argumentExpressions.stream().map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
        return exprStrings;
    }

    @Override
    public String visit(FieldPathExpression val)
    {
        return ComposerUtility.convertToStringWithQuotes(val.fieldPath);
    }

    @Override
    public String visit(GTOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GT) + "\" : " + expString + " }";
    }

    @Override
    public String visit(GTEOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GTE) + "\" : " + expString + " }";
    }

    @Override
    public String visit(InOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
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
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LTE) + "\" : " + expString + " }";
    }

    @Override
    public String visit(LTOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LT) + "\" : " + expString + " }";
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
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NE) + "\" : " + expString + " }";
    }

    @Override
    public String visit(NinOperatorExpression val)
    {
        List<String> exprStrings = getExpressionStrings(val.expressions);
        String expString = "[" + String.join(",", exprStrings) + "]";
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NIN) + "\" : " + expString + " }";
    }

    @Override
    public String visit(NorOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NOR) + "\" : [" + String.join(",", expressionsString) + "] }";
    }

    @Override
    public String visit(NotOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
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
                .map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
        return "{" + String.join(",", objPairString) + "}";
    }

    @Override
    public String visit(OrOperatorExpression val)
    {
        List<String> expressionsString = val.expressions.stream()
                .map(x -> visitMongoDBOperationElement(x)).collect(Collectors.toList());
        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.OR) + "\" : [" + String.join(",", expressionsString) + "] }";
    }


    @Override
    public String visit(QueryExprKeyValue pair)
    {
        String field = visitMongoDBOperationElement(pair.key);
        String value = visitMongoDBOperationElement(pair.value);
        return field + " : " + value;
    }

    @Override
    public String visit(Stage val)
    {
        return null;
    }

    @Override
    public String visit(ViewPipeline val)
    {
        return null;
    }

    private String visitMongoDBOperationElement(MongoDBOperationElement mongoDBOperationElement)
    {
        if (mongoDBOperationElement == null)
        {
            return "{}";
        }

        return mongoDBOperationElement.accept(new MongoDBOperationElementVisitorImpl());
    }


    @Override
    public String visit(MatchStage val)
    {
        return "{ \"$match\" : " +
                val.expression.accept(new MongoDBOperationElementVisitorImpl()) +
                " }";
    }

    @Override
    public String visit(ProjectStage val)
    {
        String projectObject = val.projections.accept(new MongoDBOperationElementVisitorImpl());
        String projectObjectWithoutId = projectObject.substring(0, projectObject.length() - 1) + ", \"_id\": 0";
        return "{ \"$project\" : " + projectObjectWithoutId + " } }";
    }

}
