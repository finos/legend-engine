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

package org.finos.legend.engine.shared.core.operational;

import org.eclipse.collections.api.block.function.Function0;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class Assert
{
    public static void assertFalse(boolean val, Function0<String> text)
    {
        if (val)
        {
            throw new EngineException(text.value());
        }
    }

    public static void assertTrue(boolean val, Function0<String> text)
    {
        if (!val)
        {
            throw new EngineException(text.value());
        }
    }

    public static void assertTrue(boolean val, Function0<String> text, SourceInformation sourceInformation, EngineErrorType type)
    {
        if (!val)
        {
            throw new EngineException(text.value(), sourceInformation, type);
        }
    }

    public static void fail(Function0<String> text, Exception e)
    {
        throw new EngineException(text.value(), e);
    }

    public static void fail(Function0<String> text)
    {
        throw new EngineException(text.value());
    }
}
