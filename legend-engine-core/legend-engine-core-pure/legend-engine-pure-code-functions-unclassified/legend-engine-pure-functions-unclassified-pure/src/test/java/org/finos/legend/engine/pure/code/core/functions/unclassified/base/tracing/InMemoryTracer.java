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

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tag;
import org.eclipse.collections.api.factory.Maps;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class InMemoryTracer implements Tracer
{
  private InMemoryScopeManager scopeManager = new InMemoryScopeManager();
  private final Map<String, Span> spanByOperationName = Maps.mutable.empty();

  @Override
  public ScopeManager scopeManager()
  {
    return scopeManager;
  }

  @Override
  public Span activeSpan()
  {
    return scopeManager.activeSpan();
  }

  @Override
  public Scope activateSpan(Span span)
  {
    return scopeManager.activate(span);
  }

  @Override
  public SpanBuilder buildSpan(String operationName)
  {
    InMemorySpanBuilder localSpanBuilder = new InMemorySpanBuilder(operationName, scopeManager);
    spanByOperationName.put(operationName, localSpanBuilder.span);
    return localSpanBuilder;
  }

  @Override
  public <C> void inject(SpanContext spanContext, Format<C> format, C c)
  {

  }

  @Override
  public <C> SpanContext extract(Format<C> format, C c)
  {
    return scopeManager.currentScope.span.context();
  }

  @Override
  public void close()
  {

  }

  public Map<Object, Object> getTags(String operationName)
  {
    return ((InMemorySpan) spanByOperationName.get(operationName)).getTags();
  }

  public void reset()
  {
    spanByOperationName.clear();
    scopeManager = new InMemoryScopeManager();
  }

  public boolean spanExists(String operationName)
  {
    return spanByOperationName.containsKey(operationName);
  }

  public class InMemoryScopeManager implements ScopeManager
  {
    private final InMemoryScope currentScope = new InMemoryScope();

    @Override
    public Scope activate(Span span)
    {
      currentScope.set(span);
      return currentScope;
    }

    @Override
    public Scope active()
    {
      return currentScope;
    }

    @Override
    public Span activeSpan()
    {
      return currentScope.span;
    }

    @Override
    public Scope activate(Span span, boolean b)
    {
      currentScope.set(span);
      return currentScope;
    }
  }

  public class InMemoryScope implements Scope
  {
    private Span span;

    @Override
    public void close()
    {
    }

    @Override
    public Span span()
    {
      return span;
    }

    public void set(Span span)
    {
      this.span = span;
    }
  }

  public class InMemorySpanBuilder implements SpanBuilder
  {
    private final Span span = new InMemorySpan();
    private final InMemoryScopeManager scopeManager;

    public InMemorySpanBuilder(String operationName, InMemoryScopeManager scopeManager)
    {
      this.scopeManager = scopeManager;
      span.setOperationName(operationName);
    }

    @Override
    public SpanBuilder asChildOf(SpanContext spanContext)
    {
      return this;
    }

    @Override
    public SpanBuilder asChildOf(Span span)
    {
      return this;
    }

    @Override
    public SpanBuilder addReference(String s, SpanContext spanContext)
    {
      return this;
    }

    @Override
    public SpanBuilder ignoreActiveSpan()
    {
      return this;
    }

    @Override
    public SpanBuilder withTag(String s, String s1)
    {
      span.setTag(s, s1);
      return this;
    }

    @Override
    public SpanBuilder withTag(String s, boolean b)
    {
      span.setTag(s, b);
      return this;
    }

    @Override
    public SpanBuilder withTag(String s, Number number)
    {
      span.setTag(s, number);
      return this;
    }

    @Override
    public <T> SpanBuilder withTag(Tag<T> tag, T t)
    {
      span.setTag(tag, t);
      return this;
    }

    @Override
    public SpanBuilder withStartTimestamp(long l)
    {
      return this;
    }

    @Override
    public Scope startActive(boolean b)
    {
      return scopeManager.activate(span, b);
    }

    @Override
    public Span startManual()
    {
      return span;
    }

    @Override
    public Span start()
    {
      return span;
    }
  }

  public static class InMemorySpan implements Span
  {
    private final InMemorySpanContext context = new InMemorySpanContext();
    private final Map<Object, Object> tags = Maps.mutable.empty();
    private final Map<String, ?> log = Maps.mutable.empty();
    private String operationName;

    @Override
    public SpanContext context()
    {
      return context;
    }

    @Override
    public Span setTag(String name, String value)
    {
      tags.put(name, value);
      return this;
    }

    @Override
    public Span setTag(String name, boolean value)
    {
      tags.put(name, value);
      return this;
    }

    @Override
    public Span setTag(String name, Number value)
    {
      tags.put(name, value);
      return this;
    }

    @Override
    public <T> Span setTag(Tag<T> tag, T t)
    {
      tags.put(tag, t);
      return this;
    }

    @Override
    public Span log(Map<String, ?> map)
    {
      tags.putAll(map);
      return this;
    }

    @Override
    public Span log(long l, Map<String, ?> map)
    {
      tags.putAll(map);
      return this;
    }

    @Override
    public Span log(String text)
    {
      tags.put("def", text);
      return this;
    }

    @Override
    public Span log(long l, String text)
    {
      tags.put("def", text);
      return this;
    }

    @Override
    public Span setBaggageItem(String name, String value)
    {
      context.put(name, value);
      return this;
    }

    @Override
    public String getBaggageItem(String name)
    {
      return context.get(name);
    }

    @Override
    public Span setOperationName(String s)
    {
      operationName = s;
      return this;
    }

    @Override
    public void finish()
    {

    }

    @Override
    public void finish(long l)
    {

    }

    public Map<Object, Object> getTags()
    {
      return tags;
    }
  }

  public static class InMemorySpanContext implements SpanContext
  {
    private final Map<String, String> items = Maps.mutable.empty();
    private final String traceId = UUID.randomUUID().toString();
    private final String spanId = UUID.randomUUID().toString();

    @Override
    public String toTraceId()
    {
      return traceId;
    }

    @Override
    public String toSpanId()
    {
      return spanId;
    }

    @Override
    public Iterable<Entry<String, String>> baggageItems()
    {
      return items.entrySet();
    }

    public void put(String key, String value)
    {
      items.put(key, value);
    }

    public String get(String name)
    {
      return items.get(name);
    }
  }
}
