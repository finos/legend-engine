// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.shared.runtime.read;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.UrlStreamExternalSource;
import org.finos.legend.engine.shared.core.url.UrlFactory;

import java.io.IOException;
import java.io.InputStream;

public class ExecutionHelper
{
    public static InputStream inputStreamFromConnection(Connection connection)
    {
        if (connection instanceof ExternalFormatConnection)
        {
            return inputStreamFromExternalSource(((ExternalFormatConnection) connection).externalSource);
        }
        else
        {
            throw new IllegalStateException("Unsupported connection type for external formats: " + connection.getClass().getSimpleName());
        }
    }

    public static String locationFromConnection(Connection connection)
    {
        if (connection instanceof ExternalFormatConnection)
        {
            return locationFromExternalSource(((ExternalFormatConnection) connection).externalSource);
        }
        else
        {
            throw new IllegalStateException("Unsupported connection type for external formats: " + connection.getClass().getSimpleName());
        }
    }

    public static InputStream inputStreamFromExternalSource(ExternalSource source)
    {
        try
        {
            if (source instanceof UrlStreamExternalSource)
            {
                return UrlFactory.create(((UrlStreamExternalSource) source).url).openStream();
            }
            else
            {
                throw new IllegalStateException("Unsupported external source: " + source.getClass().getSimpleName());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String locationFromExternalSource(ExternalSource source)
    {
        if (source instanceof UrlStreamExternalSource)
        {
            return ((UrlStreamExternalSource) source).url;
        }
        else
        {
            throw new IllegalStateException("Unsupported external source: " + source.getClass().getSimpleName());
        }
    }
}
