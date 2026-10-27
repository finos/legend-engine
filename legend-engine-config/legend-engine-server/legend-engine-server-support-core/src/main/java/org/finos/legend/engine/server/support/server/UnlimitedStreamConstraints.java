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

package org.finos.legend.engine.server.support.server;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Protocol payloads (deeply nested lambdas, whole-project models) have no size we can bound, so servers
 * lift every Jackson stream cap. This is set at server startup, not in ObjectMapperFactory, because
 * Pure Maven plugins run engine code against their own Jackson 2.10, which has no such API.
 */
public final class UnlimitedStreamConstraints
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

    private UnlimitedStreamConstraints()
    {
    }

    /**
     * A JsonFactory copies the JVM-wide defaults only when it is built, so a mapper that already exists,
     * such as Dropwizard's bootstrap mapper, gets the constraints directly.
     */
    public static void install(ObjectMapper alreadyBuilt)
    {
        StreamReadConstraints.overrideDefaultStreamReadConstraints(READ);
        StreamWriteConstraints.overrideDefaultStreamWriteConstraints(WRITE);
        alreadyBuilt.getFactory().setStreamReadConstraints(READ);
        alreadyBuilt.getFactory().setStreamWriteConstraints(WRITE);
    }
}
