// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.persistence.extension;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.output.ServiceOutputTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTestBatch;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.protocol.pure.dsl.path.valuespecification.constant.classInstance.Path;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.persistence.assertion.PersistenceTestAssertionEvaluator;
import org.finos.legend.engine.testable.persistence.exception.PersistenceException;
import org.finos.legend.engine.testable.persistence.mapper.v1.DatasetMapper;
import org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class PersistenceTestRunner implements TestRunner
{
    public static final boolean CLEAN_STAGING_DATA_DEFAULT = true;
    public static final boolean STATS_COLLECTION_DEFAULT = true;
    public static final boolean SCHEMA_EVOLUTION_DEFAULT = false;

    private Root_meta_pure_persistence_metamodel_Persistence purePersistence;
    private PlanExecutor planExecutor;
    private String pureVersion;

    public PersistenceTestRunner(Root_meta_pure_persistence_metamodel_Persistence purePersistence, String pureVersion)
    {
        this.purePersistence = purePersistence;
        this.planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        this.pureVersion = pureVersion;
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        TestResult result;
        Persistence persistence = ListIterate.detect(data.getElementsOfType(Persistence.class), ele -> ele.getPath().equals(getElementFullPath(purePersistence, pureModel.getExecutionSupport())));
        PersistenceTest persistenceTest = persistence.tests.stream().filter(test -> test.id.equals(atomicTest._id())).findFirst().get();
        Path graphFetchPath = persistenceTest.graphFetchPath;
        List<PersistenceTestBatch> testBatches = persistenceTest.testBatches;

        PersistenceTestH2Connection persistenceTestH2Connection = new PersistenceTestH2Connection();
        Connection connection = persistenceTestH2Connection.getConnection();
        boolean isPersistenceSpecModelV1 = persistence.persister != null ? true : false;

        try
        {
            validatePersistenceSpec(persistence);
            List<AssertionStatus> assertStatuses = new ArrayList<>();
            Dataset targetDataset;
            Set<String> fieldsToIgnore;
            boolean isTransactionMilestoningTimeBased;
            ServiceOutputTarget serviceOutputTarget = null;

            // Test runner flow for v1
            if (isPersistenceSpecModelV1)
            {
                targetDataset = DatasetMapper.getTargetDataset(purePersistence);
                fieldsToIgnore = IngestModeMapper.getFieldsToIgnore(persistence);
                isTransactionMilestoningTimeBased = IngestModeMapper.isTransactionMilestoningTimeBased(persistence);
            }
            // Test runner flow for v2
            else
            {
                targetDataset = org.finos.legend.engine.testable.persistence.mapper.v2.DatasetMapper.getTargetDatasetV2(purePersistence, graphFetchPath);
                serviceOutputTarget = org.finos.legend.engine.testable.persistence.mapper.v2.IngestModeMapper.getServiceOutputTarget(persistence, graphFetchPath);
                fieldsToIgnore = org.finos.legend.engine.testable.persistence.mapper.v2.IngestModeMapper.getFieldsToIgnoreForComparison(serviceOutputTarget);
                isTransactionMilestoningTimeBased = org.finos.legend.engine.testable.persistence.mapper.v2.IngestModeMapper.isTransactionMilestoningTimeBased(serviceOutputTarget);

            }
            DatasetDefinition datasetDefinition = (DatasetDefinition) targetDataset;
            if (!(persistenceTest.isTestDataFromServiceOutput))
            {
                throw new UnsupportedOperationException(String.format("Persistence Test %s " +
                        "isTestDataFromServiceOutput = %s is not supported", atomicTest._id(), persistenceTest.isTestDataFromServiceOutput));
            }

            // Loop over each testBatch
            int batchId = 0;
            for (PersistenceTestBatch testBatch : testBatches)
            {
                for (TestAssertion testAssertion : testBatch.assertions)
                {
                    AssertionStatus batchAssertionStatus = new AssertPass();
                    if (testAssertion != null)
                    {
                        // Retrieve testData
                        String testDataString = getConnectionTestData(testBatch.testData);

                        // Test runner flow for v2
                        if (!isPersistenceSpecModelV1)
                        {
                            invokePersistence(targetDataset, serviceOutputTarget, testDataString, connection);
                        }
                        // Test runner flow for v1
                        else
                        {
                            invokePersistence(targetDataset, persistence, testDataString, connection);
                        }
                        List<Map<String, Object>> output = persistenceTestH2Connection.readTable(datasetDefinition);

                        batchAssertionStatus = testAssertion.accept(new PersistenceTestAssertionEvaluator(output, fieldsToIgnore));
                    }
                    assertStatuses.add(batchAssertionStatus);
                    if (isTransactionMilestoningTimeBased && ++batchId < testBatches.size())
                    {
                        // Sleep to avoid test batches having same IN_Z
                        Thread.sleep(1000);
                    }
                }
            }
            // Construct the Test Result
            result = constructTestResult(atomicTest._id(), persistence.getPath(), assertStatuses);
        }
        catch (Exception exception)
        {
            TestError testError = new TestError();
            testError.atomicTestId = atomicTest._id();
            testError.testSuiteId = "";
            testError.error = exception.getMessage();
            result = testError;
        }
        finally
        {
            persistenceTestH2Connection.closeConnection();
        }
        return result;
    }

    private void validatePersistenceSpec(Persistence persistence) throws PersistenceException
    {
        if ((persistence.persister != null && persistence.serviceOutputTargets != null && persistence.serviceOutputTargets.size() > 0)
                || (persistence.persister == null && persistence.serviceOutputTargets == null))
        {
            throw new PersistenceException("Persistence spec should contain either persister or serviceOutputTarget blocks ");
        }
    }

    private IngestorResult invokePersistence(Dataset targetDataset, Persistence persistence, String testData,
                                             Connection connection) throws Exception
    {
        Datasets enrichedDatasets = DatasetMapper.enrichAndDeriveDatasets(persistence, targetDataset, testData);
        IngestMode ingestMode = IngestModeMapper.from(persistence);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .cleanupStagingData(CLEAN_STAGING_DATA_DEFAULT)
                .collectStatistics(STATS_COLLECTION_DEFAULT)
                .enableSchemaEvolution(SCHEMA_EVOLUTION_DEFAULT)
                .build();

        IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(connection), enrichedDatasets).get(0);
        return result;
    }

    private IngestorResult invokePersistence(Dataset targetDataset, ServiceOutputTarget serviceOutputTarget, String testData,
                                             Connection connection) throws Exception
    {
        Datasets enrichedDatasets = org.finos.legend.engine.testable.persistence.mapper.v2.DatasetMapper.enrichAndDeriveDatasets(serviceOutputTarget, targetDataset, testData);
        IngestMode ingestMode = org.finos.legend.engine.testable.persistence.mapper.v2.IngestModeMapper.from(serviceOutputTarget);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .cleanupStagingData(CLEAN_STAGING_DATA_DEFAULT)
                .collectStatistics(STATS_COLLECTION_DEFAULT)
                .enableSchemaEvolution(SCHEMA_EVOLUTION_DEFAULT)
                .build();

        IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(connection), enrichedDatasets).get(0);
        return result;
    }

    private TestResult constructTestResult(String atomicTestId, String testable, List<AssertionStatus> assertionStatuses)
    {
        TestExecuted testResult = new TestExecuted(assertionStatuses);
        testResult.atomicTestId = atomicTestId;
        testResult.testSuiteId = "";
        testResult.testable = testable;
        return testResult;
    }

    private String getConnectionTestData(TestData testData)
    {
        ConnectionTestData conTestData = testData.connection;
        if (conTestData.data instanceof ExternalFormatData)
        {
            ExternalFormatData externalFormatData = (ExternalFormatData) conTestData.data;
            String testDataString = externalFormatData.data;
            return testDataString;
        }
        else
        {
            throw new UnsupportedOperationException("Non ExternalFormatData is not supported");
        }
    }

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("TestSuite is not supported for Persistence");
    }
}