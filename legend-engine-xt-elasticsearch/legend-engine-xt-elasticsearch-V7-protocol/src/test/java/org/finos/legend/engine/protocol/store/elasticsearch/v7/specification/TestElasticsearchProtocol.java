//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.protocol.store.elasticsearch.v7.specification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.ResponseBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequestBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequestBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.types.IndexSettings;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.CoordsGeoBounds;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.FieldSort;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.FieldValue;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.GeoBounds;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.GeoHashLocation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.GeoLocation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.LatLonGeoLocation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.SortCombinations;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.SortOptions;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.SortOrder;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.Time;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.TopLeftBottomRightGeoBounds;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.TopRightBottomLeftGeoBounds;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.WktGeoBounds;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.Aggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AggregationContainer;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.Buckets;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.CompositeAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.CompositeBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MaxAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MaxAggregation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.SamplerAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.SignificantStringTermsAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.SumAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.IntegerNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.KeywordProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.Property;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.TextProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.TypeMapping;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.querydsl.DateDecayFunction;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.querydsl.DecayFunction;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.querydsl.DecayPlacement;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.querydsl.GeoDecayFunction;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.querydsl.NumericDecayFunction;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestElasticsearchProtocol
{
    private static void assertWithElasticsearchJsonRoundTrip(Object object, TypeReference<?> typeReference, String expectedJson) throws JsonProcessingException
    {
        String jsonFirstPass = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writeValueAsString(object);
        Object objectFromJson = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.readValue(jsonFirstPass, typeReference);
        Assert.assertEquals("Cannot deserialize to original object", jsonFirstPass, ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writeValueAsString(objectFromJson));
        MatcherAssert.assertThat("json does not match expected es json", jsonFirstPass, JsonMatchers.jsonEquals(expectedJson));
    }

    private static void assertWithElasticsearchJsonRoundTrip(Object object, String expectedJson) throws JsonProcessingException
    {
        Class<?> aClass = object.getClass();
        Assert.assertTrue(aClass.getName().startsWith("org.finos.legend.engine.protocol.store.elasticsearch.v7.specification"));
        String jsonFirstPass = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writeValueAsString(object);
        Object objectFromJson = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.readValue(jsonFirstPass, aClass);
        Assert.assertEquals("Cannot deserialize to original object", jsonFirstPass, ElasticsearchObjectMapperProvider.OBJECT_MAPPER.writeValueAsString(objectFromJson));
        MatcherAssert.assertThat("json does not match expected es json", jsonFirstPass, JsonMatchers.jsonEquals(expectedJson));
    }

    @Test
    public void testDictionariesAsMaps() throws JsonProcessingException
    {
        AggregationContainer value = new AggregationContainer();
        value.max = new MaxAggregation();
        value.max.field = LiteralOrExpression.literal("price");

        SearchRequestBody searchRequestBody = new SearchRequestBody();
        searchRequestBody.aggregations = Collections.singletonMap("max_price", value);

        String expectedJson =
                "{\n" +
                        "    \"aggregations\" : {\n" +
                        "        \"max_price\" : { \"max\" : { \"field\" : \"price\" } }\n" +
                        "    }\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(searchRequestBody, expectedJson);
    }

    @Test
    public void testExternallyTaggedVariants() throws JsonProcessingException
    {
        // externally tagged variants are ones with 'type#name' in keys, like aggregation results
        Aggregate value = new Aggregate();
        value.max = new MaxAggregate();
        value.max.value = LiteralOrExpression.literal(200.0);

        ResponseBody<Object> responseBody = new ResponseBody<>();
        responseBody.aggregations = Collections.singletonMap("max_price", value);
        responseBody.took = LiteralOrExpression.literal(0L);
        responseBody.timed_out = LiteralOrExpression.literal(false);

        String expectedJson =
                "{\n" +
                        "   \"aggregations\":\n" +
                        "   {\n" +
                        "       \"max#max_price\":\n" +
                        "       {\n" +
                        "           \"value\":200.0\n" +
                        "       }\n" +
                        "   },\n" +
                        "   \"timed_out\":false,\n" +
                        "   \"took\":0\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(responseBody, expectedJson);
    }

    @Test
    public void testInternallyTaggedVariants() throws JsonProcessingException
    {
        // internally tagged variants are ones with 'type' on the json, like Property
        Property ageValue = new Property();
        ageValue.integer = new IntegerNumberProperty();
        ageValue.integer.type = "integer";

        Property emailValue = new Property();
        emailValue.keyword = new KeywordProperty();
        emailValue.keyword.type = "keyword";

        Property nameValue = new Property();
        nameValue.text = new TextProperty();
        nameValue.text.type = "text";

        TypeMapping mappings = new TypeMapping();
        mappings.properties = Maps.fixedSize.of(
                "age", ageValue,
                "email", emailValue,
                "name", nameValue
        );

        CreateRequestBody createRequest = new CreateRequestBody();
        createRequest.mappings = mappings;

        String expectedJson =
                "{\n" +
                        "  \"mappings\": {\n" +
                        "    \"properties\": {\n" +
                        "      \"age\":    { \"type\": \"integer\" },  \n" +
                        "      \"email\":  { \"type\": \"keyword\"  }, \n" +
                        "      \"name\":   { \"type\": \"text\"  }     \n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(createRequest, expectedJson);
    }

    @Test
    public void testAdditionalProperty() throws JsonProcessingException
    {
        SortCombinations postDateSort = new SortCombinations();
        postDateSort.options = new SortOptions();
        FieldSort fieldSort = new FieldSort();
        fieldSort.order = SortOrder.asc;
        fieldSort.format = LiteralOrExpression.literal("strict_date_optional_time_nanos");
        postDateSort.options.__additionalProperty.put("post_date", fieldSort);

        SearchRequestBody searchRequestBody = new SearchRequestBody();
        searchRequestBody.sort = Collections.singletonList(postDateSort);

        String expectedJson =
                "{\n" +
                        "  \"sort\" : [\n" +
                        "    { \"post_date\" : {\"order\" : \"asc\", \"format\": \"strict_date_optional_time_nanos\"}}\n" +
                        "  ]\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(searchRequestBody, expectedJson);
    }

    @Test
    public void testAdditionalProperties() throws JsonProcessingException
    {
        IndexSettings indexSettings = new IndexSettings();
        indexSettings.analyze_max_token_count = LiteralOrExpression.literal(5L);
        indexSettings.__additionalProperties.put("prop1", LiteralOrExpression.literal("value1"));
        indexSettings.__additionalProperties.put("prop2", LiteralOrExpression.literal(1234));

        String expectedJson =
                "{\n" +
                        "   \"analyze.max_token_count\":5," +
                        "   \"prop1\":\"value1\",\n" +
                        "   \"prop2\":1234\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(indexSettings, expectedJson);
    }

    @Test
    public void testTaggedUnionAllPrimitives() throws JsonProcessingException
    {
        FieldValue fieldValue1 = new FieldValue();
        fieldValue1._boolean = LiteralOrExpression.literal(true);

        FieldValue fieldValue2 = new FieldValue();
        fieldValue2._double = LiteralOrExpression.literal(1.2);

        FieldValue fieldValue3 = new FieldValue();
        fieldValue3._long = LiteralOrExpression.literal(123L);

        FieldValue fieldValue4 = new FieldValue();
        fieldValue4.string = LiteralOrExpression.literal("hello");

        FieldValue fieldValue5 = new FieldValue();
        fieldValue5.any = LiteralOrExpression.literal(Collections.singletonMap("hello", "world"));

        List<FieldValue> fieldValues = Arrays.asList(
                fieldValue1,
                fieldValue2,
                fieldValue3,
                fieldValue4,
                fieldValue5
        );

        String expectedJson = "[ true, 1.2, 123, \"hello\", {\"hello\":\"world\"} ]";

        assertWithElasticsearchJsonRoundTrip(fieldValues, new TypeReference<List<FieldValue>>()
        {
        }, expectedJson);
    }

    @Test
    public void testTaggedUnion() throws JsonProcessingException
    {
        GeoBounds geoBounds1 = new GeoBounds();
        geoBounds1.coords = new CoordsGeoBounds();
        geoBounds1.coords.top = LiteralOrExpression.literal(1.0);
        geoBounds1.coords.bottom = LiteralOrExpression.literal(-1.0);
        geoBounds1.coords.left = LiteralOrExpression.literal(-1.1);
        geoBounds1.coords.right = LiteralOrExpression.literal(1.1);

        GeoBounds geoBounds2 = new GeoBounds();
        geoBounds2.tlbr = new TopLeftBottomRightGeoBounds();
        geoBounds2.tlbr.top_left = new GeoLocation();
        geoBounds2.tlbr.top_left.coords = Lists.mutable.of(1.0, -1.0, 2.0, -2.0).collect(value -> LiteralOrExpression.literal(value));
        geoBounds2.tlbr.bottom_right = new GeoLocation();
        geoBounds2.tlbr.bottom_right.text = LiteralOrExpression.literal("geo-loc");

        GeoBounds geoBounds3 = new GeoBounds();
        geoBounds3.trbl = new TopRightBottomLeftGeoBounds();
        geoBounds3.trbl.top_right = new GeoLocation();
        geoBounds3.trbl.top_right.geohash = new GeoHashLocation();
        geoBounds3.trbl.top_right.geohash.geohash = LiteralOrExpression.literal("hash123");
        geoBounds3.trbl.bottom_left = new GeoLocation();
        geoBounds3.trbl.bottom_left.latlon = new LatLonGeoLocation();
        geoBounds3.trbl.bottom_left.latlon.lat = LiteralOrExpression.literal(1.2);
        geoBounds3.trbl.bottom_left.latlon.lon = LiteralOrExpression.literal(13.0);

        GeoBounds geoBounds4 = new GeoBounds();
        geoBounds4.wkt = new WktGeoBounds();
        geoBounds4.wkt.wkt = LiteralOrExpression.literal("wkt");

        List<GeoBounds> geoBounds = Arrays.asList(
                geoBounds1,
                geoBounds2,
                geoBounds3,
                geoBounds4
        );

        String expectedJson =
                "[\n" +
                        "  {\n" +
                        "    \"bottom\": -1.0,\n" +
                        "    \"left\": -1.1,\n" +
                        "    \"right\": 1.1,\n" +
                        "    \"top\": 1.0\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"bottom_right\": \"geo-loc\",\n" +
                        "    \"top_left\": [\n" +
                        "      1.0,\n" +
                        "      -1.0,\n" +
                        "      2.0,\n" +
                        "      -2.0\n" +
                        "    ]\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"bottom_left\": {\n" +
                        "      \"lat\": 1.2,\n" +
                        "      \"lon\": 13.0\n" +
                        "    },\n" +
                        "    \"top_right\": {\n" +
                        "      \"geohash\": \"hash123\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"wkt\": \"wkt\"\n" +
                        "  }\n" +
                        "]";

        assertWithElasticsearchJsonRoundTrip(geoBounds, new TypeReference<List<GeoBounds>>()
        {
        }, expectedJson);
    }

    @Test
    @Ignore("deserializer cannot deduce the actual type since not enough distinct properties to to distinguish them")
    public void testTaggedUnionWithAdditionalProperties() throws JsonProcessingException
    {
        DecayFunction decayFunction1 = new DecayFunction();
        decayFunction1.date = new DateDecayFunction();
        DecayPlacement<LiteralOrExpression<String>, Time> decayPlacement1 = new DecayPlacement<>();
        decayPlacement1.origin = LiteralOrExpression.literal("0");
        decayPlacement1.scale = new Time();
        decayPlacement1.scale.offset = LiteralOrExpression.literal(1234L);
        decayFunction1.date.__additionalProperty.put("field", decayPlacement1);

        DecayFunction decayFunction2 = new DecayFunction();
        decayFunction2.numeric = new NumericDecayFunction();
        DecayPlacement<LiteralOrExpression<Number>, LiteralOrExpression<Number>> decayPlacement2 = new DecayPlacement<>();
        decayPlacement2.origin = LiteralOrExpression.literal(1.2);
        decayPlacement2.scale = LiteralOrExpression.literal(1.3);
        decayFunction2.numeric.__additionalProperty.put("field2", decayPlacement2);

        DecayFunction decayFunction3 = new DecayFunction();
        decayFunction3.geo = new GeoDecayFunction();
        DecayPlacement<GeoLocation, LiteralOrExpression<String>> decayPlacement3 = new DecayPlacement<>();
        decayPlacement3.origin = new GeoLocation();
        decayPlacement3.origin.text = LiteralOrExpression.literal("geo-location");
        decayPlacement3.scale = LiteralOrExpression.literal("scale");
        decayFunction3.geo.__additionalProperty.put("field3", decayPlacement3);

        List<DecayFunction> decayFunctions = Arrays.asList(
                decayFunction1,
                decayFunction2,
                decayFunction3
        );

        String expectedJson =
                "[\n" +
                        "  {\n" +
                        "    \"field\": {\n" +
                        "      \"origin\": \"0\",\n" +
                        "      \"scale\": 1234\n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"field2\": {\n" +
                        "      \"origin\": 1.2,\n" +
                        "      \"scale\": 1.3\n" +
                        "    }\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"field3\": {\n" +
                        "      \"origin\": \"geo-location\",\n" +
                        "      \"scale\": \"scale\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        assertWithElasticsearchJsonRoundTrip(decayFunctions, new TypeReference<List<DecayFunction>>()
        {
        }, expectedJson);
    }

    @Test
    public void testAdditionalPropertiesWithExternallyTaggedType() throws JsonProcessingException
    {
        ResponseBody<Object> responseBody = new ResponseBody<>();
        responseBody.took = LiteralOrExpression.literal(0L);
        responseBody.timed_out = LiteralOrExpression.literal(false);

        Aggregate aggregate = new Aggregate();
        aggregate.sampler = new SamplerAggregate();
        aggregate.sampler.doc_count = LiteralOrExpression.literal(200L);

        responseBody.aggregations.put("sample", aggregate);

        Aggregate innerAggregate = new Aggregate();
        innerAggregate.sigsterms = new SignificantStringTermsAggregate();
        innerAggregate.sigsterms.doc_count = LiteralOrExpression.literal(200L);
        innerAggregate.sigsterms.bg_count = LiteralOrExpression.literal(650L);

        aggregate.sampler.__additionalProperties.put("keywords", innerAggregate);

        String expectedJson =
                "{\n" +
                        "  \"aggregations\": {\n" +
                        "    \"sampler#sample\": {\n" +
                        "      \"doc_count\": 200,\n" +
                        "      \"sigsterms#keywords\": {\n" +
                        "        \"bg_count\": 650,\n" +
                        "        \"doc_count\": 200\n" +
                        "      }\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"timed_out\": false,\n" +
                        "  \"took\": 0\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(responseBody, expectedJson);
    }

    @Test
    public void testCompositeArrayBucketsAggregate() throws JsonProcessingException
    {
        ResponseBody<Object> responseBody = new ResponseBody<>();
        responseBody.took = LiteralOrExpression.literal(0L);
        responseBody.timed_out = LiteralOrExpression.literal(false);

        Aggregate aggregate = new Aggregate();
        responseBody.aggregations.put("agg", aggregate);

        aggregate.composite = new CompositeAggregate();
        aggregate.composite.buckets = new Buckets<>();
        aggregate.composite.buckets.array = new ArrayList<>();

        CompositeBucket compositeBucket = new CompositeBucket();
        compositeBucket.doc_count = LiteralOrExpression.literal(0L);
        aggregate.composite.buckets.array.add(compositeBucket);

        FieldValue keyValue = new FieldValue();
        compositeBucket.key.put("field", keyValue);
        keyValue.string = LiteralOrExpression.literal("value");

        Aggregate otherAgg = new Aggregate();
        compositeBucket.__additionalProperties.put("otherAgg", otherAgg);
        otherAgg.sum = new SumAggregate();
        otherAgg.sum.value = LiteralOrExpression.literal(1234.0);

        String expectedJson =
                "{\n" +
                        " \"aggregations\": {\n" +
                        "  \"composite#agg\": {\n" +
                        "   \"buckets\": [\n" +
                        "    {\n" +
                        "     \"doc_count\": 0,\n" +
                        "     \"key\": {\n" +
                        "      \"field\": \"value\"\n" +
                        "     },\n" +
                        "     \"sum#otherAgg\": {\n" +
                        "      \"value\": 1234.0\n" +
                        "     }\n" +
                        "    }\n" +
                        "   ]\n" +
                        "  }\n" +
                        " },\n" +
                        " \"timed_out\": false,\n" +
                        " \"took\": 0\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(responseBody, expectedJson);
    }

    @Test
    public void testCompositeKeyedBucketsAggregate() throws JsonProcessingException
    {
        ResponseBody<Object> responseBody = new ResponseBody<>();
        responseBody.took = LiteralOrExpression.literal(0L);
        responseBody.timed_out = LiteralOrExpression.literal(false);

        Aggregate aggregate = new Aggregate();
        responseBody.aggregations.put("agg", aggregate);

        aggregate.composite = new CompositeAggregate();
        aggregate.composite.buckets = new Buckets<>();

        CompositeBucket compositeBucket = new CompositeBucket();
        compositeBucket.doc_count = LiteralOrExpression.literal(0L);
        aggregate.composite.buckets.keyed.put("keyedBucket", compositeBucket);

        FieldValue keyValue = new FieldValue();
        compositeBucket.key.put("field", keyValue);
        keyValue.string = LiteralOrExpression.literal("value");

        Aggregate otherAgg = new Aggregate();
        compositeBucket.__additionalProperties.put("otherAgg", otherAgg);
        otherAgg.sum = new SumAggregate();
        otherAgg.sum.value = LiteralOrExpression.literal(1234.0);

        String expectedJson =
                "{\n" +
                        " \"aggregations\": {\n" +
                        "  \"composite#agg\": {\n" +
                        "   \"buckets\": {\n" +
                        "    \"keyedBucket\": {\n" +
                        "     \"doc_count\": 0,\n" +
                        "     \"key\": {\n" +
                        "      \"field\": \"value\"\n" +
                        "     },\n" +
                        "     \"sum#otherAgg\": {\n" +
                        "      \"value\": 1234.0\n" +
                        "     }\n" +
                        "    }\n" +
                        "   }\n" +
                        "  }\n" +
                        " },\n" +
                        " \"timed_out\": false,\n" +
                        " \"took\": 0\n" +
                        "}";

        assertWithElasticsearchJsonRoundTrip(responseBody, expectedJson);
    }
}
