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

package org.finos.legend.engine.perf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes the machine and JVM a run was measured on. The fingerprint is what a baseline entry is
 * keyed by: absolute durations plainly do not survive a change of machine, and ratios travel better
 * but not far enough to trust, so a run is only ever compared against an entry recorded here.
 */
public class Environment
{
    public static Map<String, Object> capture()
    {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("os", System.getProperty("os.name"));
        environment.put("osVersion", System.getProperty("os.version"));
        environment.put("arch", System.getProperty("os.arch"));
        environment.put("cpu", cpuModel());
        environment.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        environment.put("maxHeapMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        environment.put("javaVersion", System.getProperty("java.version"));
        environment.put("javaVendor", System.getProperty("java.vendor"));
        environment.put("jvmName", System.getProperty("java.vm.name"));
        environment.put("jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments().toString());
        environment.put("engineVersion", engineVersion());
        environment.put("gitCommit", gitCommit());
        environment.put("ci", System.getenv("CI") != null);
        return environment;
    }

    /**
     * The subset of the environment that must match before two runs' absolute timings may be
     * compared. Processor count and heap are included because both change scheduling and GC cost.
     */
    public static String fingerprint(Map<String, Object> environment)
    {
        return String.valueOf(environment.get("os")) + "|"
                + environment.get("arch") + "|"
                + environment.get("cpu") + "|"
                + environment.get("availableProcessors") + "|"
                + environment.get("maxHeapMb") + "|"
                + majorJavaVersion(String.valueOf(environment.get("javaVersion")));
    }

    private static String majorJavaVersion(String version)
    {
        int dot = version.indexOf('.');
        return dot > 0 ? version.substring(0, dot) : version;
    }

    private static String engineVersion()
    {
        String version = Environment.class.getPackage() == null ? null : Environment.class.getPackage().getImplementationVersion();
        return version == null ? "unknown" : version;
    }

    private static String cpuModel()
    {
        String os = System.getProperty("os.name", "");
        if (os.startsWith("Mac"))
        {
            return command("sysctl", "-n", "machdep.cpu.brand_string");
        }
        if (os.startsWith("Linux"))
        {
            // x86 exposes "model name"; arm64 does not, so fall back to lscpu and then to the
            // implementer/part identifiers, which at least separate one arm machine from another.
            String model = command("sh", "-c", "grep -m1 'model name' /proc/cpuinfo | cut -d: -f2");
            if (isKnown(model))
            {
                return model;
            }
            model = command("sh", "-c", "lscpu 2>/dev/null | grep -m1 'Model name' | cut -d: -f2");
            if (isKnown(model))
            {
                return model;
            }
            model = command("sh", "-c", "grep -m1 'CPU implementer' /proc/cpuinfo | cut -d: -f2");
            String part = command("sh", "-c", "grep -m1 'CPU part' /proc/cpuinfo | cut -d: -f2");
            return isKnown(model) ? "arm implementer " + model + " part " + part : "unknown";
        }
        return "unknown";
    }

    private static boolean isKnown(String value)
    {
        // Virtualised hosts often answer with a placeholder rather than leaving the field empty.
        return value != null && value.length() > 1 && !"unknown".equals(value);
    }

    private static String command(String... command)
    {
        try
        {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line = reader.readLine();
                process.waitFor();
                return line == null ? "unknown" : line.trim();
            }
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }

    private static String gitCommit()
    {
        String fromEnv = System.getenv("GITHUB_SHA");
        if (fromEnv != null && !fromEnv.isEmpty())
        {
            return fromEnv;
        }
        return command("git", "rev-parse", "HEAD");
    }
}
