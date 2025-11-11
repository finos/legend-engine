// Copyright 2025 Goldman Sachs
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

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

public class PrometheusUserMetrics
{
    public Counter connections;
    public Counter preparedStatements;
    public Counter statements;
    public Counter errors;
    public Gauge liveConnections;

    public PrometheusUserMetrics(PrometheusRegistry registry)
    {
        this.connections = Counter.builder().labelNames("user").name("connections").help("Global connection count for a user").register(registry);
        this.preparedStatements = Counter.builder().labelNames("user").name("preparedStatements").help("Global prepared statement creations for a user").register(registry);
        this.statements = Counter.builder().labelNames("user").name("statements").help("Global statement creations for a user").register(registry);
        this.errors = Counter.builder().labelNames("user").name("errors").help("Global error counts for a user").register(registry);
        this.liveConnections = Gauge.builder().labelNames("user").name("liveConnections").help("Live connections count for a user").register(registry);
    }
}
