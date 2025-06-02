// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.tracing;

import io.opentracing.util.GlobalTracer;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class AbstractTestTraceSpan extends AbstractPureTestWithCoreCompiled
{
  @After
  public void cleanRuntime()
  {
    runtime.delete("fromString.pure");
    runtime.compile();
  }

  protected static final InMemoryTracer tracer = new InMemoryTracer();

  @Test
  public void testTraceSpan()
  {
    compileTestSource("fromString.pure", "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute');\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertTrue(tracer.spanExists("Test Execute"));
  }

  @Test
  public void testTraceSpanWithReturnValue()
  {
    compileTestSource("fromString.pure", "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    let text = meta::pure::functions::tracing::traceSpan(|' World', 'Test Execute');\n" +
        "    print('Hello' + $text, 1);\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
  }

  @Test
  public void testTraceSpanWithAnnotations()
  {
    compileTestSource("fromString.pure", "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "   let annotations = newMap([\n" +
        "      pair('key1', 'value1'), \n" +
        "      pair('key2', 'value2')\n" +
        "     ]);  \n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |$annotations);\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertTrue(tracer.spanExists("Test Execute"));
    Map<Object, Object> tags = this.tracer.getTags("Test Execute");
    Assert.assertEquals(tags.get("key1"), "value1");
    Assert.assertEquals(tags.get("key2"), "value2");
  }

  @Test
  public void testTraceSpanUsingEval()
  {
    compileTestSource("fromString.pure", "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    let res = meta::pure::functions::tracing::traceSpan_Function_1__String_1__V_m_->eval(|'Hello World', 'Test Execute');\n" +
        "    print($res,1);" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertTrue(tracer.spanExists("Test Execute"));
  }

  @Test
  public void testDoNoTraceIfTracerNotRegistered()
  {
    tracer.reset();
    unregisterTracer();
    compileTestSource("fromString.pure", "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute');\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertFalse(tracer.spanExists("Test Execute"));
    GlobalTracer.registerIfAbsent(tracer);
  }

  @Test
  public void testTraceSpanShouldHandleErrorWhileEvaluatingTagsLamda()
  {
    compileTestSource("fromString.pure", "function getTags(): Map<String, String>[1] {" +
        "   assert('a' == 'b', |'');    " +
        "   newMap([        \n" +
        "      pair('key1', '') \n" +
        "     ]);  \n" +
        "}" +
        "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |getTags(), false);\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertTrue(tracer.spanExists("Test Execute"));
    Map<Object, Object> tags = this.tracer.getTags("Test Execute");
    Assert.assertTrue(tags.get("Exception").toString().startsWith("Unable to resolve tags - "));
  }

  @Test
  public void testTraceSpanShouldHandleStackOverflowErrorWhileEvaluatingTagsLamda()
  {
    compileTestSource("fromString.pure", "function getTags(): Map<String, String>[1] {" +
        "   getTags();  \n" +
        "}" +
        "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World', 1), 'Test Execute', |getTags(), false);\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
    Assert.assertEquals("'Hello World'", this.functionExecution.getConsole().getLine(0));
    Assert.assertTrue(tracer.spanExists("Test Execute"));
    Map<Object, Object> tags = this.tracer.getTags("Test Execute");
    Assert.assertTrue(tags.get("Exception").toString().startsWith("Unable to resolve tags - "));
  }

  @Test(expected = PureAssertFailException.class)
  public void testTraceSpanShouldNotHandleErrorWhileEvaluatingTagsLamda()
  {
    compileTestSource("fromString.pure", "function getTags(): Map<String, String>[1] {" +
        "   assert('a' == 'b', |'');    " +
        "   newMap([        \n" +
        "      pair('key1', '') \n" +
        "     ]);  \n" +
        "}" +
        "function testTraceSpan():Nil[0]\n" +
        "{\n" +
        "    meta::pure::functions::tracing::traceSpan(|print('Hello World',1), 'Test Execute', |getTags());\n" +
        "}\n");
    this.execute("testTraceSpan():Nil[0]");
  }

  @AfterClass
  public static void tearDown()
  {
    tracer.reset();
    unregisterTracer();
  }

  private static void unregisterTracer()
  {
    try
    {
      // HACK since GlobalTracer api doesnt provide a way to reset the tracer which is needed for testing
      Field tracerField = GlobalTracer.get().getClass().getDeclaredField("isRegistered");
      tracerField.setAccessible(true);
      tracerField.set(GlobalTracer.get(), false);
      Assert.assertFalse(GlobalTracer.isRegistered());
    }
    catch (Exception ignored)
    {
    }
  }

}
