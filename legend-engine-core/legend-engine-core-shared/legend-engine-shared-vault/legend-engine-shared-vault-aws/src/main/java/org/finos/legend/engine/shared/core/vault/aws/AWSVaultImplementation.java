// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.shared.core.vault.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class AWSVaultImplementation implements VaultImplementation
{
    private final SecretsManagerClient client;
    private final String secretKey;

    private final Map<String, String> cache = ConcurrentHashMap.newMap();

    public AWSVaultImplementation(String accessKeyId, String secretAccessKey, Region region, String secretKey)
    {
        this.client = SecretsManagerClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(region)
                .build();
        this.secretKey = secretKey;
    }

    @Override
    public String getValue(String key)
    {
        return this.cache.computeIfAbsent(key, this::getValueFromAwsVault);
    }

    private String getValueFromAwsVault(String key)
    {
        GetSecretValueRequest.Builder requestBuilder = GetSecretValueRequest.builder().secretId(this.secretKey);
        GetSecretValueRequest getSecretValueRequest = requestBuilder.build();
        GetSecretValueResponse secretValue;
        try
        {
            secretValue = this.client.getSecretValue(getSecretValueRequest);
        }
        catch (ResourceNotFoundException e)
        {
            return null;
        }

        JsonNode val;
        try
        {
            JsonNode object = new ObjectMapper().readTree(secretValue.secretString());
            val = object.get(key);
            if (val == null)
            {
                throw new RuntimeException("The key " + key + " can't be found. Available ones: " + Lists.mutable.fromStream(StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(object.fieldNames(), Spliterator.ORDERED),
                        false)).makeString(", "));
            }
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
        return val.asText();
    }

    @Override
    public boolean hasValue(String key)
    {
        return this.getValue(key) != null;
    }
}
