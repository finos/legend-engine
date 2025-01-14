// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.ingest.compiler;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.protocol.ingest.metamodel.DatasetMark;
import org.finos.legend.engine.protocol.ingest.metamodel.Watermark;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.Map;

public class IngestJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    @Override
    public  Map<String, Class<?>> dependencies()
    {
        return Maps.fixedSize.of(
                "meta::external::ingest::metamodel::Watermark", Watermark.class,
                "meta::external::ingest::metamodel::DatasetMark", DatasetMark.class
        );
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(this.dependencies().values());
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("ingest");
    }
}
