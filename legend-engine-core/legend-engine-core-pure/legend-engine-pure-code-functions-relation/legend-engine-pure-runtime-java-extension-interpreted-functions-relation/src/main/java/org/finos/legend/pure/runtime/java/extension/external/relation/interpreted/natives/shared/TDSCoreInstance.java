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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

public class TDSCoreInstance extends SimpleCoreInstance
{
    private final TestTDSInterpreted tds;

    public TDSCoreInstance(TestTDS tds, CoreInstance genericType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super("", null, _Package.getByUserPath("meta::pure::metamodel::relation::TDS", processorSupport), -1, repository, false);
        Instance.addValueToProperty(this, M3Properties.classifierGenericType, genericType, processorSupport);
        this.tds = (TestTDSInterpreted)tds;
    }

    public TestTDS getTDS()
    {
        return this.tds;
    }
}
