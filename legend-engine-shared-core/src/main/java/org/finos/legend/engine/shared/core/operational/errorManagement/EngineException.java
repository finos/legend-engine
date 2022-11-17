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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;

public class EngineException extends RuntimeException
{
    private EngineErrorType errorType = null;
    private SourceInformation sourceInformation = SourceInformation.getUnknownSourceInformation();
    private ExceptionCategory errorCategory = ExceptionCategory.UNKNOWN_ERROR;

    public EngineException(String message)
    {
        super(message);
    }

    public EngineException(String message, ExceptionCategory errorCategory)
    {
        this(message);
        this.errorCategory = errorCategory;
    }

    public EngineException(String message, Exception cause)
    {
        super(message, cause);
        if (cause instanceof EngineException)
        {
            this.errorType = ((EngineException) cause).getErrorType();
            SourceInformation sourceInfo = ((EngineException) cause).getSourceInformation();
            if (sourceInfo != null)
            {
                this.sourceInformation = sourceInfo;
            }
        }
    }

    public EngineException(String message, Exception cause, ExceptionCategory errorCategory)
    {
        this(message, cause);
        this.errorCategory = errorCategory;
    }

    public EngineException(String message, SourceInformation sourceInformation, Throwable cause)
    {
        super(message, cause);
        this.sourceInformation = sourceInformation;
        if (cause instanceof EngineException)
        {
            this.errorType = ((EngineException) cause).getErrorType();
        }
    }

    public EngineException(String message, SourceInformation sourceInformation, Throwable cause, ExceptionCategory errorCategory)
    {
        this(message, sourceInformation, cause);
        this.errorCategory = errorCategory;
    }

    public EngineException(String message, EngineErrorType type)
    {
        super(message);
        this.errorType = type;
    }

    public EngineException(String message, SourceInformation sourceInformation, EngineErrorType type)
    {
        super(message);
        this.sourceInformation = sourceInformation;
        this.errorType = type;
    }

    public EngineException(String message, SourceInformation sourceInformation, EngineErrorType type, ExceptionCategory errorCategory)
    {
        this(message, sourceInformation, type);
        this.errorCategory = errorCategory;
    }

    public EngineException(String message, SourceInformation sourceInformation, EngineErrorType type, Throwable cause)
    {
        super(message, cause);
        this.sourceInformation = sourceInformation;
        this.errorType = type;
    }

    public EngineException(String message, SourceInformation sourceInformation, EngineErrorType type, Throwable cause, ExceptionCategory errorCategory)
    {
        this(message, sourceInformation, type, cause);
        this.errorCategory = errorCategory;
    }

    public EngineErrorType getErrorType()
    {
        return errorType;
    }

    public ExceptionCategory getErrorCategory()
    {
        return errorCategory;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public static EngineException findException(Throwable throwable)
    {
        for (Throwable t = throwable; t != null; t = t.getCause())
        {
            if (t instanceof EngineException)
            {
                return (EngineException) t;
            }
        }
        return null;
    }

    /**
     * Only used for testing, the backend should return just the error message.
     */
    public static String buildPrettyErrorMessage(String errorMessage, SourceInformation sourceInformation, EngineErrorType type)
    {
        return (type == null ? "" : type + " error") + (sourceInformation == SourceInformation.getUnknownSourceInformation() || sourceInformation == null ? "" : " at " + sourceInformation.getMessage() + "") + (errorMessage == null ? "" : ": " + errorMessage);
    }
}
