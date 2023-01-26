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

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.PlatformCredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.engine.IdentityProvider;
import org.finos.legend.engine.connection.jdbc.LegendPostgresConnectionProvider;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.PostgresTestContainerWrapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.spark.jdbc.LegendSparkJdbcConnectionProvider;
import org.finos.legend.engine.spark.jdbc.LegendSparkJdbcConnectionProviderExtension;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TestSparkJdbcIntegration
{
    private static PostgresTestContainerWrapper POSTGRES = null;
    private static Connection POSTGRES_CONNECTION;

    public TestSparkJdbcIntegration()
    {
        // some glue code for Spark/hadoop on windows
        System.setProperty("hadoop.home.dir", "D:\\ephrim-sw\\winutils\\winutils-master\\hadoop-3.0.0\\");
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        try
        {
            setupPostgress();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assume.assumeTrue("Test skipped due to integration setup failures", false);
        }
    }

    private static void setupPostgress() throws Exception
    {
        Class.forName("org.postgresql.Driver");
        POSTGRES = PostgresTestContainerWrapper.build();
        POSTGRES.start();
    }

    @AfterClass
    public static void cleanup()
    {
        try
        {
            cleanupPostgres();
        }
        catch (Exception e)
        {
            // best effort cleanup
        }
    }

    private static void cleanupPostgres()
    {
        if (POSTGRES == null)
        {
            return;
        }
        POSTGRES.stop();
    }

    public void setupDatabase() throws Exception
    {
        POSTGRES_CONNECTION = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUser(),
                POSTGRES.getPassword()
        );

        Statement stmt = POSTGRES_CONNECTION.createStatement();
        MutableList<String> setupSqls = Lists.mutable.with(
                "create schema legend"
                //"drop table if exists legend.PERSON;",
                //"create table legend.PERSON(firstName VARCHAR(200), lastName VARCHAR(200));"
        );
        for (String sql : setupSqls)
        {
            stmt.executeUpdate(sql);
        }
        stmt.close();
    }

    @Test
    public void testSparkJdbc() throws Exception
    {
        this.setupDatabase();

        // 1 - create a Postgres connection spec

        String connectionName = "legend::postgres::pg1";

        StaticDatasourceSpecification staticDatasourceSpecification = new StaticDatasourceSpecification();
        staticDatasourceSpecification.host = POSTGRES.postgreSQLContainer.getContainerIpAddress();
        staticDatasourceSpecification.port = POSTGRES.getPort();
        staticDatasourceSpecification.databaseName = "test";

        UserPasswordAuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification();
        authenticationSpecification.username = POSTGRES.getUser();
        authenticationSpecification.password = new PropertiesFileSecret(connectionName + "::password");

        ConnectionSpecification connectionSpecification = new ConnectionSpecification(
                connectionName,
                staticDatasourceSpecification,
                authenticationSpecification
        );

        ConnectionSpecificationProvider connectionSpecificationProvider = ConnectionSpecificationProvider.builder()
                .with(connectionSpecification)
                .build();

        // 2 - create an identity provider

        IdentityProvider identityProvider = IdentityProvider.builder().with(new Identity("alice")).build();

        // 3 - create a credential provider

        Properties properties = new Properties();
        properties.put(connectionName + "::password", POSTGRES.getPassword());
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(propertiesFileCredentialVault)
                .build();

        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                .build();

        UserPasswordCredentialProvider userPasswordCredentialProvider = new UserPasswordCredentialProvider(
                Lists.mutable.with(new UserPasswordFromVaultRule(credentialVaultProvider))
        );
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(userPasswordCredentialProvider)
                .build();

        // 4 - Initialize Spark
        SparkSession spark = SparkSession
                .builder()
                .appName("legend-spark-jdbc")
                .master("local[*]")
                .config("spark.sql.sources.disabledJdbcConnProviderList", "basic")
                .getOrCreate();

        // 5 - Inject Legend

        LegendSparkJdbcConnectionProvider legendSparkJdbcConnectionProvider = LegendSparkJdbcConnectionProvider.builder()
                .with(new LegendPostgresConnectionProvider(identityProvider, credentialProviderProvider, connectionSpecificationProvider))
                .build();
        LegendSparkJdbcConnectionProviderExtension.setDelegate(legendSparkJdbcConnectionProvider);

        // 6 - Run!

        ImmutableList<Person> dataToWrite = Lists.immutable.of(
                new Person("jane", "doe"),
                new Person("john", "doe")
        );

        Dataset<Person> dataset = spark.createDataset(dataToWrite.castToList(), Encoders.bean(Person.class));

        dataset.write()
                .format("jdbc")
                .option("url", POSTGRES.getJdbcUrl())
                .option("legend-connectionName", connectionName)
                .option("dbtable", "legend.PERSON")
                .save();

        ImmutableList<Person> dataFromDatabase = this.readFromDatabase();
        assertEquals(dataToWrite.toSortedList(), dataFromDatabase.toSortedList());
    }

    public ImmutableList<Person> readFromDatabase() throws SQLException
    {
        MutableList<Person> persons = Lists.mutable.empty();
        Statement statement = POSTGRES_CONNECTION.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from legend.PERSON");
        while (resultSet.next())
        {
            String firstName = resultSet.getString(1);
            String lastName = resultSet.getString(2);
            persons.add(new Person(firstName, lastName));
        }
        resultSet.close();
        statement.close();

        return persons.toImmutable();
    }

    public static class Person implements Serializable, Comparable<Person>
    {
        private String firstName;
        private String lastName;

        public Person(String firstName, String lastName)
        {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName()
        {
            return firstName;
        }

        public void setFirstName(String firstName)
        {
            this.firstName = firstName;
        }

        public String getLastName()
        {
            return lastName;
        }

        public void setLastName(String lastName)
        {
            this.lastName = lastName;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            Person person = (Person) o;
            return firstName.equals(person.firstName) && lastName.equals(person.lastName);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(firstName, lastName);
        }

        @Override
        public String toString()
        {
            return "Person{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    '}';
        }

        @Override
        public int compareTo(TestSparkJdbcIntegration.Person that)
        {
            String thisName = this.lastName + this.firstName;
            String thatName = that.lastName + that.firstName;
            return thisName.compareTo(thatName);
        }
    }
}
