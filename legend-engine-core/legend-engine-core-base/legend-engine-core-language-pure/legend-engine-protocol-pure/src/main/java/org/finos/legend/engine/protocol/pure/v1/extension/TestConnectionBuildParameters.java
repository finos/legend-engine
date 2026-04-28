// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.extension;

public class TestConnectionBuildParameters
{
    public static final TestConnectionBuildParameters NONE = newBuilder().build();

    private final boolean isRelation;

    private TestConnectionBuildParameters(Builder builder)
    {
        this.isRelation = builder.isRelation;
    }

    public boolean isRelation()
    {
        return isRelation;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private boolean isRelation = false;

        private Builder()
        {
        }

        public Builder withIsRelation(boolean isRelation)
        {
            this.isRelation = isRelation;
            return this;
        }

        public TestConnectionBuildParameters build()
        {
            return new TestConnectionBuildParameters(this);
        }
    }
}
