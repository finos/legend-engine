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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableConnection_Impl;

public class PackageableConnectionCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "PackageableConnection");
    }

    @Override
    public CompilerExtension build()
    {
        return new PackageableConnectionCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        PackageableConnection.class,
                        Lists.fixedSize.with(Mapping.class),
                        (PackageableConnection packageableConnection, CompileContext context) ->
                        {
                            Root_meta_pure_runtime_PackageableConnection metamodel = new Root_meta_pure_runtime_PackageableConnection_Impl(packageableConnection.name, SourceInformationHelper.toM3SourceInformation(packageableConnection.sourceInformation), context.pureModel.getClass("meta::pure::runtime::PackageableConnection"));
                            Root_meta_core_runtime_Connection connection = packageableConnection.connectionValue.accept(new ConnectionFirstPassBuilder(context));
                            return metamodel._connectionValue(connection);
                        },
                        (PackageableConnection packageableConnection, CompileContext context) ->
                        {
                            final Root_meta_core_runtime_Connection pureConnection = context.pureModel.getConnection(context.pureModel.buildPackageString(packageableConnection._package, packageableConnection.name), packageableConnection.sourceInformation);
                            packageableConnection.connectionValue.accept(new ConnectionSecondPassBuilder(context, pureConnection));
                        }
                )
        );
    }
}
