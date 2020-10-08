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

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.PathElement;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.PropertyPathElement;

import java.math.BigDecimal;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperValueSpecificationGrammarComposer
{
    public static final MutableMap<String, String> SPECIAL_INFIX;

    static
    {
        SPECIAL_INFIX = Maps.mutable.empty();
        SPECIAL_INFIX.put("equal", "==");
        SPECIAL_INFIX.put("lessThanEqual", "<=");
        SPECIAL_INFIX.put("lessThan", "<");
        SPECIAL_INFIX.put("greaterThanEqual", ">=");
        SPECIAL_INFIX.put("greaterThan", ">");
        SPECIAL_INFIX.put("plus", "+");
        SPECIAL_INFIX.put("minus", "-");
        SPECIAL_INFIX.put("times", "*");
        SPECIAL_INFIX.put("divide", "/");
        SPECIAL_INFIX.put("and", "&&");
        SPECIAL_INFIX.put("or", "||");
    }

    public static final MutableSet<String> FN_PREFIX = Sets.mutable.with("if", "col", "agg");

    public static final MutableSet<String> NEXT_LINE_FN = Sets.mutable.with("filter", "project", "and", "or", "groupBy");

    public static String renderFunction(AppliedFunction appliedFunction, boolean toCreateNewLine, DEPRECATED_PureGrammarComposerCore shiftedTransformer, DEPRECATED_PureGrammarComposerCore topParameterTransfomer, DEPRECATED_PureGrammarComposerCore transformer)
    {
        List<ValueSpecification> parameters = appliedFunction.parameters;
        String function = LazyIterate.collect(FastList.newListWith(appliedFunction.function.split("::")), PureGrammarComposerUtility::convertIdentifier).makeString("::");
        if (!parameters.isEmpty())
        {
            ValueSpecification firstParameter = parameters.get(0);
            String top = firstParameter.accept(topParameterTransfomer);
            if (function.equals("not") || firstParameter instanceof AppliedFunction && SPECIAL_INFIX.get(((AppliedFunction) firstParameter).function) != null)
            {
                return function + "(" + top + ")";
            }
            return top + (toCreateNewLine ? shiftedTransformer.returnChar() + shiftedTransformer.getIndentationString() : "") + (shiftedTransformer.isRenderingHTML() ? "<span class='pureGrammar-arrow'>" : "") + "->" + (shiftedTransformer.isRenderingHTML() ? "</span>" : "")
                    + renderFunctionName(function, transformer)
                    + (toCreateNewLine ? shiftedTransformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(shiftedTransformer, 1) : "") + "("
                    + (toCreateNewLine ? shiftedTransformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(shiftedTransformer, 3) : "")
                    + ListIterate.collect(parameters.subList(1, parameters.size()), p -> p.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(shiftedTransformer).withIndentation(3).build()))
                                 .makeString(", " + (toCreateNewLine ? shiftedTransformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(shiftedTransformer, 3) : ""))
                    + (toCreateNewLine ? shiftedTransformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(shiftedTransformer, 1) : "") + ")";
        }
        return renderFunctionName(function, transformer) + "()";
    }

    public static String renderFunctionName(String name, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return (transformer.isRenderingHTML() ? "<span class='pureGrammar-function'>" : "") + name + (transformer.isRenderingHTML() ? "</span>" : "");
    }

    public static String possiblyAddParenthesis(String function, ValueSpecification param, DEPRECATED_PureGrammarComposerCore transformer)
    {
        if (function.equals("and") || function.equals("or"))
        {
            if (param instanceof AppliedFunction && SPECIAL_INFIX.get(((AppliedFunction) param).function) != null)
            {
                return "(" + param.accept(transformer) + ")";
            }
        }
        else if ("divide".equals(function) || "times".equals(function) || "plus".equals(function) || "minus".equals(function))
        {
            if (isLowerPrecedenceFunction(param, function))
            {
                return "(" + param.accept(transformer) + ")";
            }
        }

        return param.accept(transformer);
    }

    private static boolean isLowerPrecedenceFunction(ValueSpecification v, String function)
    {
        if (v instanceof AppliedFunction)
        {
            String compareTo = ((AppliedFunction) v).function;
            return (isMultDiv(function) && !isMultDiv(compareTo)) || (isPlusMinus(function) && isRelational(compareTo));
        }

        return false;
    }

    private static boolean isMultDiv(String function)
    {
        return "times".equals(function) || "divide".equals(function);
    }

    private static boolean isPlusMinus(String function)
    {
        return "plus".equals(function) || "minus".equals(function);
    }

    private static boolean isRelational(String function)
    {
        return "lessThan".equals(function) || "lessThanEqual".equals(function) || "greaterThan".equals(function) || "greaterThanEqual".equals(function);
    }


    public static String renderCollection(List<?> values, org.eclipse.collections.api.block.function.Function<Object, String> func, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return "[" + (transformer.isRenderingPretty() ? transformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(transformer, 2) : "")
                + LazyIterate.collect(values, func).makeString(", " + (transformer.isRenderingPretty() ? transformer.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(transformer, 2) : ""))
                + (transformer.isRenderingPretty() ? transformer.returnChar() + transformer.getIndentationString() : "") + "]";
    }

    public static String renderDecimal(BigDecimal b, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return transformer.isRenderingHTML() ? "<span class='pureGrammar-decimal'>" + b + "</span>" : String.valueOf(b);
    }

    public static String renderString(String s, DEPRECATED_PureGrammarComposerCore transformer)
    {
        String resultString;
        if (transformer.isRenderingHTML())
        {
            resultString = "<span class='pureGrammar-string'>'" + s + "'</span>";
        }
        else if (transformer.isValueSpecificationExternalParameter())
        {
            resultString = s;
        }
        else
        {
            resultString = convertString(s, true);
        }
        return resultString;
    }

    public static String renderBoolean(Boolean b, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return transformer.isRenderingHTML() ? "<span class='pureGrammar-boolean'>" + b + "</span>" : String.valueOf(b);
    }

    public static String renderFloat(Double f, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return transformer.isRenderingHTML() ? "<span class='pureGrammar-float'>" + f + "</span>" : String.valueOf(f);
    }

    public static String renderInteger(Long b, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return transformer.isRenderingHTML() ? "<span class='pureGrammar-integer'>" + b + "</span>" : String.valueOf(b);
    }

    public static String renderDate(String s, DEPRECATED_PureGrammarComposerCore transformer)
    {
        String dateString;
        if (transformer.isRenderingHTML())
        {
            dateString = "<span class='pureGrammar-datetime'>" + s + "</span>";
        }
        else if (transformer.isValueSpecificationExternalParameter())
        {
            dateString = s.replaceFirst("%", "").replaceAll(".0000", "");
        }
        else
        {
            dateString = s;
        }
        return dateString;
    }

    public static String renderPathElement(PathElement pathElement, DEPRECATED_PureGrammarComposerCore transformer)
    {
        if (pathElement instanceof PropertyPathElement)
        {
            PropertyPathElement propertyPathElement = (PropertyPathElement) pathElement;
            return (transformer.isRenderingHTML() ? "<span class=pureGrammar-property>" : "") + propertyPathElement.property + (transformer.isRenderingHTML() ? "</span>" : "")
                    + (propertyPathElement.parameters.size() > 1 ? "(" + ListAdapter.adapt(propertyPathElement.parameters).collect(l -> l.accept(transformer)).makeString(", ") + ")" : "");
        }
        return unsupported(pathElement.getClass());
    }

    public static String printFullPath(String fullPath, DEPRECATED_PureGrammarComposerCore transformer)
    {
        if (transformer.isRenderingHTML())
        {
            int index = fullPath.lastIndexOf("::");
            if (index == -1)
            {
                return "<span class='pureGrammar-packageableElement'>" + fullPath + "</span>";
            }
            return "<span class='pureGrammar-package'>" + fullPath.substring(0, index + 2) + "</span><span class='pureGrammar-packageableElement'>" + fullPath.substring(index + 2) + "</span>";
        }
        return fullPath;
    }
}
