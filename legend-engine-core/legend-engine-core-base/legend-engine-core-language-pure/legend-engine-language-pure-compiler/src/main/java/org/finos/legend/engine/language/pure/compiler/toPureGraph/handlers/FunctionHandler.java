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
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
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

import java.util.Arrays;
import java.util.List;

public class FunctionHandler
{
    private PureModel pureModel;
    private final String fullName;
    private String functionName;
    private final boolean isNative;
    private final ReturnInference returnInference;
    private final ResolveTypeParameterInference resolvedTypeParametersInference;
    private Dispatch dispatch;

    private FuncHandlerLazyState funcHandlerLazyState;

    FunctionHandler(PureModel pureModel, String fullName, String name, boolean isNative, ReturnInference returnInference)
    {
        this(pureModel, fullName, name, isNative, returnInference, p -> true);
    }

    FunctionHandler(PureModel pureModel, String fullName, String name, boolean isNative, ReturnInference returnInference, Dispatch dispatch)
    {
        this(pureModel, fullName, name, isNative, returnInference, null, dispatch);
    }

    public FunctionHandler(PureModel pureModel, String fullName, String name, boolean isNative, ReturnInference returnInference, ResolveTypeParameterInference resolvedTypeParametersInference, Dispatch dispatch)
    {
        this.pureModel = pureModel;
        this.fullName = fullName;
        this.isNative = isNative;
        this.returnInference = returnInference;
        this.resolvedTypeParametersInference = resolvedTypeParametersInference;
        this.dispatch = dispatch;
        this.functionName = name;
    }

    public void initialize()
    {
        if (this.funcHandlerLazyState == null)
        {
            synchronized (this)
            {
                if (this.funcHandlerLazyState == null)
                {
                    initialize(pureModel.getFunction(fullName, isNative));
                    this.pureModel.loadModelFromFunctionHandler(this);
                }
            }
        }
    }

    public void initialize(Function<?> func)
    {
        this.funcHandlerLazyState = new FuncHandlerLazyState(func);
    }

    public SimpleFunctionExpression process(List<ValueSpecification> vs, SourceInformation sourceInformation)
    {
        initialize();

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
        Assert.assertTrue(this.funcHandlerLazyState.func != null, () -> "Func is null");
        return new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::valuespecification::SimpleFunctionExpression"))
                ._func(this.funcHandlerLazyState.func)
                ._functionName(this.funcHandlerLazyState.func._functionName())
                ._genericType(inferred.genericType)
                ._multiplicity(inferred.multiplicity)
                ._parametersValues(Lists.mutable.withAll(vs))
                ._resolvedTypeParameters(resolvedTypeParameters);
    }

    public Dispatch getDispatch()
    {
        return dispatch;
    }

    public void setDispatch(Dispatch di)
    {
        this.dispatch = di;
    }

    public String getFullName()
    {
        return this.fullName;
    }

    public String getFunctionSignature()
    {
        this.initialize();
        return this.funcHandlerLazyState.functionSignature;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public Function<? extends Object> getFunc()
    {
        this.initialize();
        return this.funcHandlerLazyState.func;
    }

    public int getParametersSize()
    {
        this.initialize();
        return this.funcHandlerLazyState.parametersSize;
    }
}
