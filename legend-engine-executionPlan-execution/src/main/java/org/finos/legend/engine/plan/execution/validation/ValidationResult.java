// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.validation;

public class ValidationResult
{
    private static final ValidationResult SUCCESS = new ValidationResult(true);
    private final boolean isValid;
    private String inValidReason;

    private ValidationResult(boolean isValid)
    {
        this.isValid = isValid;
    }

    private ValidationResult(boolean isValid, String inValidReason)
    {
        this.isValid = isValid;
        this.inValidReason = inValidReason;
    }

    static ValidationResult errorValidationResult(String errorMessage)
    {
        return new ValidationResult(false, errorMessage);
    }

    static ValidationResult successValidationResult()
    {
        return ValidationResult.SUCCESS;
    }

    public boolean isValid()
    {
        return this.isValid;
    }

    @Override
    public String toString()
    {
        return !isValid ? inValidReason : "valid";
    }
}
