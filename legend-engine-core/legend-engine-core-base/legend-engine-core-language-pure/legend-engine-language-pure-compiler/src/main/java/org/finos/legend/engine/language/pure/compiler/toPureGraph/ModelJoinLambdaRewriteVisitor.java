// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.path.valuespecification.constant.classInstance.Path;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CBoolean;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CByteArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDateTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CFloat;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CInteger;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CLatestDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

/**
 * Rewrites a ModelJoin join-condition lambda body by substituting the user-declared
 * parameter names ({@code srcName} / {@code tgtName}) with the runtime sentinels
 * {@link HelperMappingBuilder#MODEL_JOIN_SOURCE_VAR _mj_src} /
 * {@link HelperMappingBuilder#MODEL_JOIN_TARGET_VAR _mj_tgt} that downstream consumers
 * (router, plan generator) bind to the source / target sets.
 * <p>
 * Uses the {@link ValueSpecificationVisitor} interface so that every {@link ValueSpecification}
 * subtype is enumerated explicitly. Missing a case becomes a compile-time error rather than a
 * silent identity fall-through (which would leave inner {@link LambdaFunction} bodies
 * un-rewritten).
 * <p>
 * Coverage:
 * <ul>
 *   <li><b>Recursed / rewritten</b> — {@link Variable}, {@link AppliedProperty},
 *       {@link AppliedFunction}, {@link Collection}, {@link LambdaFunction} (shadow-aware).</li>
 *   <li><b>Explicitly unsupported (throws {@link EngineException})</b> —
 *       {@link KeyExpression} ({@code ^Type{k=v}} new-instance syntax); {@link ClassInstance}
 *       when it wraps a {@link ColSpec}/{@link ColSpecArray} ({@code ~[…]} colspec),
 *       a {@link Path} ({@code #/…/#}), or a {@link GraphFetchTree} ({@code #{…}#}).
 *       These constructs can carry nested variable references and lambdas but are not
 *       supported inside a Boolean ModelJoin join condition today.</li>
 *   <li><b>Identity</b> — primitives ({@code CString}, {@code CInteger}, …),
 *       {@link EnumValue}, {@link UnitInstance}, {@link GenericTypeInstance},
 *       {@link PackageableElementPtr}, {@link UnitType}, other {@link ClassInstance}
 *       variants, and all deprecated leaves.</li>
 * </ul>
 * <p>
 * Shadowing: if an inner lambda declares a parameter with the same name as
 * {@code srcName} or {@code tgtName}, references to that name inside the body refer to
 * the inner parameter and must <b>not</b> be rewritten. Implemented by passing {@code null}
 * for the shadowed name into the nested visitor; {@link #mapVariableName} is null-safe.
 */
class ModelJoinLambdaRewriteVisitor implements ValueSpecificationVisitor<ValueSpecification>
{
    private final String srcName;
    private final String tgtName;

    ModelJoinLambdaRewriteVisitor(String srcName, String tgtName)
    {
        this.srcName = srcName;
        this.tgtName = tgtName;
    }

    // =========================================================================
    // Rewriting arms — recurse into children.
    // =========================================================================

    @Override
    public ValueSpecification visit(Variable variable)
    {
        String mapped = mapVariableName(variable.name);
        if (mapped == null)
        {
            return variable;
        }
        Variable result = new Variable();
        result.name = mapped;
        result.sourceInformation = variable.sourceInformation;
        return result;
    }

    @Override
    public ValueSpecification visit(AppliedProperty ap)
    {
        AppliedProperty result = new AppliedProperty();
        result.property = ap.property;
        result.sourceInformation = ap.sourceInformation;
        if (ap.parameters == null)
        {
            return result;
        }
        // Rewrite each parameter. If the first is a bare Variable matching src/tgt, substitute
        // the sentinel directly (equivalent to but skipping a re-dispatch through visit(Variable)).
        // Any tail parameters — unusual on AppliedProperty but syntactically possible — are also
        // recursively rewritten, so nested variable references never get silently dropped.
        MutableList<ValueSpecification> newParams = Lists.mutable.withInitialCapacity(ap.parameters.size());
        for (int i = 0; i < ap.parameters.size(); i++)
        {
            ValueSpecification p = ap.parameters.get(i);
            if (i == 0)
            {
                String matchedVar = matchBaseToVariable(p);
                if (matchedVar != null)
                {
                    newParams.add(newSentinelVariable(matchedVar, p.sourceInformation));
                    continue;
                }
            }
            newParams.add(p.accept(this));
        }
        result.parameters = newParams;
        return result;
    }

    @Override
    public ValueSpecification visit(AppliedFunction af)
    {
        AppliedFunction result = new AppliedFunction();
        result.function = af.function;
        result.fControl = af.fControl;
        result.sourceInformation = af.sourceInformation;
        result.parameters = af.parameters != null
                ? ListIterate.collect(af.parameters, p -> p.accept(this))
                : null;
        return result;
    }

    @Override
    public ValueSpecification visit(Collection collection)
    {
        Collection result = new Collection();
        result.sourceInformation = collection.sourceInformation;
        result.multiplicity = collection.multiplicity;
        result.values = collection.values != null
                ? ListIterate.collect(collection.values, p -> p.accept(this))
                : null;
        return result;
    }

    @Override
    public ValueSpecification visit(LambdaFunction lambda)
    {
        // Shadowing: if this inner lambda re-binds a name that matches srcName/tgtName,
        // the outer sentinel must not leak into its body — pass null for the shadowed side.
        String innerSrc = srcName;
        String innerTgt = tgtName;
        boolean shadowed = false;
        if (lambda.parameters != null)
        {
            for (Variable p : lambda.parameters)
            {
                if (innerSrc != null && innerSrc.equals(p.name))
                {
                    innerSrc = null;
                    shadowed = true;
                }
                if (innerTgt != null && innerTgt.equals(p.name))
                {
                    innerTgt = null;
                    shadowed = true;
                }
            }
        }
        ValueSpecificationVisitor<ValueSpecification> inner = shadowed
                ? new ModelJoinLambdaRewriteVisitor(innerSrc, innerTgt)
                : this;
        LambdaFunction result = new LambdaFunction();
        result.parameters = lambda.parameters;
        result.sourceInformation = lambda.sourceInformation;
        result.body = lambda.body != null
                ? ListIterate.collect(lambda.body, p -> p.accept(inner))
                : null;
        return result;
    }

    // =========================================================================
    // Explicitly-unsupported arms — throw a clear COMPILATION error.
    // =========================================================================

    @Override
    public ValueSpecification visit(KeyExpression keyExpression)
    {
        throw unsupported(keyExpression, "KeyExpression (^Type{key = value} new-instance syntax)");
    }

    @Override
    public ValueSpecification visit(ClassInstance iv)
    {
        if (iv.value instanceof ColSpec || iv.value instanceof ColSpecArray)
        {
            throw unsupported(iv, "ColSpec / ColSpecArray (~[…] column-spec constructs, e.g. inside ->project / ->extend / ->groupBy)");
        }
        if (iv.value instanceof Path)
        {
            throw unsupported(iv, "Path (#/…/# path expressions)");
        }
        if (iv.value instanceof GraphFetchTree)
        {
            throw unsupported(iv, "GraphFetchTree (#{…}# graph-fetch tree expressions)");
        }
        // Other ClassInstance variants (Pair, AggregateValue, TDS*, RelationStoreAccessor,
        // PureList, RuntimeInstance, ExecutionContextInstance, SerializationConfig, and
        // extension-provided types) are left as identity — they don't hold user-referenced
        // variables in the shapes that appear inside a Boolean join condition.
        return iv;
    }

    // =========================================================================
    // Identity arms — safe leaves with no nested variable references. Kept explicit
    // (rather than a base fall-through) so the visitor contract stays exhaustive and
    // any new subtype added upstream forces a compile-time decision.
    // =========================================================================

    @Override
    public ValueSpecification visit(ValueSpecification valueSpecification)
    {
        return valueSpecification;
    }

    @Override
    public ValueSpecification visit(PackageableElementPtr packageableElementPtr)
    {
        return packageableElementPtr;
    }

    @Override
    public ValueSpecification visit(CString cString)
    {
        return cString;
    }

    @Override
    public ValueSpecification visit(CDateTime cDateTime)
    {
        return cDateTime;
    }

    @Override
    public ValueSpecification visit(CLatestDate cLatestDate)
    {
        return cLatestDate;
    }

    @Override
    public ValueSpecification visit(CStrictDate cStrictDate)
    {
        return cStrictDate;
    }

    @Override
    public ValueSpecification visit(CStrictTime cStrictTime)
    {
        return cStrictTime;
    }

    @Override
    public ValueSpecification visit(CBoolean cBoolean)
    {
        return cBoolean;
    }

    @Override
    public ValueSpecification visit(EnumValue enumValue)
    {
        return enumValue;
    }

    @Override
    public ValueSpecification visit(CInteger cInteger)
    {
        return cInteger;
    }

    @Override
    public ValueSpecification visit(CDecimal cDecimal)
    {
        return cDecimal;
    }

    @Override
    public ValueSpecification visit(CByteArray cByteArray)
    {
        return cByteArray;
    }

    @Override
    public ValueSpecification visit(CFloat cFloat)
    {
        return cFloat;
    }

    @Override
    public ValueSpecification visit(GenericTypeInstance genericTypeInstance)
    {
        return genericTypeInstance;
    }

    @Override
    public ValueSpecification visit(UnitInstance unitInstance)
    {
        return unitInstance;
    }

    @Override
    public ValueSpecification visit(UnitType unitType)
    {
        return unitType;
    }

    // ---- Deprecated / legacy value-spec kinds — identity (no expected occurrence). ----

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(HackedUnit hackedUnit)
    {
        return hackedUnit;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return appliedQualifiedProperty;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(PrimitiveType primitiveType)
    {
        return primitiveType;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(Whatever whatever)
    {
        return whatever;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(MappingInstance mappingInstance)
    {
        return mappingInstance;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class aClass)
    {
        return aClass;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return unknownAppliedFunction;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueSpecification visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum anEnum)
    {
        return anEnum;
    }

    // =========================================================================
    // Private helpers.
    // =========================================================================

    /**
     * Returns the sentinel name if {@code varName} matches {@code srcName} / {@code tgtName};
     * otherwise {@code null}. Null-safe on both {@code srcName} and {@code tgtName} to support
     * lambda-parameter shadowing (a shadowed name is passed in as null).
     */
    private String mapVariableName(String varName)
    {
        if (srcName != null && srcName.equals(varName))
        {
            return HelperMappingBuilder.MODEL_JOIN_SOURCE_VAR;
        }
        if (tgtName != null && tgtName.equals(varName))
        {
            return HelperMappingBuilder.MODEL_JOIN_TARGET_VAR;
        }
        return null;
    }

    /**
     * If {@code base} is a {@link Variable} whose name maps to a sentinel, returns the
     * sentinel; otherwise {@code null}.
     */
    private String matchBaseToVariable(ValueSpecification base)
    {
        if (base instanceof Variable)
        {
            return mapVariableName(((Variable) base).name);
        }
        return null;
    }

    private static Variable newSentinelVariable(String varName, SourceInformation sourceInformation)
    {
        Variable variable = new Variable();
        variable.name = varName;
        variable.sourceInformation = sourceInformation;
        return variable;
    }

    private static EngineException unsupported(ValueSpecification vs, String constructDescription)
    {
        return new EngineException(
                "ModelJoin join condition does not support " + constructDescription
                        + ". Rewrite the condition using property access, function calls, collections, and lambdas.",
                vs.sourceInformation,
                EngineErrorType.COMPILATION);
    }
}



