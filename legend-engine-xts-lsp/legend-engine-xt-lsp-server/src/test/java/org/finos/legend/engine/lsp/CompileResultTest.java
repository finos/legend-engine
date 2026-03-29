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

package org.finos.legend.engine.lsp;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class CompileResultTest
{
    @Test
    public void notReady_state()
    {
        LegendPureSession.CompileResult r = LegendPureSession.CompileResult.notReady();
        Assert.assertFalse(r.isReady());
        Assert.assertFalse(r.isSuccess());
        Assert.assertFalse(r.isInternalError());
        Assert.assertNull(r.getError());
        Assert.assertTrue(r.getModifiedFiles().isEmpty());
    }

    @Test
    public void success_state()
    {
        LegendPureSession.CompileResult r = LegendPureSession.CompileResult.success(Arrays.asList("/a.pure", "/b.pure"));
        Assert.assertTrue(r.isReady());
        Assert.assertTrue(r.isSuccess());
        Assert.assertFalse(r.isInternalError());
        Assert.assertNull(r.getError());
        Assert.assertEquals(2, r.getModifiedFiles().size());
        Assert.assertEquals("/a.pure", r.getModifiedFiles().get(0));
    }

    @Test
    public void compileError_state()
    {
        Exception e = new RuntimeException("parse error");
        LegendPureSession.CompileResult r = LegendPureSession.CompileResult.error(e, false);
        Assert.assertTrue(r.isReady());
        Assert.assertFalse(r.isSuccess());
        Assert.assertFalse(r.isInternalError());
        Assert.assertSame(e, r.getError());
    }

    @Test
    public void internalError_state()
    {
        Exception e = new NullPointerException("oops");
        LegendPureSession.CompileResult r = LegendPureSession.CompileResult.error(e, true);
        Assert.assertTrue(r.isReady());
        Assert.assertFalse(r.isSuccess());
        Assert.assertTrue(r.isInternalError());
        Assert.assertSame(e, r.getError());
    }
}
