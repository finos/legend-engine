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

package org.finos.legend.engine.test.emit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EMITPhaseResult
{
    public enum Status
    {
        SUCCESS,
        FAILURE,
        SKIPPED,
        NOT_RUN
    }

    private final EMITPhase phase;
    private final Status status;
    private final long durationMs;
    private final String message;
    private final Throwable throwable;
    private final List<?> outputs;

    private EMITPhaseResult(EMITPhase phase, Status status, long durationMs, String message, Throwable throwable, List<?> outputs)
    {
        this.phase = phase;
        this.status = status;
        this.durationMs = durationMs;
        this.message = message;
        this.throwable = throwable;
        this.outputs = outputs;
    }

    private EMITPhaseResult(EMITPhase phase, Status status, long durationMs, String message, Throwable exception)
    {
        this(phase, status, durationMs, message, exception, Collections.emptyList());
    }

    public EMITPhase getPhase()
    {
        return this.phase;
    }

    public Status getStatus()
    {
        return this.status;
    }

    public long getDurationMs()
    {
        return this.durationMs;
    }

    public String getMessage()
    {
        return this.message;
    }

    public Throwable getThrowable()
    {
        return this.throwable;
    }

    public List<?> getOutputs()
    {
        return this.outputs;
    }

    public static EMITPhaseResult success(EMITPhase phase, long durationMs, String message, List<?> outputs)
    {
        return new EMITPhaseResult(phase, Status.SUCCESS, durationMs, message, null, wrapList(outputs));
    }

    public static EMITPhaseResult success(EMITPhase phase, long durationMs, String message, Object... outputs)
    {
        return new EMITPhaseResult(phase, Status.SUCCESS, durationMs, message, null, wrapList(outputs));
    }

    public static EMITPhaseResult failure(EMITPhase phase, long durationMs, String message)
    {
        return failure(phase, durationMs, message, null);
    }

    public static EMITPhaseResult failure(EMITPhase phase, long durationMs, String message, Throwable throwable, Object... outputs)
    {
        return new EMITPhaseResult(phase, Status.FAILURE, durationMs, message, throwable, wrapList(outputs));
    }

    public static EMITPhaseResult failure(EMITPhase phase, long durationMs, String message, Throwable throwable, List<?> outputs)
    {
        return new EMITPhaseResult(phase, Status.FAILURE, durationMs, message, throwable, wrapList(outputs));
    }

    public static EMITPhaseResult skipped(EMITPhase phase, String reason)
    {
        return new EMITPhaseResult(phase, Status.SKIPPED, 0L, reason, null);
    }

    public static EMITPhaseResult notRun(EMITPhase phase, String reason)
    {
        return new EMITPhaseResult(phase, Status.NOT_RUN, 0L, reason, null);
    }

    public boolean isSuccess()
    {
        return this.status == Status.SUCCESS || this.status == Status.SKIPPED;
    }

    public boolean isFailure()
    {
        return this.status == Status.FAILURE;
    }

    private static List<?> wrapList(List<?> list)
    {
        return ((list == null) || list.isEmpty()) ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    private static List<?> wrapList(Object... objects)
    {
        return ((objects == null) || (objects.length == 0)) ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(objects));
    }
}
