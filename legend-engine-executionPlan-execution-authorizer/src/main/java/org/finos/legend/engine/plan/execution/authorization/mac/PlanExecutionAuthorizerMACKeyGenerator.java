// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.authorization.mac;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/*
    A simple command line tool to generate and write a key to file.
    Note : This is not a general purpose key generator. Use with caution.
 */
public class PlanExecutionAuthorizerMACKeyGenerator
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.out.println("Usage: PlanExecutionAuthorizerMACKeyGenerator <path to output key file>");
        }
        PlanExecutionAuthorizerMACKeyGenerator generator = new PlanExecutionAuthorizerMACKeyGenerator();
        Path outputKeyFile = Paths.get(args[0]);
        System.out.println("Generating key to file " + outputKeyFile.toFile().getAbsolutePath());
        generator.generateKey(outputKeyFile);
        System.out.println("Completed generating key");
    }

    public void generateKey(Path outputKeyFile) throws Exception
    {
        byte[] base64EncodedKey = this.generateKeyAsBytes();
        Files.write(outputKeyFile, base64EncodedKey, StandardOpenOption.CREATE_NEW);
    }

    public String generateKeyAsString() throws Exception
    {
        return new String(this.generateKeyAsBytes(), StandardCharsets.UTF_8);
    }

    public byte[] generateKeyAsBytes() throws Exception
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();

        byte[] base64EncodedKey = Base64.getEncoder().encode(secretKey.getEncoded());
        return base64EncodedKey;
    }
}
