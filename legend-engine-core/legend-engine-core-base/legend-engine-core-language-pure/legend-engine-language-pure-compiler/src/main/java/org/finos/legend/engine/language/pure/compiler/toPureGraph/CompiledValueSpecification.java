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

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;

/**
 * Compiler-internal protocol node standing for a value specification that is already compiled, so a
 * rewritten expression can embed it without compiling it again. It is never serialized.
 */
final class CompiledValueSpecification extends ValueSpecification
{
    final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification compiled;

    CompiledValueSpecification(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification compiled, SourceInformation sourceInformation)
    {
        this.compiled = compiled;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
