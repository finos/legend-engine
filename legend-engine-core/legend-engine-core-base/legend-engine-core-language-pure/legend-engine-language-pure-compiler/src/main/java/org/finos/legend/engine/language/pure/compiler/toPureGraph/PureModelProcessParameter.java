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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import java.util.concurrent.ForkJoinPool;

public class PureModelProcessParameter
{
    private final String packagePrefix;
    private final ForkJoinPool forkJoinPool;
    private final boolean enablePartialCompilation;

    PureModelProcessParameter()
    {
        this(null);
    }

    public PureModelProcessParameter(String packagePrefix)
    {
        this(packagePrefix, null, false);
    }

    private PureModelProcessParameter(String packagePrefix, ForkJoinPool forkJoinPool, boolean enablePartialCompilation)
    {
        this.packagePrefix = packagePrefix;
        this.forkJoinPool = forkJoinPool;
        this.enablePartialCompilation = enablePartialCompilation;
    }

    public String getPackagePrefix()
    {
        return this.packagePrefix;
    }

    public ForkJoinPool getForkJoinPool()
    {
        return this.forkJoinPool;
    }

    public boolean getEnablePartialCompilation()
    {
        return this.enablePartialCompilation;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private String packagePrefix;
        private ForkJoinPool forkJoinPool;
        private boolean enablePartialCompilation;

        public Builder()
        {
        }

        public void setPackagePrefix(String packagePrefix)
        {
            this.packagePrefix = packagePrefix;
        }

        public Builder withPackagePrefix(String packagePrefix)
        {
            setPackagePrefix(packagePrefix);
            return this;
        }

        public void setForkJoinPool(ForkJoinPool forkJoinPool)
        {
            this.forkJoinPool = forkJoinPool;
        }

        public Builder withForkJoinPool(ForkJoinPool forkJoinPool)
        {
            setForkJoinPool(forkJoinPool);
            return this;
        }

        public void setEnablePartialCompilation(boolean enablePartialCompilation)
        {
            this.enablePartialCompilation = enablePartialCompilation;
        }

        public Builder withEnablePartialCompilation(boolean enablePartialCompilation)
        {
            setEnablePartialCompilation(enablePartialCompilation);
            return this;
        }

        public PureModelProcessParameter build()
        {
            return new PureModelProcessParameter(this.packagePrefix, this.forkJoinPool, this.enablePartialCompilation);
        }
    }
}