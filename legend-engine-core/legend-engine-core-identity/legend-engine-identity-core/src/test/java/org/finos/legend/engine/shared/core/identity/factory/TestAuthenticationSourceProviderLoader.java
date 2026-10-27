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

package org.finos.legend.engine.shared.core.identity.factory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestAuthenticationSourceProviderLoader
{
    private static final String FUTURE_JVM_PROVIDER = "org.finos.legend.engine.shared.core.identity.factory.FutureJvmAuthenticationSourceProvider";

    @TempDir
    Path classpathRoot;

    @Test
    public void providersThatFailToLoadAreSkippedAndTheRestAreReturned() throws Exception
    {
        writeClassCompiledForAFutureJvm(FUTURE_JVM_PROVIDER);
        writeServicesFile(
                "org.finos.legend.engine.shared.core.identity.factory.DoesNotExist",
                FUTURE_JVM_PROVIDER,
                ThrowingAuthenticationSourceProvider.class.getName(),
                LoadableAuthenticationSourceProvider.class.getName());

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classpathRoot.toUri().toURL()}, getClass().getClassLoader()))
        {
            List<AuthenticationSourceProvider> providers = AuthenticationSourceProviderLoader.load(classLoader);

            Assertions.assertEquals(
                    Arrays.asList(LoadableAuthenticationSourceProvider.class),
                    providers.stream().map(Object::getClass).collect(Collectors.toList()));
        }
    }

    @Test
    public void noRegisteredProvidersLoadsAnEmptyList() throws Exception
    {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classpathRoot.toUri().toURL()}, null))
        {
            Assertions.assertEquals(0, AuthenticationSourceProviderLoader.load(classLoader).size());
        }
    }

    private void writeServicesFile(String... providerClassNames) throws Exception
    {
        Path services = classpathRoot.resolve("META-INF/services/" + AuthenticationSourceProvider.class.getName());
        Files.createDirectories(services.getParent());
        Files.write(services, String.join("\n", providerClassNames).getBytes(StandardCharsets.UTF_8));
    }

    private void writeClassCompiledForAFutureJvm(String className) throws Exception
    {
        byte[] bytes;
        try (InputStream in = LoadableAuthenticationSourceProvider.class.getResourceAsStream("LoadableAuthenticationSourceProvider.class"))
        {
            bytes = readAll(in);
        }
        bytes[6] = (byte) 0xFF;
        bytes[7] = (byte) 0xFF;
        Path classFile = classpathRoot.resolve(className.replace('.', '/') + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, bytes);
    }

    private static byte[] readAll(InputStream in) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int read = in.read(buffer); read != -1; read = in.read(buffer))
        {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
