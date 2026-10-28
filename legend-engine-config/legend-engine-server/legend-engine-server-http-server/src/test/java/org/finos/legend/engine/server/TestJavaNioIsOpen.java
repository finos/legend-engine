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

package org.finos.legend.engine.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJavaNioIsOpen
{
    private static final String ADD_OPENS_JAVA_NIO = "--add-opens=java.base/java.nio=ALL-UNNAMED";

    @Test
    public void theEngineImageStartsTheJvmWithJavaNioOpen() throws Exception
    {
        String pom = new String(Files.readAllBytes(Paths.get("pom.xml")), StandardCharsets.UTF_8);

        Assertions.assertTrue(pom.contains("<jvmFlag>" + ADD_OPENS_JAVA_NIO + "</jvmFlag>"), "the jib jvmFlags must pass " + ADD_OPENS_JAVA_NIO);
    }

    @Test
    public void theTestJvmHasJavaNioOpen()
    {
        Assertions.assertTrue(Object.class.getModule().isOpen("java.nio", TestJavaNioIsOpen.class.getModule()),
                "surefire.vm.params (or CI's JDK_JAVA_OPTIONS) must pass " + ADD_OPENS_JAVA_NIO);
    }
}
