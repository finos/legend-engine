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

package org.finos.legend.engine.language.pure.code.completer.test;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.code.completer.CompleterExtension;
import org.finos.legend.engine.language.pure.code.completer.RelationalCompleterExtension;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.ServiceLoader;

public class TestExtensionAvailable
{
    @Test
    public void testServiceAvailable()
    {
        List<CompleterExtension> completerExtensions = Lists.mutable.withAll(ServiceLoader.load(CompleterExtension.class));
        Assert.assertTrue(completerExtensions.stream().anyMatch(x -> x instanceof RelationalCompleterExtension));
    }
}