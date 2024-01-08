// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.autocomplete;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class CompilationResult
{
    private PureModel pureModel;
    private GenericType genericType;
    private Multiplicity multiplicity;

    public CompilationResult(PureModel pureModel, GenericType genericType, Multiplicity multiplicity)
    {
        this.pureModel = pureModel;
        this.genericType = genericType;
        this.multiplicity = multiplicity;
    }

    public PureModel getPureModel()
    {
        return this.pureModel;
    }

    public GenericType getGenericType()
    {
        return this.genericType;
    }

    public Multiplicity getMultiplicity()
    {
        return this.multiplicity;
    }
}
