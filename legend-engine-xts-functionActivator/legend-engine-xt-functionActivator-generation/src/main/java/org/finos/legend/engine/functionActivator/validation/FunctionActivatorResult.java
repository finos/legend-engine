//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.functionActivator.validation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class FunctionActivatorResult
{
    public MutableList<FunctionActivatorError> errors = Lists.mutable.empty();
    public MutableList<FunctionActivatorWarning> warnings = Lists.mutable.empty();
    
    public FunctionActivatorResult()
    {
        
    }

    public FunctionActivatorResult(MutableList<FunctionActivatorError> errors)
    {
        this.errors = errors;
    }

    public FunctionActivatorResult(MutableList<FunctionActivatorError> errors, MutableList<FunctionActivatorWarning> warnings)
    {
        this.errors = errors;
        this.warnings = warnings;
    }
    
    public MutableList<FunctionActivatorError> getErrors()
    {
        return this.errors;
    }

    public MutableList<FunctionActivatorWarning> getWarnings()
    {
        return this.warnings;
    }
    
    public void addError(FunctionActivatorError error)
    {
        this.errors.add(error);
    }

    public void addWarning(FunctionActivatorWarning warning)
    {
        this.warnings.add(warning);
    }
    
    public void addAll(FunctionActivatorResult result)
    {
        this.errors.addAll(result.errors);
        this.warnings.addAll(result.warnings);
    }
}
