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

package org.finos.legend.pure.runtime.java.extension.relation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m2.inlinedsl.tds.TDSExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.TDS;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.finos.legend.pure.runtime.java.compiled.delta.MetadataProvider;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataPelt;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

public class TestTDSImpl extends TestTDS
{
    static ProcessorSupport ps;

    static
    {
        RichIterable<CodeRepository> codeRepos = CodeRepositoryProviderHelper.findCodeRepositories().select((r) -> !r.getName().equals("test_generic_repository") && !r.getName().equals("other_test_generic_repository"));
        ClassLoader classLoader = TestTDSImpl.class.getClassLoader();
        CompiledExecutionSupport cs = new CompiledExecutionSupport(new JavaCompilerState((MemoryFileManager) null, classLoader), new CompiledProcessorSupport(classLoader, MetadataPelt.fromClassLoader(classLoader, codeRepos.collect(CodeRepository::getName)), Sets.mutable.empty()), (SourceRegistry) null, new CompositeCodeStorage(new RepositoryCodeStorage[]{new ClassLoaderCodeStorage(classLoader, codeRepos)}), (IncrementalCompiler) null, (ExecutionActivityListener) null, new ConsoleCompiled(), (MetadataProvider) null, Sets.mutable.empty(), CompiledExtensionLoader.extensions());
        ps = cs.getProcessorSupport();
    }

    public TestTDSImpl(String csv)
    {
        super(ps);
        TDS<?> tds = TDSExtension.parse(csv, null, ps);
        this.build(readCsv(tds._csv()), ((RelationType<?>) tds._classifierGenericType()._typeArguments().getFirst()._rawType())._columns().collect(_Column::getColumnType).toList(), ps);
    }

    public TestTDSImpl(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypesByColumn, int rows)
    {
        super(columnOrdered, pureTypesByColumn, rows, null);
    }

    @Override
    public Object getValueAsCoreInstance(String columnName, int rowNum)
    {
        return null;
    }

    @Override
    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypesByColumn, int rows)
    {
        return new TestTDSImpl(columnOrdered, pureTypesByColumn, rows);
    }
}
