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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.Dispatch;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ResolveTypeParameterInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ReturnInference;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.TypeAndMultiplicity;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.List;
import java.util.Map;

public class FunctionHandler
{
    private final MutableMap<String, Function<?>> fcache;
    private final PureModel pureModel;
    private final String fullName;
    private final String functionName;
    private final boolean isNative;
    private final ReturnInference returnInference;
    private final ResolveTypeParameterInference resolvedTypeParametersInference;
    private Dispatch dispatch;

    private volatile LazyState funcHandlerLazyState;

    FunctionHandler(PureModel pureModel, MutableMap<String, Function<?>> fcache, String fullName, String name, boolean isNative, ReturnInference returnInference)
    {
        this(pureModel, fcache, fullName, name, isNative, returnInference, p -> true);
    }

    FunctionHandler(PureModel pureModel, MutableMap<String, Function<?>> fcache, String fullName, String name, boolean isNative, ReturnInference returnInference, Dispatch dispatch)
    {
        this(pureModel, fcache, fullName, name, isNative, returnInference, null, dispatch);
    }

    public FunctionHandler(PureModel pureModel, MutableMap<String, Function<?>> fcache, String fullName, String name, boolean isNative, ReturnInference returnInference, ResolveTypeParameterInference resolvedTypeParametersInference, Dispatch dispatch)
    {
        this.pureModel = pureModel;
        this.fcache = fcache;
        this.fullName = fullName;
        this.isNative = isNative;
        this.returnInference = returnInference;
        this.resolvedTypeParametersInference = resolvedTypeParametersInference;
        this.dispatch = dispatch;
        this.functionName = name;
    }

    public void initialize()
    {
        getLazyState();
    }

    public void initialize(Function<?> func)
    {
        this.funcHandlerLazyState = (func == null) ? null : new LazyState(func);
    }

    public SimpleFunctionExpression process(List<ValueSpecification> vs, SourceInformation sourceInformation)
    {
        Function<?> func = getLazyState().getFunc();

        TypeAndMultiplicity inferred;
        try
        {
            inferred = this.returnInference.infer(vs);
        }
        catch (EngineException e)
        {
            e.mayUpdateSourceInformation(sourceInformation);
            throw e;
        }
        RichIterable<? extends GenericType> resolvedTypeParameters = this.resolvedTypeParametersInference == null ? Lists.mutable.empty() : this.resolvedTypeParametersInference.infer(vs);
        return new Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl("", null, this.pureModel.getClass("meta::pure::metamodel::valuespecification::SimpleFunctionExpression"))
                ._func(func)
                ._functionName(func._functionName())
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
        return getLazyState().getFunctionSignature();
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public Function<?> getFunc()
    {
        return getLazyState().getFunc();
    }

    public int getParametersSize()
    {
        return getLazyState().getParametersSize();
    }

    public void build(Map<String, Function<?>> result)
    {
        result.put(this.fullName, getFunc());
    }

    private LazyState getLazyState()
    {
        LazyState result = this.funcHandlerLazyState;
        if (result == null)
        {
            synchronized (this)
            {
                if ((result = this.funcHandlerLazyState) == null)
                {
                    Function<?> f = (this.fcache == null) ? this.pureModel.getFunction(this.fullName, this.isNative) : this.fcache.get(this.fullName);
                    if (f == null)
                    {
                        throw new IllegalStateException("Cannot find function: " + this.fullName);
                    }
                    this.funcHandlerLazyState = result = new LazyState(f);
                }
            }
        }
        return result;
    }

    private static class LazyState
    {
        private final Function<?> func;
        private volatile String functionSignature;
        private volatile int parametersSize = -1;

        private LazyState(Function<?> func)
        {
            this.func = func;
        }

        Function<?> getFunc()
        {
            return this.func;
        }

        String getFunctionSignature()
        {
            String result = this.functionSignature;
            if (result == null)
            {
                synchronized (this)
                {
                    if ((result = this.functionSignature) == null)
                    {
                        this.functionSignature = result = this.func._name();
                    }
                }
            }
            return result;
        }

        int getParametersSize()
        {
            int result = this.parametersSize;
            if (result == -1)
            {
                synchronized (this)
                {
                    if ((result = this.parametersSize) == -1)
                    {
                        this.parametersSize = result = ((FunctionType) this.func._classifierGenericType()._typeArguments().getAny()._rawType())._parameters().size();
                    }
                }
            }
            return result;
        }
    }
}
