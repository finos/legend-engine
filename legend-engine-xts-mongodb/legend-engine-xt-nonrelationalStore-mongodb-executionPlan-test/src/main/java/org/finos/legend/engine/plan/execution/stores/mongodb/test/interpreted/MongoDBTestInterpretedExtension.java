// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.mongodb.test.interpreted;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.mongodb.test.shared.MongoDBCommands;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class MongoDBTestInterpretedExtension extends BaseInterpretedExtension
{
    public MongoDBTestInterpretedExtension()
    {
        super(Lists.mutable.with(
                Tuples.pair(MongoDBCommands.STOP_SERVER_FUNCTION,    MongoDBStopCommand::new),
                Tuples.pair(MongoDBCommands.START_SERVER_FUNCTION,    MongoDBStartCommand::new),
                Tuples.pair(MongoDBCommands.REQUEST_SERVER_FUNCTION,    MongodBRequestCommand::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new MongoDBTestInterpretedExtension();
    }
}
