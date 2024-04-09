// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;

public class PostgresServerLauncher
{
    private final String configPath;

    public PostgresServerLauncher(String configPath)
    {
        this.configPath = configPath;
    }

    public void launch() throws IOException
    {
        //TODO ADD CLI
        Injector injector = Guice.createInjector(new PostgresMainModule(configPath));
        injector.getInstance(PostgresServer.class).run();
    }

    public static void main(String[] args) throws IOException
    {
        String configPath = args[0];
        new PostgresServerLauncher(configPath).launch();
    }
}

