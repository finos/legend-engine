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

package org.finos.legend.engine.connection;

import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig;
import net.mguenther.kafka.junit.KeyValue;
import net.mguenther.kafka.junit.ObserveKeyValues;
import net.mguenther.kafka.junit.SendKeyValues;
import net.mguenther.kafka.junit.TopicConfig;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.IdentityProvider;
import org.finos.legend.engine.connection.kafka.KafkaDatasourceSpecification;
import org.finos.legend.engine.spark.LegendSparkDataFrameConfigurator;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestSparkKafkaIntegration
{
    private static EmbeddedKafkaCluster KAFKA;

    public TestSparkKafkaIntegration()
    {
        // some glue code for Spark/hadoop on windows
        System.setProperty("hadoop.home.dir", "D:\\ephrim-sw\\winutils\\winutils-master\\hadoop-3.0.0\\");
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        try
        {
            setupKafka();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assume.assumeTrue("Test skipped due to integration setup failures", false);
        }
    }

    private static void setupKafka()
    {
        KAFKA = EmbeddedKafkaCluster.provisionWith(EmbeddedKafkaClusterConfig.defaultClusterConfig());
        KAFKA.start();
    }

    @AfterClass
    public static void cleanup()
    {
        try
        {
            cleanupKafka();
        }
        catch (Exception e)
        {
            // best effort cleanup
        }
    }

    private static void cleanupKafka()
    {
        if (KAFKA == null)
        {
            return;
        }
        KAFKA.close();
    }

    public void publishToKafka() throws Exception
    {
        KAFKA.createTopic(TopicConfig.withName("test-topic").useDefaults());
        KAFKA.send(SendKeyValues.to("test-topic", Lists.immutable.of(
                new KeyValue<>("key1","{\"msg\":\"m1\"}}"),
                new KeyValue<>("key2","{\"msg\":\"m2\"}}")
        ).castToList()));

        KAFKA.observe(ObserveKeyValues.on("test-topic", 2));
    }

    @Test
    public void testSparkKafka() throws Exception
    {
        this.publishToKafka();

        // 1 - create a Kafka connection spec
        ConnectionSpecification kafkaConnectionSpecification = new ConnectionSpecification(
                "legend::kafka::kafka1",
                new KafkaDatasourceSpecification(
                        KAFKA.getBrokerList(),
                        "test-topic",
                        "earliest",
                        "latest"
                ),
                null
        );

        ConnectionSpecificationProvider connectionSpecificationProvider = ConnectionSpecificationProvider.builder()
                .with(kafkaConnectionSpecification)
                .build();

        // 2 - create an identity provider

        IdentityProvider identityProvider = IdentityProvider.builder().build();

        // 3 - create a credential provider - This example does not make use of any credentials

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().build();

        // 4 - Initialize Spark

        SparkSession spark = SparkSession
                .builder()
                .appName("legend-spark-jdbc")
                .master("local[*]")
                .config("spark.sql.sources.disabledJdbcConnProviderList", "basic")
                .getOrCreate();

        DataFrameReader dataFrameReader = spark
                .read()
                .format("kafka");

        // 5 - Inject Legend

        dataFrameReader = new LegendSparkDataFrameConfigurator(connectionSpecificationProvider)
                .addResource(
                        "kafka",
                        kafkaConnectionSpecification.name,
                        dataFrameReader);

        // 6 - Run!

        List<String> dataset = dataFrameReader
                .load()
                .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
                .map((MapFunction<Row, String>) func -> func.getString(0) + "=" + func.getString(1), Encoders.STRING())
                        .collectAsList();

        assertEquals("[key1={\"msg\":\"m1\"}}, key2={\"msg\":\"m2\"}}]", dataset.toString());
    }
}
