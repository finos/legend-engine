// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.shared.core.test;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Engine object mappers must work on a Jackson older than 2.18, where UnlimitedStreamConstraints does nothing:
 * <ul>
 *     <li>2.10.5, which Legend model projects' generated tests run engine code with (their entities pom pins it for
 *     every project structure up to 13), has no stream constraints at all;</li>
 *     <li>2.17.2 has the read and write constraints but not the token count limit, which arrived in 2.18.</li>
 * </ul>
 * This test only runs in the old-jackson surefire executions of this module, which replace the module's Jackson with
 * the version named by the {@code old.jackson.version} system property.
 */
public class TestUnlimitedStreamConstraintsWithOldJackson
{
    @Test
    public void theClasspathHasTheOldJackson()
    {
        String expected = System.getProperty("old.jackson.version");
        Assertions.assertNotNull(expected, "old.jackson.version is set by the old-jackson surefire executions");
        Assertions.assertEquals(expected, PackageVersion.VERSION.toString());
    }

    @Test
    public void standardObjectMappersWork() throws Exception
    {
        ObjectMapper mapper = ObjectMapperFactory.withStandardConfigurations(new ObjectMapper());
        Assertions.assertEquals("{\"key\":\"value\"}", mapper.writeValueAsString(Collections.singletonMap("key", "value")));
    }

    @Test
    public void unlimitedJvmDefaultsAreANoOp()
    {
        ObjectMapperFactory.useUnlimitedStreamConstraintsByDefault();
    }
}
