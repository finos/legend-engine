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

package org.finos.legend.engine.pure.preeval;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalImplementation
{
    @Test
    public void testUnsetDefaultsToPure()
    {
        Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.parse(null));
    }

    @Test
    public void testBlankDefaultsToPure()
    {
        Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.parse("  "));
    }

    @Test
    public void testParsesCaseInsensitively()
    {
        Assertions.assertEquals(PreevalImplementation.JAVA, PreevalImplementation.parse("java"));
        Assertions.assertEquals(PreevalImplementation.SHADOW, PreevalImplementation.parse(" Shadow "));
    }

    @Test
    public void testRejectsUnknownValue()
    {
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> PreevalImplementation.parse("FAST"));
        Assertions.assertEquals("Invalid value 'FAST' for system property legend.engine.preeval.implementation; expected one of PURE, JAVA, SHADOW", e.getMessage());
    }

    @Test
    public void testCurrentFollowsSystemProperty()
    {
        withProperty("java", () -> Assertions.assertEquals(PreevalImplementation.JAVA, PreevalImplementation.current()));
        withProperty(null, () -> Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.current()));
        withProperty("SHADOW", () -> Assertions.assertEquals(PreevalImplementation.SHADOW, PreevalImplementation.current()));
    }

    @Test
    public void testCurrentRejectsUnknownValueEveryTime()
    {
        withProperty("FAST", () ->
        {
            Assertions.assertThrows(IllegalArgumentException.class, PreevalImplementation::current);
            Assertions.assertThrows(IllegalArgumentException.class, PreevalImplementation::current);
        });
    }

    private static void withProperty(String value, Runnable body)
    {
        String previous = System.getProperty(PreevalImplementation.SYSTEM_PROPERTY);
        try
        {
            if (value == null)
            {
                System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            }
            else
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, value);
            }
            body.run();
        }
        finally
        {
            if (previous == null)
            {
                System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            }
            else
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, previous);
            }
        }
    }
}
