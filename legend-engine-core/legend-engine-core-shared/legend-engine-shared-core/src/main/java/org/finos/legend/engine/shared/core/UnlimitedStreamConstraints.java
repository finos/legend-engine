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

package org.finos.legend.engine.shared.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;

/**
 * Lifts Jackson's stream limits, but only on jackson-core 2.18 or later, the first version with every constraint API
 * used here (2.15 added the read constraints, 2.16 the write constraints and the name and document length limits, 2.18
 * the token count limit). On an older Jackson both methods do nothing. That case is real: Legend model projects' generated
 * tests run engine code with the jackson-databind their entities pom pins (2.10.5 for every project structure up to
 * 13). Before 2.15 there is nothing to lift; on 2.15 to 2.17 Jackson's default limits stay in place. The constraint
 * classes are only touched through {@link Limits}, so they are never loaded on an older Jackson.
 */
final class UnlimitedStreamConstraints
{
    private static final boolean JACKSON_HAS_ALL_STREAM_CONSTRAINTS = isAtLeast(PackageVersion.VERSION, 2, 18);

    private UnlimitedStreamConstraints()
    {
    }

    static void applyTo(JsonFactory factory)
    {
        if (JACKSON_HAS_ALL_STREAM_CONSTRAINTS)
        {
            Limits.applyTo(factory);
        }
    }

    static void applyAsJvmDefaults()
    {
        if (JACKSON_HAS_ALL_STREAM_CONSTRAINTS)
        {
            Limits.applyAsJvmDefaults();
        }
    }

    // PackageVersion.VERSION is the jackson-core on the classpath (what JsonFactory.version() returns)
    private static boolean isAtLeast(Version version, int major, int minor)
    {
        return (version.getMajorVersion() > major) || ((version.getMajorVersion() == major) && (version.getMinorVersion() >= minor));
    }

    private static final class Limits
    {
        private static final StreamReadConstraints READ = StreamReadConstraints.builder()
                .maxNestingDepth(Integer.MAX_VALUE)
                .maxDocumentLength(-1L)
                .maxTokenCount(-1L)
                .maxNumberLength(Integer.MAX_VALUE)
                .maxStringLength(Integer.MAX_VALUE)
                .maxNameLength(Integer.MAX_VALUE)
                .build();
        private static final StreamWriteConstraints WRITE = StreamWriteConstraints.builder()
                .maxNestingDepth(Integer.MAX_VALUE)
                .build();

        private static void applyTo(JsonFactory factory)
        {
            factory.setStreamReadConstraints(READ);
            factory.setStreamWriteConstraints(WRITE);
        }

        private static void applyAsJvmDefaults()
        {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(READ);
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(WRITE);
        }
    }
}
