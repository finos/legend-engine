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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class ValidationResult
{
    private static final ValidationResult SUCCESS = new Success();

    public abstract boolean valid();

    public boolean invalid()
    {
        return !valid();
    }

    public abstract List<String> reasons();

    public abstract ValidationResult combine(ValidationResult other);

    public static ValidationResult success()
    {
        return SUCCESS;
    }

    public static ValidationResult failure(String reason)
    {
        return new Failure(reason);
    }

    public static ValidationResult failure(List<String> reasons)
    {
        return new Failure(reasons);
    }

    private static class Success extends ValidationResult
    {
        private Success()
        {
        }

        @Override
        public boolean valid()
        {
            return true;
        }

        @Override
        public List<String> reasons()
        {
            return Collections.singletonList("Success");
        }

        @Override
        public ValidationResult combine(ValidationResult other)
        {
            return other.valid() ? this : other;
        }
    }

    private static class Failure extends ValidationResult
    {
        private final List<String> reasons;

        private Failure(String reason)
        {
            this.reasons = Collections.singletonList(Objects.requireNonNull(reason));
        }

        private Failure(List<String> reasons)
        {
            this.reasons = Collections.unmodifiableList(reasons);
        }

        @Override
        public boolean valid()
        {
            return false;
        }

        @Override
        public List<String> reasons()
        {
            return reasons;
        }

        @Override
        public ValidationResult combine(ValidationResult other)
        {
            if (other.valid())
            {
                return this;
            }

            List<String> allReasons = new ArrayList<>(this.reasons().size() + other.reasons().size());
            allReasons.addAll(this.reasons());
            allReasons.addAll(other.reasons());
            return ValidationResult.failure(allReasons);
        }
    }
}
