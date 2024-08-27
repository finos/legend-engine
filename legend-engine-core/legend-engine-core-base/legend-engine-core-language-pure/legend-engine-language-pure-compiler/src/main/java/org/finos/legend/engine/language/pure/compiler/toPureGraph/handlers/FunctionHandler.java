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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.Dispatch;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ResolveTypeParameterInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ReturnInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.type.Type;

import java.util.List;

public class FunctionHandler
{
    private final String _package;
    private final String fullName;
    private final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object> func;
    private final String functionSignature;
    private final String functionName;
    private final ReturnInference returnInference;
    private final ResolveTypeParameterInference resolvedTypeParametersInference;
    private Dispatch dispatch;
    private final int parametersSize;
    private PureModel pureModel;

    FunctionHandler(PureModel pureModel, String name, boolean isNative, ReturnInference returnInference)
    {
        this(pureModel, name, isNative, returnInference, p -> true);
    }

    FunctionHandler(PureModel pureModel, String name, boolean isNative, ReturnInference returnInference, Dispatch dispatch)
    {
        this(pureModel, name, pureModel.getFunction(name, isNative), returnInference, null, dispatch);
    }

    FunctionHandler(PureModel pureModel, String name, boolean isNative, ReturnInference returnInference, ResolveTypeParameterInference resolvedTypeParametersInference, Dispatch dispatch)
    {
        this(pureModel, name, pureModel.getFunction(name, isNative), returnInference, resolvedTypeParametersInference, dispatch);
    }

    public FunctionHandler(PureModel pureModel, String name, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object> func, ReturnInference returnInference, ResolveTypeParameterInference resolvedTypeParametersInference, Dispatch dispatch)
    {
        this.pureModel = pureModel;
        this._package = PackageableElement.getUserPathForPackageableElement(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) func)._package());
        this.func = func;
        this.functionSignature = func._name();
        this.fullName = name;
        this.functionName = func._functionName();
        this.returnInference = returnInference;
        this.dispatch = dispatch;
        this.parametersSize = ((FunctionType) func._classifierGenericType()._typeArguments().getAny()._rawType())._parameters().size();
        this.resolvedTypeParametersInference = resolvedTypeParametersInference;
    }

    public SimpleFunctionExpression process(List<ValueSpecification> vs, SourceInformation sourceInformation)
    {
        TypeAndMultiplicity inferred = null;
        try
        {
            inferred = returnInference.infer(vs);
        }
        catch (EngineException e)
        {
            e.mayUpdateSourceInformation(sourceInformation);
            throw e;
        }
        RichIterable<? extends GenericType> resolvedTypeParameters = resolvedTypeParametersInference == null ? Lists.mutable.empty() : resolvedTypeParametersInference.infer(vs);
        Assert.assertTrue(func != null, () -> "Func is null");
        return new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::valuespecification::SimpleFunctionExpression"))
                ._func(func)
                ._functionName(func._functionName())
                ._genericType(inferred.genericType)
                ._multiplicity(inferred.multiplicity)
                ._parametersValues(Lists.mutable.withAll(vs))
                ._resolvedTypeParameters(resolvedTypeParameters);
    }

    // TO DELETE!
    public String buildCode(PureModel pureModel)
    {
        RichIterable<? extends VariableExpression> vars = ((FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
        StringBuilder builder = new StringBuilder(functionName);
        builder.append(" ");
        for (VariableExpression v : vars)
        {
            builder.append(v._genericType()._rawType() != null && Type.subTypeOf(v._genericType()._rawType(), pureModel.getType("meta::pure::metamodel::function::Function"), pureModel.getExecutionSupport().getProcessorSupport()) ? "1" : "0");
        }
        return builder.toString();
    }

    public Dispatch getDispatch()
    {
        return dispatch;
    }

    public String getPackage()
    {
        return this._package;
    }

    public String getFunctionSignature()
    {
        return functionSignature;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public Function<? extends Object> getFunc()
    {
        return func;
    }

    public void setDispatch(Dispatch di)
    {
        this.dispatch = di;
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public int getParametersSize()
    {
        return this.parametersSize;
    }
}
