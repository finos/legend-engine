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

package org.finos.legend.engine.test.emit.extensions;

import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.List;

/**
 * Test-only {@link ModelGenerationExtension} paired with
 * {@link EmitDemoModelGenerationCompilerExtension} to exercise
 * {@link org.finos.legend.engine.test.emit.EMITPhase#MODEL_GENERATION}.
 * For any Pure {@link Class} resolved by the companion compiler extension it
 * generates a sibling {@code <name>Generated} class so the test can assert
 * the merged PMCD picked up the new element and a recompile succeeded.
 */
public class EmitDemoModelGenerationExtension implements ModelGenerationExtension
{
    public static final String GENERATED_PACKAGE = "demo::modelgen::generated";
    public static final String GENERATED_SUFFIX = "Generated";

    @Override
    public List<Function3<PackageableElement, CompileContext, String, PureModelContextData>> getPureModelContextDataGenerators()
    {
        return Collections.singletonList((element, context, clientVersion) ->
        {
            if (!(element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class))
            {
                return null;
            }

            org.finos.legend.engine.protocol.pure.m3.type.Class cls = new org.finos.legend.engine.protocol.pure.m3.type.Class();
            cls.name = element._name() + GENERATED_SUFFIX;
            cls._package = GENERATED_PACKAGE;
            Property property = new Property();
            property.name = "generatedAt";
            property.multiplicity = Multiplicity.PURE_ONE;
            property.genericType = new GenericType(new PackageableType("String"));
            cls.properties = Collections.singletonList(property);
            return PureModelContextData.newBuilder()
                    .withElement(cls)
                    .build();
        });
    }
}
