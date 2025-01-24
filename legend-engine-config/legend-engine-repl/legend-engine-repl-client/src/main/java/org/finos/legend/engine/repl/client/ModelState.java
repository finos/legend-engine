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


package org.finos.legend.engine.repl.client;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.core.legend.LegendInterface;

public class ModelState
{
    private final LegendInterface legendInterface;
    private final MutableList<ReplExtension> replExtensions;
    private final MutableList<String> state = Lists.mutable.empty();

    public ModelState(LegendInterface legendInterface, MutableList<ReplExtension> replExtensions)
    {
        this.legendInterface = legendInterface;
        this.replExtensions = replExtensions;
    }

    public ModelState addElement(String element)
    {
        this.state.add(element);
        return this;
    }

    public PureModelContextData parse()
    {
        return this.legendInterface.parse(getText());
    }

    public PureModel compile()
    {
        return this.legendInterface.compile(parse());
    }

    public PureModel compileWithTransient(String transientCode)
    {
        return this.legendInterface.compile(parseWithTransient(transientCode));
    }

    public PureModelContextData parseWithTransient(String transientCode)
    {
        return this.legendInterface.parse(getText() + transientCode);
    }

    public String getText()
    {
        String code = Lists.mutable.withAll(state).makeString("\n");
        return code + replExtensions.flatCollect(r -> r.generateDynamicContent(code)).makeString("\n");
    }
}
