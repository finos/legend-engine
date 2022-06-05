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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.List;
import java.util.Optional;

abstract class AbstractDataFactory<T>
{
    private final String definingPath;
    final List<String> nullStrings;

    AbstractDataFactory(String definingPath, List<String> nullStrings)
    {
        this.definingPath = definingPath;
        this.nullStrings = nullStrings;
    }

    public Optional<IChecked<T>> createParsed(IChecked<RawFlatData> unparsed, List<FieldHandler> fieldHandlers, ParsedFlatDataToObject<? extends T> objectFactory)
    {
        if (unparsed.getDefects().stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical))
        {
            return Optional.of(BasicChecked.newChecked(null, unparsed.getValue(), unparsed.getDefects()));
        }
        else if (unparsed.getValue() == null)
        {
            return Optional.empty();
        }
        else
        {
            RawFlatData rawData = unparsed.getValue();
            LazyParsedFlatData parseData = new LazyParsedFlatData(rawData, unparsed.getDefects(), fieldHandlers, definingPath);

            for (FieldHandler handler : fieldHandlers)
            {
                if (handler.hasRawValue(rawData))
                {
                    String errorMessage = handler.validate(rawData);
                    if (errorMessage == null)
                    {
                        parseData.setVerified(handler);
                    }
                    else
                    {
                        parseData.addInvalidInputDefect(handler, errorMessage);
                    }
                }
                else if (!handler.getField().isOptional())
                {
                    parseData.addMissingValueDefect(handler);
                }
                else
                {
                    parseData.setMissing(handler);
                }
            }

            List<IDefect> defects = parseData.getDefects();
            IChecked<? extends T> checkedValue = objectFactory.makeChecked(parseData);
            defects.addAll(checkedValue.getDefects());
            T value = checkedValue.getValue();
            if (objectFactory.isReturnable())
            {
                if (defects.stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical))
                {
                    value = null;
                }
                return Optional.of(BasicChecked.newChecked(value, unparsed.getValue(), defects));
            }
            else if (defects.isEmpty())
            {
                return Optional.empty();
            }
            else
            {
                defects.add(BasicDefect.newNonReturnableDefect(definingPath));
                return Optional.of(BasicChecked.newChecked(null, new NonRecordRawFlatData(unparsed.getValue()), defects));
            }
        }
    }
}
