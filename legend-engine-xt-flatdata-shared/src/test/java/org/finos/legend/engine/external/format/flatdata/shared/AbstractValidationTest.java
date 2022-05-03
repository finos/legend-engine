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

package org.finos.legend.engine.external.format.flatdata.shared;

import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidation;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidationResult;
import org.junit.Assert;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class AbstractValidationTest
{
    protected void test(String flatDataGrammar, String... expectedErrors)
    {
        FlatData flatData = new FlatDataSchemaParser(flatDataGrammar).parse();
        FlatDataValidationResult result = FlatDataValidation.validate(flatData);
        if (expectedErrors.length == 0)
        {
            if (!result.isValid())
            {
                Assert.fail("Result should be valid but has defects:\n" + result.getDefects().stream().map(Object::toString).collect(Collectors.joining("\n")));
            }
        }
        else
        {
            Assert.assertFalse("Result should be invalid", result.isValid());
            result.getDefects().forEach(d ->
                                        {
                                            if (!Arrays.asList(expectedErrors).contains(d.toString()))
                                            {
                                                Assert.fail("Unexpected defect: " + d);
                                            }
                                        });
            Arrays.asList(expectedErrors).forEach(e ->
                                                  {
                                                      if (result.getDefects().stream().noneMatch(d -> e.equals(d.toString())))
                                                      {
                                                          Assert.fail("Missing defect: " + e);
                                                      }
                                                  });
        }
    }
}
