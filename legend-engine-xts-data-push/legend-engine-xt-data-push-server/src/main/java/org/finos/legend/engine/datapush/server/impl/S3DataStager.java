// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.datapush.server.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.datapush.server.Data;
import org.finos.legend.engine.datapush.server.DataStager;
import org.finos.legend.engine.datapush.server.SQLData;
import org.finos.legend.engine.shared.core.identity.Identity;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class S3DataStager implements DataStager
{
    private final String s3Endpoint;
    private final AwsCredentialsProvider s3CredentialProvider;

    public S3DataStager(String s3Endpoint, AwsCredentialsProvider s3CredentialProvider)
    {
        this.s3Endpoint = s3Endpoint;
        this.s3CredentialProvider = s3CredentialProvider;
    }

    private static String generateBucketName(Identity identity)
    {
        return identity.getName().replaceAll("_", "").toLowerCase();
    }

    private static String generateObjectKey(Identity identity)
    {
        return identity.getName() + "__" + Instant.now() + "__" + UUID.randomUUID();
    }

    private S3Client getS3Client()
    {
        return S3Client
                .builder()
                .endpointOverride(URI.create(this.s3Endpoint))
                .credentialsProvider(this.s3CredentialProvider)
                .region(Region.of(this.s3Endpoint))
                .build();
    }

    @Override
    public String write(Identity identity, Data data)
    {
        SQLData sqlData = (SQLData) data;
        S3Client s3Client = this.getS3Client();
        String bucketName = generateBucketName(identity);

        // create the default bucket
        boolean bucketExists;
        try
        {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            bucketExists = true;
        }
        catch (NoSuchBucketException e)
        {
            bucketExists = false;
        }
        if (!bucketExists)
        {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }

        // put the object in the bucket
        String objectId = generateObjectKey(identity);
        try
        {
            s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(objectId).build(), RequestBody.fromString(new ObjectMapper().writeValueAsString(sqlData)));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return objectId;
    }

    @Override
    public Data read(Identity identity, String stagingRef)
    {
        S3Client s3Client = this.getS3Client();
        String bucketName = generateBucketName(identity);
        String content = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(stagingRef).build(), ResponseTransformer.toBytes()).asUtf8String();
        try
        {
            return new ObjectMapper().readValue(content, Data.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
