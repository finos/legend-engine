// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.postgres.utils;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.api.trace.Tracer;

public class OpenTelemetry
{
    private static final String LEGEND_ENGINE_XTS_SQL = "legend-engine-xts-sql";

    public static final LongUpDownCounter ACTIVE_SESSIONS = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .upDownCounterBuilder("active_sessions")
            .setDescription("Number of active sessions")
            .build();

    public static final LongCounter TOTAL_SESSIONS = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_sessions")
            .setDescription("Total of  sessions").build();


    public static final LongUpDownCounter ACTIVE_EXECUTE = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .upDownCounterBuilder("active_execute_request")
            .setDescription("Number of active execute requests")
            .build();


    public static final LongCounter TOTAL_EXECUTE = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_execute_requests")
            .setDescription("Total of execute requests").build();


    public static final LongCounter TOTAL_SUCCESS_EXECUTE = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_success_execute_requests")
            .setDescription("Total of success execute requests").build();


    public static final LongCounter TOTAL_FAILURE_EXECUTE = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_failure_execute_requests")
            .setDescription("Total of failure execute requests").build();


    public static final DoubleHistogram EXECUTE_DURATION = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .histogramBuilder("execute_requests_duration")
            .setDescription("Total of success execute requests")
            .build();


    public static final LongUpDownCounter ACTIVE_METADATA = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .upDownCounterBuilder("active_metadata_requests")
            .setDescription("Number of active metadata requests")
            .build();


    public static final LongCounter TOTAL_METADATA = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_metadata_requests")
            .setDescription("Total of metadata requests").build();


    public static final LongCounter TOTAL_SUCCESS_METADATA = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_success_metadata_requests")
            .setDescription("Total of success metadata requests")
            .build();

    public static final LongCounter TOTAL_FAILURE_METADATA = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .counterBuilder("total_failure_metadata_requests")
            .setDescription("Total of failure metadata requests")
            .build();

    public static final DoubleHistogram METADATA_DURATION = GlobalOpenTelemetry.get()
            .getMeter(LEGEND_ENGINE_XTS_SQL)
            .histogramBuilder("metadata_requests_duration")
            .setDescription(("Execute duration"))
            .build();

    public static Tracer getTracer()
    {
        return GlobalOpenTelemetry.getTracer(LEGEND_ENGINE_XTS_SQL);
    }

}
