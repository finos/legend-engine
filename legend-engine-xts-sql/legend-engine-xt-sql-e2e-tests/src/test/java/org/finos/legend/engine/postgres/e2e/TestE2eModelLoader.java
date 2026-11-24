// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestE2eModelLoader
{
    private static PureModelContextPointer pointer(String group, String artifact, String version)
    {
        AlloySDLC sdlc = new AlloySDLC();
        sdlc.groupId = group;
        sdlc.artifactId = artifact;
        sdlc.version = version;
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlc;
        return pointer;
    }

    private static E2eModelLoader loader()
    {
        PureModelContextData model = GrammarParseTestUtils.loadPureModelContextFromResource("e2e-model.pure", TestE2eModelLoader.class);
        return new E2eModelLoader(model, "pg-host", 5433, "pg-db");
    }

    @Test
    public void supportsOnlyTheE2eCoordinates()
    {
        E2eModelLoader loader = loader();
        Assertions.assertTrue(loader.supports(pointer(E2eModelLoader.GROUP_ID, E2eModelLoader.ARTIFACT_ID, E2eModelLoader.VERSION)));
        Assertions.assertFalse(loader.supports(pointer(E2eModelLoader.GROUP_ID, E2eModelLoader.ARTIFACT_ID, "2.0.0")));
        Assertions.assertFalse(loader.supports(pointer("other", E2eModelLoader.ARTIFACT_ID, E2eModelLoader.VERSION)));
    }

    @Test
    public void rejectsLoadingOtherCoordinates()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> loader().load(null, pointer("other", "other", "1"), null, null));
    }

    @Test
    public void testConnectionPointsAtThePostgresContainer()
    {
        PureModelContextData model = loader().load(null,
                pointer(E2eModelLoader.GROUP_ID, E2eModelLoader.ARTIFACT_ID, E2eModelLoader.VERSION), null, null);
        PackageableConnection packageableConnection = model.getElementsOfType(PackageableConnection.class).stream()
                .filter(c -> E2eModelLoader.CONNECTION_PATH.equals(c.getPath()))
                .findFirst().orElseThrow(AssertionError::new);
        RelationalDatabaseConnection connection = (RelationalDatabaseConnection) packageableConnection.connectionValue;
        StaticDatasourceSpecification specification = (StaticDatasourceSpecification) connection.datasourceSpecification;
        Assertions.assertEquals("pg-host", specification.host);
        Assertions.assertEquals(5433, specification.port);
        Assertions.assertEquals("pg-db", specification.databaseName);
        Assertions.assertEquals("e2e.", ((UserNamePasswordAuthenticationStrategy) connection.authenticationStrategy).baseVaultReference);
    }

    @Test
    public void modelIsCachedUnderItsPointer()
    {
        E2eModelLoader loader = loader();
        PureModelContextPointer pointer = pointer(E2eModelLoader.GROUP_ID, E2eModelLoader.ARTIFACT_ID, E2eModelLoader.VERSION);
        Assertions.assertTrue(loader.shouldCache(pointer));
        Assertions.assertSame(pointer, loader.cacheKey(pointer, null));
    }
}
