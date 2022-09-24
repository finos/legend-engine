// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.service.utils;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CertGenerator
{
    public static class Certs
    {
        public String serverKeyStorePath;
        public String caKeyStorePath;
    }

    public Certs generateCerts() throws Exception
    {
        Path certsWorkDir = Files.createTempDirectory("tempcerts");
        copyFile("/certs", "certs.sh", certsWorkDir, true);
        copyFile("/certs", "cert.conf", certsWorkDir, false);
        copyFile("/certs", "csr.conf", certsWorkDir, false);

        System.out.println("Using cert working dir " + certsWorkDir.toAbsolutePath().toString());

        Path generatedWorkDir = certsWorkDir.resolve("generated");

        String certsSetupScript = certsWorkDir.resolve("certs.sh").toAbsolutePath().toString();
        ProcessBuilder command = new ProcessBuilder().command(
                FastList.newListWith(certsSetupScript, certsWorkDir.toAbsolutePath().toString())
        );

        Process process = command.start();
        InputStream errorStream = process.getErrorStream();
        InputStream inputStream = process.getInputStream();

        boolean success = process.waitFor(30, TimeUnit.SECONDS);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream));
        String errors = bufferedReader.lines().collect(Collectors.joining("\n"));
        System.out.println("+ Cert generation stderr : ");
        System.out.println(errors);

        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String inputs = bufferedReader.lines().collect(Collectors.joining("\n"));
        System.out.println("+ Cert generation stdout : ");
        System.out.println(inputs);

        if (!success)
        {
            throw new Exception("Failed to run cert setup script " + certsSetupScript);
        }

        Certs certs = new Certs();

        File serverKeyStoreFile = generatedWorkDir.resolve("serverkeystore.jks").toFile();
        if (!serverKeyStoreFile.exists())
        {
            throw new Exception("Cert setup not successful. File " + serverKeyStoreFile.toPath().toAbsolutePath() + " does not exist");
        }
        certs.serverKeyStorePath = serverKeyStoreFile.toPath().toAbsolutePath().toString();

        File caKeyStoreFile = generatedWorkDir.resolve("cakeystore.jks").toFile();
        if (!caKeyStoreFile.exists())
        {
            throw new Exception("Cert setup not successful. File " + caKeyStoreFile.toPath().toAbsolutePath() + " does not exist");
        }
        certs.caKeyStorePath = caKeyStoreFile.toPath().toAbsolutePath().toString();

        return certs;
    }

    private static void copyFile(String resourceDir, String fileName, Path targetDir, boolean makeExecutable) throws Exception
    {
        Path source = Paths.get(MTLSClientServerTest.class.getResource(resourceDir + "/" + fileName).toURI());
        Path target = targetDir.resolve(fileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("copied " + source.toAbsolutePath() + " to " + target.toAbsolutePath());
        if (makeExecutable)
        {
            target.toFile().setExecutable(true);
        }
    }
}
