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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;

/**
 * Converts JFR execution samples to collapsed stacks for demangle_pure_frames.py, when async-profiler
 * is not available. Streams the recording, so memory stays bounded by the number of distinct stacks.
 *
 * <pre>
 * java -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:FlightRecorderOptions:stackdepth=2048 \
 *      -XX:StartFlightRecording:method-profiling=max,filename=run.jfr ... PipelineBench ...
 * java -Xmx1g scripts/JfrToCollapsed.java run.jfr main.collapsed [--thread main] [--root frame-substring]
 * </pre>
 *
 * --root keeps only samples with a frame containing the substring and cuts each stack above the
 * outermost such frame, so the report covers one phase (for example Root_meta_pure_router_routeFunction).
 * Without -XX:+DebugNonSafepoints JFR attributes samples to the wrong leaf frames, and with the default
 * stack depth of 64 the deep recursion of generated Pure code loses its root frames.
 */
public class JfrToCollapsed
{
    public static void main(String[] args) throws IOException
    {
        Path jfr = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        String thread = "main";
        String root = null;
        for (int i = 2; i < args.length; i += 2)
        {
            if ("--thread".equals(args[i]))
            {
                thread = args[i + 1];
            }
            else if ("--root".equals(args[i]))
            {
                root = args[i + 1];
            }
            else
            {
                throw new IllegalArgumentException("Unknown option " + args[i]);
            }
        }

        Map<String, Integer> stacks = new HashMap<>();
        long total = 0;
        long kept = 0;
        long truncated = 0;
        try (RecordingFile recording = new RecordingFile(jfr))
        {
            while (recording.hasMoreEvents())
            {
                RecordedEvent event = recording.readEvent();
                if (!"jdk.ExecutionSample".equals(event.getEventType().getName()) || event.getStackTrace() == null)
                {
                    continue;
                }
                RecordedThread sampled = event.getThread("sampledThread");
                if (sampled == null || !thread.equals(sampled.getJavaName()))
                {
                    continue;
                }
                total++;
                if (event.getStackTrace().isTruncated())
                {
                    truncated++;
                }
                List<RecordedFrame> frames = event.getStackTrace().getFrames();
                int start = frames.size() - 1;
                if (root != null)
                {
                    start = -1;
                    for (int i = frames.size() - 1; i >= 0; i--)
                    {
                        if (frameName(frames.get(i)).contains(root))
                        {
                            start = i;
                            break;
                        }
                    }
                    if (start < 0)
                    {
                        continue;
                    }
                }
                StringBuilder stack = new StringBuilder();
                for (int i = start; i >= 0; i--)
                {
                    if (stack.length() > 0)
                    {
                        stack.append(';');
                    }
                    stack.append(frameName(frames.get(i)));
                }
                stacks.merge(stack.toString(), 1, Integer::sum);
                kept++;
            }
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out)))
        {
            stacks.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> writer.println(e.getKey() + " " + e.getValue()));
        }
        System.err.println(total + " samples on thread " + thread + ", " + kept + " kept, " + truncated + " truncated stacks");
    }

    private static String frameName(RecordedFrame frame)
    {
        return frame.getMethod().getType().getName().replace('.', '/') + "." + frame.getMethod().getName();
    }
}
