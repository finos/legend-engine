// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.functionActivator.metamodel.FunctionActivator;
import org.finos.legend.engine.protocol.pure.v1.ProtocolToClassifierPathLoader;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "Pure - Protocol")
@Path("pure/v1/protocol/pure")
@Produces(MediaType.APPLICATION_JSON)
public class PureProtocol
{
    @GET
    @Path("getClassifierPathMap")
    @ApiOperation(value = "Get the mapping between element protocol serialization type and element classifier path")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getClassifierPathMap()
    {
        Map<Class<? extends PackageableElement>, String> classifierPathMap = ProtocolToClassifierPathLoader.getProtocolClassToClassifierMap();
        Map<String, String> result = Maps.mutable.empty();
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    info.getSubTypes().forEach(subType ->
                    {
                        if (PackageableElement.class.isAssignableFrom(subType.getOne()) && classifierPathMap.containsKey(subType.getOne()))
                        {
                            if (result.containsKey(subType.getTwo()))
                            {
                                // ignore duplications
                                return;
                            }
                            result.put(subType.getTwo(), classifierPathMap.get(subType.getOne()));
                        }
                    });
                }));
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity("[" + result.entrySet().stream().map(entry -> "{\"type\":\"" + entry.getKey() + "\",\"classifierPath\":\"" + entry.getValue() + "\"}").collect(Collectors.joining(",")) + "]").build();
    }

    @GET
    @Path("getSubtypeInfo")
    @ApiOperation(value = "Get the protocol serialization subtype information")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response getSubtypeInfo()
    {
        SubtypeInfoResult result = new SubtypeInfoResult();
        MutableSet<String> storeSubtypes = Sets.mutable.empty();
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    info.getSubTypes().forEach(subType ->
                    {
                        if (Store.class.isAssignableFrom(subType.getOne()))
                        {
                            if (storeSubtypes.contains(subType.getTwo()))
                            {
                                // ignore duplications
                                return;
                            }
                            storeSubtypes.add(subType.getTwo());
                        }
                    });
                }));
        result.storeSubtypes = storeSubtypes.toList();

        MutableSet<String> functionActivatorSubtypes = Sets.mutable.empty();
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    info.getSubTypes().forEach(subType ->
                    {
                        if (FunctionActivator.class.isAssignableFrom(subType.getOne()))
                        {
                            if (functionActivatorSubtypes.contains(subType.getTwo()))
                            {
                                // ignore duplications
                                return;
                            }
                            functionActivatorSubtypes.add(subType.getTwo());
                        }
                    });
                }));
        result.functionActivatorSubtypes = functionActivatorSubtypes.toList();
        return Response.status(200).type(MediaType.APPLICATION_JSON).entity(result).build();
    }

    public static class SubtypeInfoResult
    {
        public List<String> storeSubtypes;
        public List<String> functionActivatorSubtypes;
    }
}

