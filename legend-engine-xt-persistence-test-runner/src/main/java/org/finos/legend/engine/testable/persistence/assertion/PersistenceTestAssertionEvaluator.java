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

package org.finos.legend.engine.testable.persistence.assertion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.MutablePair;
import org.finos.legend.engine.protocol.pure.v1.extension.TestAssertionEvaluator;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.ActiveRowsEquivalentToJson;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.AllRowsEquivalentToJson;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.status.ActiveRowsEquivalentToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.status.AllRowsEquivalentToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.testable.assertion.TestAssertionHelper;
import org.finos.legend.engine.testable.persistence.model.ActiveRowsFilterCondition;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Comparator;
import java.util.ArrayList;


public class PersistenceTestAssertionEvaluator implements TestAssertionEvaluator
{
    private List<Map<String, Object>> result;
    private Set<String> fieldsToIgnore;
    private ActiveRowsFilterCondition activeRowsFilterCondition;

    public PersistenceTestAssertionEvaluator(List<Map<String, Object>> result, Set<String> fieldsToIgnore, ActiveRowsFilterCondition activeRowsFilterCondition)
    {
        this.result = result;
        this.fieldsToIgnore = fieldsToIgnore;
        this.activeRowsFilterCondition = activeRowsFilterCondition;
    }

    @Override
    public AssertionStatus visit(TestAssertion testAssertion)
    {
        if (testAssertion instanceof AllRowsEquivalentToJson)
        {
            AllRowsEquivalentToJson allRowsEquivalentToJson = (AllRowsEquivalentToJson) testAssertion;
            ExternalFormatData externalFormatData = allRowsEquivalentToJson.expected;

            String expectedDataString = externalFormatData.data;
            AssertionStatus assertionStatus;
            ObjectMapper mapper = TestAssertionHelper.buildObjectMapperForJSONComparison();
            String actualResult = "";

            try
            {
                actualResult = mapper.writeValueAsString(result);
                List<Map<String, Object>> expected = mapper.readValue(expectedDataString, new TypeReference<List<Map<String, Object>>>() {});
                compareAllRows(result, expected, fieldsToIgnore);
                assertionStatus = new AssertPass();

            }
            catch (AssertionError e)
            {
                AllRowsEquivalentToJsonAssertFail fail = new AllRowsEquivalentToJsonAssertFail();
                fail.expected = expectedDataString;
                fail.actual = actualResult;
                fail.message = e.getMessage();
                assertionStatus = fail;
            }
            catch (Exception e)
            {
                AssertFail fail = new AssertFail();
                fail.message = e.getMessage();
                assertionStatus = fail;
            }
            assertionStatus.id = testAssertion.id;
            return assertionStatus;
        }
        else if (testAssertion instanceof ActiveRowsEquivalentToJson)
        {
            ActiveRowsEquivalentToJson activeRowsEquivalentToJson = (ActiveRowsEquivalentToJson) testAssertion;
            ExternalFormatData externalFormatData = activeRowsEquivalentToJson.expected;

            String expectedDataString = externalFormatData.data;
            AssertionStatus assertionStatus;
            ObjectMapper mapper = TestAssertionHelper.buildObjectMapperForJSONComparison();
            String actualResult = "";

            try
            {
                actualResult = mapper.writeValueAsString(result);
                List<Map<String, Object>> expected = mapper.readValue(expectedDataString, new TypeReference<List<Map<String, Object>>>() {});
                compareActiveRows(result, expected, fieldsToIgnore, activeRowsFilterCondition);
                assertionStatus = new AssertPass();

            }
            catch (AssertionError e)
            {
                ActiveRowsEquivalentToJsonAssertFail fail = new ActiveRowsEquivalentToJsonAssertFail();
                fail.expected = expectedDataString;
                fail.actual = actualResult;
                fail.message = e.getMessage();
                assertionStatus = fail;
            }
            catch (Exception e)
            {
                AssertFail fail = new AssertFail();
                fail.message = e.getMessage();
                assertionStatus = fail;
            }
            assertionStatus.id = testAssertion.id;
            return assertionStatus;
        }
        else
        {
            throw new UnsupportedOperationException("Only AllRowsEquivalentToJson and ActiveRowsEquivalentToJson Supported");
        }
    }

    private boolean compareAllRows(List<Map<String, Object>> result, List<Map<String, Object>> expected, Set<String> fieldsToIgnore)
    {
        return compareJsonObjects(result, expected, fieldsToIgnore);
    }

    private boolean compareActiveRows(List<Map<String, Object>> result, List<Map<String, Object>> expected, Set<String> fieldsToIgnore, ActiveRowsFilterCondition activeRowsFilterCondition)
    {
        result = findActiveRows(result, activeRowsFilterCondition);
        return compareJsonObjects(result, expected, fieldsToIgnore);
    }

    private List<Map<String, Object>> findActiveRows(List<Map<String, Object>> result, ActiveRowsFilterCondition activeRowsFilterCondition)
    {
        List<Map<String, Object>> activeRows = new ArrayList<>(result);
        for (Map<String, Object> row: result)
        {
            if (activeRowsFilterCondition != null && !row.get(activeRowsFilterCondition.getColumn()).toString().equals(activeRowsFilterCondition.getValue().toString()))
            {
                activeRows.remove(row);
            }
        }
        return activeRows;
    }

    private boolean compareJsonObjects(List<Map<String, Object>> result, List<Map<String, Object>> expected, Set<String> fieldsToIgnore)
    {
        if (result.size() != expected.size())
        {
            throw new AssertionError(String.format("AssertionError: Number of rows in results [%d] does not match number of rows in expected data [%d]", result.size(), expected.size()));
        }

        List<TreeMap<String, Object>> resultList = removeFieldsToIgnore(result, fieldsToIgnore);
        List<TreeMap<String, Object>> expectedList = removeFieldsToIgnore(expected, fieldsToIgnore);

        resultList.sort(Comparator.comparing(m -> m.hashCode()));
        expectedList.sort(Comparator.comparing(m -> m.hashCode()));

        Streams.zip(resultList.stream(), expectedList.stream(), MutablePair::new).forEach(row ->
        {
            TreeMap<String, Object> res = row.getLeft();
            TreeMap<String, Object> exp = row.getRight();

            if (res.size() != exp.size())
            {
                throw new AssertionError(String.format("AssertionError: Number of columns in results [%d] does not match number of columns in expected data [%d]", res.size(), exp.size()));
            }

            res.forEach((key, value) ->
            {
                Object expValue = exp.get(key);
                if (expValue == null)
                {
                    if (value != null)
                    {
                        throw new AssertionError("AssertionError: Results do not match the expected data");
                    }
                }
                else
                {
                    if (!expValue.equals(value))
                    {
                        throw new AssertionError("AssertionError: Results do not match the expected data");
                    }
                }
            });
        });
        return true;
    }

    private List<TreeMap<String, Object>> removeFieldsToIgnore(List<Map<String, Object>> result, Set<String> fieldsToIgnore)
    {
        List<TreeMap<String, Object>> filteredResult = new ArrayList<>();
        result.forEach(row ->
        {
            TreeMap<String, Object> filteredCols = new TreeMap<>();
            row.entrySet().forEach(col ->
            {
                if (!fieldsToIgnore.contains(col.getKey()))
                {
                    filteredCols.put(col.getKey(), col.getValue());
                }
            });
            filteredResult.add(filteredCols);
        });

        return filteredResult;
    }
}
