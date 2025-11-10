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

package org.finos.legend.engine.postgres.protocol.wire;

import org.finos.legend.engine.postgres.PrometheusUserMetrics;

public class SessionStats
{
    public String name = ".oO Unknown Oo.";
    public String startTime;
    public String endTime;
    // Session counters
    public int preparedStatementCount = 0;
    public int statementCount = 0;
    public int errorCount = 0;
    // Global Prometheus counters for user
    private PrometheusUserMetrics prometheusUserMetrics;

    public void setPrometheusUserMetrics(PrometheusUserMetrics prometheusUserMetrics)
    {
        this.prometheusUserMetrics = prometheusUserMetrics;
    }

    public void incPreparedStatements()
    {
        this.preparedStatementCount++;
        if (this.prometheusUserMetrics != null)
        {
            this.prometheusUserMetrics.preparedStatements.labelValues(name).inc();
        }
    }

    public void incStatements()
    {
        this.statementCount++;
        if (this.prometheusUserMetrics != null)
        {
            this.prometheusUserMetrics.statements.labelValues(name).inc();
        }
    }

    public void incErrors()
    {
        this.errorCount++;
        if (this.prometheusUserMetrics != null)
        {
            this.prometheusUserMetrics.errors.labelValues(name).inc();
        }
    }
}
