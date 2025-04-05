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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;

/** Use to mock data in function using a runtime for execution
 * store represents the store you want to mock data for
 * This assume the function uses 1 (or none) runtime
 * We will resolve the connection used for the store
 * In the future, this could be extended to add runtime pointer if more than one runtime
 * and/or one connection but for now the expectation is one store can be mocked
 */

public class StoreTestData
{
    public String doc;
    public StoreProviderPointer store;
    public EmbeddedData data;
    public SourceInformation sourceInformation;

}
