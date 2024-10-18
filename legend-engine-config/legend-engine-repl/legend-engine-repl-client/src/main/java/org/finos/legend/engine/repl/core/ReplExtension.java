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

package org.finos.legend.engine.repl.core;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.shared.core.extension.LegendExtension;

public interface ReplExtension extends LegendExtension
{
    @Override
    default MutableList<String> group()
    {
        return Lists.mutable.with("REPL");
    }

    @Override
    default String type()
    {
        return "core";
    }

    @Override
    default MutableList<String> typeGroup()
    {
        return Lists.mutable.empty();
    }

    MutableList<Command> getExtraCommands();

    boolean supports(Result res);

    String print(Result res);

    MutableList<String> generateDynamicContent(String code);

    default void initialize(Client client)
    {
    }

    // This method is called after all extensions and the client have been initialized.
    // This is useful for cases where we need to invoke initialization-type tasks from one extension
    // that might depend on another extension. This is for now the preferred approach over specifying
    // a dependency graph to determine the order of initialization.
    default void postInitialize(Client client)
    {
    }
}
