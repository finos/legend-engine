//  Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.testConnection.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.runtime.testConnection.interpreted.natives.GetTestConnection;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class GetTestConnectionInterpretedExtension extends BaseInterpretedExtension
{
    public GetTestConnectionInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("getTestConnection_DatabaseType_1__RelationalDatabaseConnection_1_", GetTestConnection::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new GetTestConnectionInterpretedExtension();
    }
}
