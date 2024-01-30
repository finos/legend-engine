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

package org.finos.legend.engine.postgres.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendExecutionService;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendStaticClient;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticHandlerConfig implements HandlerConfig
{
    private String result;
    private String schema;
    private int delay;

    private StaticHandlerConfig()
    {
    }

    public StaticHandlerConfig(String result, String schema, int delay)
    {
        this.result = result;
        this.schema = schema;
        this.delay = delay;
    }

    public String getResult()
    {
        return result;
    }

    public String getSchema()
    {
        return schema;
    }

    public int getDelay()
    {
        return delay;
    }

    @Override
    public SessionsFactory buildSessionsFactory()
    {
        LegendStaticClient executionClient = new LegendStaticClient(getResult(), getSchema(), getDelay());
        LegendExecutionService client = new LegendExecutionService(executionClient);
        return new LegendSessionFactory(client);
    }

    @Override
    public String toString()
    {
        return "StaticHandlerConfig{" +
                "result='" + result + '\'' +
                ", schema='" + schema + '\'' +
                ", delay=" + delay +
                '}';
    }
}
