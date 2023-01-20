// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.demo.AuthenticationDemo;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo_Impl;

public class AuthenticationDemoCompilerExtension implements IAuthenticationDemoCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        Processor<AuthenticationDemo> processor = Processor.newProcessor(
                AuthenticationDemo.class,
                Lists.fixedSize.empty(),
                (authenticationDemo, context) -> new Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo_Impl(authenticationDemo.name, null, context.pureModel.getClass("meta::pure::runtime::connection::authentication::demo::AuthenticationDemo"))
                        ._name(authenticationDemo.name),
                (authenticationDemo, context) ->
                {
                    Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo pureAuthenticationDemo = (Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo) context.pureModel.getOrCreatePackage(authenticationDemo._package)._children().detect(c -> authenticationDemo.name.equals(c._name()));
                    pureAuthenticationDemo._authentication(HelperAuthenticationBuilder.buildAuthenticationSpecification(authenticationDemo.authenticationSpecification, context));
                }
        );
        return Lists.fixedSize.of(processor);
    }
}
