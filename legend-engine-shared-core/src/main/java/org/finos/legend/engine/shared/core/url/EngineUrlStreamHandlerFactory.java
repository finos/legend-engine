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

package org.finos.legend.engine.shared.core.url;

import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

public class EngineUrlStreamHandlerFactory implements URLStreamHandlerFactory
{
    static final EngineUrlStreamHandlerFactory INSTANCE = new EngineUrlStreamHandlerFactory();

    private enum State
    {
        UNINITIALIZED,
        INITIALIZED,
        INITIALIZATION_FAILED
    }

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final List<String> JRE_PROTOCOLS = Arrays.asList("file", "ftp", "http", "https", "jar", "mailto");
    private static volatile State factoryInitialized = State.UNINITIALIZED;

    public static synchronized void initialize()
    {
        if (EngineUrlStreamHandlerFactory.factoryInitialized == State.UNINITIALIZED)
        {
            try
            {
                URL.setURLStreamHandlerFactory(INSTANCE);
                factoryInitialized = State.INITIALIZED;
                LOGGER.info(new LogInfo((String)null, LoggingEventType.URL_FACTORY_REGISTERED, "Registered Alloy URL Factory").toString());
            }
            catch (Error e)
            {
                factoryInitialized = State.INITIALIZATION_FAILED;
                LOGGER.info(new LogInfo((String)null, LoggingEventType.URL_FACTORY_REGISTER_FAILED, "Unable to register Alloy URL Factory.  URL creation via direct constructors will fail for Alloy protocols").toString());
            }
        }
    }

    private final Map<String, URLStreamHandler> handlers = new TreeMap<>();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        return JRE_PROTOCOLS.contains(protocol)
                ? null
                : handlers.computeIfAbsent(protocol, this::findForProtocol);
    }

    private URLStreamHandler findForProtocol(String protocol)
    {
        for (UrlProtocolHandler handler : ServiceLoader.load(UrlProtocolHandler.class))
        {
            if (handler.handles(protocol))
            {
                return handler.getHandler(protocol);
            }
        }
        return null;
    }
}
