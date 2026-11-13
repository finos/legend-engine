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

package org.finos.legend.engine.language.pure.dsl.generation.model;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

/**
 * A failure of a generation specification's run, with the generation node and the generation
 * element it happened in, so a caller can show it against them. The message, source information
 * and error type are those of the failure raised, which is kept as the cause.
 */
public class ModelGenerationException extends EngineException
{
    private final String generationNodeId;
    private final String generationElement;

    private ModelGenerationException(String message, SourceInformation sourceInformation, EngineErrorType errorType, Throwable cause, String generationNodeId, String generationElement)
    {
        super(message, sourceInformation, errorType, cause);
        this.generationNodeId = generationNodeId;
        this.generationElement = generationElement;
    }

    static ModelGenerationException wrap(RuntimeException cause, String generationNodeId, String generationElement)
    {
        if (cause instanceof ModelGenerationException)
        {
            return (ModelGenerationException) cause;
        }
        if (cause instanceof EngineException)
        {
            EngineException engineException = (EngineException) cause;
            return new ModelGenerationException(engineException.getMessage(), engineException.getSourceInformation(), engineException.getErrorType(), cause, generationNodeId, generationElement);
        }
        return new ModelGenerationException(cause.getMessage(), null, null, cause, generationNodeId, generationElement);
    }

    /**
     * The id of the generation node the failure happened in.
     */
    public String getGenerationNodeId()
    {
        return this.generationNodeId;
    }

    /**
     * The generation element being generated or merged when it failed, or null when the node's
     * output as a whole failed to compile.
     */
    public String getGenerationElement()
    {
        return this.generationElement;
    }
}
