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

package org.finos.legend.engine.language.pure.dsl.text.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.text.Text;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_PackageableElement_Impl;

import java.util.Collections;

public class TextCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        // NOTE: we stub out since this element doesn't have an equivalent packageable element form in PURE metamodel
        return Collections.singletonList(Processor.newProcessor(Text.class, (text, context) -> new Root_meta_pure_metamodel_PackageableElement_Impl("")));
    }
}
