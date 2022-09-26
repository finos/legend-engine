// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.persistence.ingestmode;

import org.apache.commons.io.FileUtils;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.testable.model.RunTestsResult;
import org.finos.legend.engine.testable.persistence.extension.PersistenceTestableRunnerExtension;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.io.File;
import java.io.IOException;

public abstract class TestPersistenceBase
{

    protected String readPureCode(String path) throws IOException
    {
        File file = new File(path);
        return FileUtils.readFileToString(file, "UTF-8");
    }

    protected RunTestsResult testPersistence(String persistenceSpec)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(persistenceSpec);
        PureModel model = Compiler.compile(contextData, DeploymentMode.TEST, null);
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        Root_meta_pure_persistence_metamodel_Persistence purePersistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        // Invoke
        PersistenceTestableRunnerExtension extension = new PersistenceTestableRunnerExtension();
        return extension.executePersistenceTest(purePersistence, model, contextData);
    }
}
