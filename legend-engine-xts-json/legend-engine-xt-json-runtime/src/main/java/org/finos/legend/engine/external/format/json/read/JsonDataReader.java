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

package org.finos.legend.engine.external.format.json.read;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class JsonDataReader<T>
{
    private final JsonParser parser;
    private final ObjectMapper objectMapper;

    private boolean finishedReading = false;
    private boolean inArray = false;
    private long recordCount = 0;

    private final Queue<IChecked<T>> queue = new LinkedList<>();
    private final Set<String> declaredMethods = Arrays.stream(this.getClass().getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());

    public JsonDataReader(InputStream in, boolean useBigDecimalForFloats, String pathOffset)
    {
        try
        {
            JsonParser baseParser = new JsonFactory().createParser(in);
            if (pathOffset != null)
            {
                this.parser = new FilteringParserDelegate(baseParser, new JsonPointerBasedFilter(pathOffset), false, false);
            }
            else
            {
                this.parser = baseParser;
            }

            this.objectMapper = new ObjectMapper();
            if (useBigDecimalForFloats)
            {
                this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Stream<IChecked<T>> startStream()
    {
        Iterator<IChecked<T>> iterator = new Iterator<IChecked<T>>()
        {
            @Override
            public boolean hasNext()
            {
                if (queue.peek() == null && !isFinished())
                {
                    queue.addAll(readCheckedObjects());
                }
                if (queue.peek() == null)
                {
                    close();
                }

                return queue.peek() != null;
            }

            @Override
            public IChecked<T> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("End of stream has passed");
                }

                return queue.remove();
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false).onClose(this::close);
    }

    protected abstract IChecked<T> readCheckedObject(JsonNode node, JsonDataRecord source);

    private Collection<IChecked<T>> readCheckedObjects()
    {
        try
        {
            this.recordCount++;
            JsonNode node = this.objectMapper.readValue(this.parser, JsonNode.class);
            return Collections.singleton(readCheckedObject(node, new JsonDataRecord(this.recordCount, node.toString())));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean isFinished()
    {
        nextToken();
        if (!this.finishedReading && getCurrentToken() == JsonToken.START_ARRAY && !inArray)
        {
            nextToken();
            inArray = true;
        }
        if (!this.finishedReading && getCurrentToken() == JsonToken.END_ARRAY && inArray)
        {
            nextToken();
            inArray = false;
            this.finishedReading = true;
        }
        this.finishedReading |= getCurrentToken() == null;
        return this.finishedReading;
    }

    private void close()
    {
        if (this.parser.isClosed())
        {
            return;
        }

        try
        {
            this.parser.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void nextToken()
    {
        try
        {
            this.parser.nextToken();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private JsonToken getCurrentToken()
    {
        return parser.getCurrentToken();
    }

    protected boolean readMethodExists(String name)
    {
        return declaredMethods.contains(name);
    }

    protected Object readMethodInvoke(String name, JsonNode node)
    {
        Method m;
        try
        {
            m = this.getClass().getMethod(name, JsonNode.class);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e.getMessage());
        }

        try
        {
            return m.invoke(this, node);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected String acceptString(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE String";
            this.check(Collections.singletonList(JsonNodeType.valueOf("STRING")),
                    node.getNodeType(),
                    errorMessage);
            return node.textValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected boolean acceptBoolean(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Boolean";
            this.check(Collections.singletonList(JsonNodeType.valueOf("BOOLEAN")),
                    node.getNodeType(),
                    errorMessage);
            return node.booleanValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected long acceptInteger(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Integer";
            this.check(Collections.singletonList(JsonNodeType.valueOf("NUMBER")),
                    node.getNodeType(),
                    errorMessage);
            return node.longValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected double acceptFloat(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Float";
            this.check(Collections.singletonList(JsonNodeType.valueOf("NUMBER")),
                    node.getNodeType(),
                    errorMessage);
            return node.doubleValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected BigDecimal acceptDecimal(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Decimal";
            this.check(Arrays.asList(JsonNodeType.valueOf("STRING"),
                    JsonNodeType.valueOf("NUMBER")),
                    node.getNodeType(),
                    errorMessage);
            return node.getNodeType()
                    .equals(JsonNodeType.STRING)
                    ? new BigDecimal(node.textValue())
                    : node.decimalValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected Number acceptNumber(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Number";
            this.check(Arrays.asList(JsonNodeType.valueOf("STRING"),
                    JsonNodeType.valueOf("NUMBER")),
                    node.getNodeType(),
                    errorMessage);
            return node.getNodeType()
                    .equals(JsonNodeType.STRING)
                    ? new BigDecimal(node.textValue())
                    : node.isDouble()
                    ? node.doubleValue()
                    : node.longValue();
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected PureDate acceptStrictDate(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE StrictDate";
            this.check(Collections.singletonList(JsonNodeType.valueOf("STRING")),
                    node.getNodeType(),
                    errorMessage);
            return org.finos.legend.engine.plan.dependencies.domain.date.PureDate
                    .parsePureDate(node.textValue());
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected PureDate acceptDateTime(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE DateTime";
            this.check(Collections.singletonList(JsonNodeType.valueOf("STRING")),
                    node.getNodeType(),
                    errorMessage);
            return org.finos.legend.engine.plan.dependencies.domain.date.PureDate
                    .parsePureDate(node.textValue());
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected PureDate acceptDate(JsonNode node)
    {
        try
        {
            String errorMessage = "Unexpected node type:" + node.getNodeType() + " for PURE Date";
            this.check(Collections.singletonList(JsonNodeType.valueOf("STRING")),
                    node.getNodeType(),
                    errorMessage);
            return org.finos.legend.engine.plan.dependencies.domain.date.PureDate
                    .parsePureDate(node.textValue());
        }
        catch (IllegalArgumentException ex)
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(ex.getMessage());
        }
    }

    protected <U> List<U> acceptMany(JsonNode node,
                                     Function<JsonNode, U> acceptor,
                                     Consumer<String> defectRecorder)
    {
        List<U> result = new ArrayList<>();
        if (node.isNull())
        {
            return result;
        }
        if (node.isArray())
        {
            for (JsonNode n : node)
            {
                try
                {
                    result.add(acceptor.apply(n));
                }
                catch (org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException ex)
                {
                    defectRecorder.accept(ex.getMessage());
                }
            }
        }
        else
        {
            try
            {
                result.add(acceptor.apply(node));
            }
            catch (org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException ex)
            {
                defectRecorder.accept(ex.getMessage());
            }
        }
        return result;
    }

    protected void check(JsonNodeType expectedNode, JsonNodeType currentNode)
    {
        check(expectedNode, currentNode, "Failed to parse JSON, expected '" + expectedNode + "', Found " + currentNode);
    }

    protected void check(JsonNodeType expectedNode, JsonNodeType currentNode, String errorMessage)
    {
        check(Collections.singletonList(expectedNode), currentNode, errorMessage);
    }

    protected void check(List<JsonNodeType> expectedNodes,
                         JsonNodeType currentNode,
                         String errorMessage)
    {
        if (!expectedNodes.contains(currentNode))
        {
            throw new org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException(errorMessage);
        }
    }
}
