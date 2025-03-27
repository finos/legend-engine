//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.deephaven.test.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.DeephavenCommands;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class DeephavenTestInterpretedExtension extends BaseInterpretedExtension
{
    public DeephavenTestInterpretedExtension()
    {
        super(Lists.mutable.with(
                Tuples.pair(DeephavenCommands.START_SERVER_FUNCTION, DeephavenStartCommand::new),
                Tuples.pair(DeephavenCommands.STOP_SERVER_FUNCTION, DeephavenStopCommand::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new DeephavenTestInterpretedExtension();
    }
}
