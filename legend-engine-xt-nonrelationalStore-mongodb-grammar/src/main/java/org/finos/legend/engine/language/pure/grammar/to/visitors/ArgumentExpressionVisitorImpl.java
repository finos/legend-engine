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

public class ArgumentExpressionVisitorImpl
{

//    private final int indentLevel;
//
//    public ArgumentExpressionVisitorImpl(int tabIndex)
//    {
//        indentLevel = tabIndex;
//    }
//
//    public ArgumentExpressionVisitorImpl()
//    {
//        this(0);
//    }
//
//
//
//    public String visit(AndOperatorExpression val)
//    {
//        List<String> expressionsString = val.expressions.stream()
//                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "]";
//        //return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "] }";
//    }
//
//
//
//    public String visit(ComparisonOperatorExpression val)
//    {
//        return null;
//    }
//
//
//    public String visit(ComputedFieldValue val)
//    {
//        return convertToStringWithQuotes(val.value);
//    }
//
//
//    public String visit(EqOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.EQ) + "\" : " + expString;
//    }
//
//
//    public String visit(FieldPathExpression val)
//    {
//        return convertToStringWithQuotes(val.fieldPath);
//    }
//
//
//    public String visit(GTOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GT) + "\" : " + expString;
//    }
//
//
//    public String visit(GTEOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.GTE) + "\" : " + expString + " }";
//    }
//
//
//    public String visit(InOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.IN) + "\" : " + expString + " }";
//    }
//
//
//    public String visit(JsonSchemaExpression val)
//    {
//        return val.schemaExpression.accept(new BaseTypeVisitorImpl(this.indentLevel));
//        //return null;
//    }
//
//
//    public String visit(LTEOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LTE) + "\" : " + expString;
//    }
//
//
//    public String visit(LTOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.LT) + "\" : " + expString;
//    }
//
//
//    public String visit(LiteralValue val)
//    {
//        return val.value.accept(new BaseTypeValueVisitorImpl());
//    }
//
//
//    public String visit(LogicalOperatorExpression val)
//    {
//        return null;
//    }
//
//
//    public String visit(NEOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NE) + "\" : " + expString;
//    }
//
//
//    public String visit(NinOperatorExpression val)
//    {
//        String expString = visit(val.expression);
//        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NIN) + "\" : " + expString + " }";
//    }
//
//
//    public String visit(NorOperatorExpression val)
//    {
//        List<String> expressionsString = val.expressions.stream()
//                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
//        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NOR) + "\" : [" + String.join(",", expressionsString) + "] }";
//    }
//
//
//    public String visit(NotOperatorExpression val)
//    {
//        List<String> expressionsString = val.expressions.stream()
//                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
//        return "{ \"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.NOT) + "\" : [" + String.join(",", expressionsString) + "] }";
//    }
//
//
//    public String visit(ObjectExpression val)
//    {
//        return null;
//    }
//
//
//    public String visit(ObjectQueryExpression val)
//    {
//        List<String> objPairString = val.keyValues.stream()
//                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
//        return "{" + String.join(",", objPairString) + "}";
//    }
//
//
//    public String visit(OrOperatorExpression val)
//    {
//        List<String> expressionsString = val.expressions.stream()
//                .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
//        return "\"" + ComposerUtility.lowerCaseOperatorAndAddDollar(Operator.OR) + "\" : [" + String.join(",", expressionsString) + "] ";
//    }
//
//
//
//    public String visit(QueryExprKeyValue pair)
//    {
//        String field = visitArgumentExpression(pair.key);
//        String value = visitArgumentExpression(pair.value);
//        return field + " : " + value;
//    }
//
//    private String visitArgumentExpression(MongoDBOperaionElement argumentExpression)
//    {
//        if (argumentExpression == null)
//        {
//            return "{}";
//        }
//
//        return argumentExpression.accept(new ArgumentExpressionVisitorImpl());
//    }

}