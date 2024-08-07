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

package org.finos.legend.engine.persistence.components.relational.exception;

import org.finos.legend.engine.persistence.components.exception.PersistenceException;
import org.finos.legend.engine.persistence.components.relational.api.DataError;

import java.util.List;

public class DataQualityException extends PersistenceException
{
    private List<DataError> dataErrors;

    public List<DataError> getDataErrors()
    {
        return dataErrors;
    }

    public DataQualityException(String message, List<DataError> dataErrors)
    {
        super(message);
        this.dataErrors = dataErrors;
    }

    @Override
    public boolean isRecoverable()
    {
        return false;
    }
}
