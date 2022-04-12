// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.data.contentPattern;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPatternVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ContentPattern;

import java.util.Optional;

public class ContentPatternFirstPassBuilder implements ContentPatternVisitor<Root_meta_external_store_service_metamodel_data_ContentPattern>
{
    private final CompileContext context;
    private final ProcessingContext processingContext;

    public ContentPatternFirstPassBuilder(CompileContext context, ProcessingContext processingContext)
    {
        this.context = context;
        this.processingContext = processingContext;
    }

    @Override
    public Root_meta_external_store_service_metamodel_data_ContentPattern visit(ContentPattern contentPattern)
    {
        Optional<ContentPatternCompiler> compiler = ListIterate.detectOptional(ContentPatternCompilerExtensionLoader.extensions(), ext -> ext.supports(contentPattern));
        if (!compiler.isPresent())
        {
            throw new EngineException("No compiler found for content pattern for type - " + contentPattern.getClass().getSimpleName(), contentPattern.sourceInformation, EngineErrorType.COMPILATION);
        }

        return compiler.get().compile(contentPattern);
    }
}
