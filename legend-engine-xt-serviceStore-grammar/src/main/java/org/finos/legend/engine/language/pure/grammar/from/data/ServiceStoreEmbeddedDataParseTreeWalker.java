// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.data;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.ServiceStoreEmbeddedDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.contentPattern.HelperContentPatternGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.StringValuePattern;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpMethod;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class ServiceStoreEmbeddedDataParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final SourceInformation sourceInformation;
    private final PureGrammarParserExtensions extensions;

    public ServiceStoreEmbeddedDataParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.sourceInformation = sourceInformation;
        this.extensions = extensions;
    }

    public ServiceStoreEmbeddedData visit(ServiceStoreEmbeddedDataParserGrammar.DefinitionContext ctx)
    {
        ServiceStoreEmbeddedData serviceStoreEmbeddedData = new ServiceStoreEmbeddedData();
        serviceStoreEmbeddedData.sourceInformation = this.sourceInformation;
        serviceStoreEmbeddedData.serviceStubMappings = ListIterate.collect(ctx.serviceStubMappings().serviceStubMapping(), this::visitServiceStubMapping);

        return serviceStoreEmbeddedData;
    }

    private ServiceStubMapping visitServiceStubMapping(ServiceStoreEmbeddedDataParserGrammar.ServiceStubMappingContext ctx)
    {
        ServiceStubMapping serviceStubMapping = new ServiceStubMapping();
        serviceStubMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestPatternContext requestPatternContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceRequestPattern(), "request", serviceStubMapping.sourceInformation);
        ServiceStoreEmbeddedDataParserGrammar.ServiceResponseDefinitionContext responseDefinitionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceResponseDefinition(), "response", serviceStubMapping.sourceInformation);

        serviceStubMapping.requestPattern = visitServiceRequestPattern(requestPatternContext);
        serviceStubMapping.responseDefinition = visitServiceResponseDefinition(responseDefinitionContext);

        return serviceStubMapping;
    }

    private ServiceRequestPattern visitServiceRequestPattern(ServiceStoreEmbeddedDataParserGrammar.ServiceRequestPatternContext ctx)
    {
        ServiceRequestPattern serviceRequestPattern = new ServiceRequestPattern();
        serviceRequestPattern.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        //Method
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestMethodDefinitionContext methodCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceRequestMethodDefinition(), "method", serviceRequestPattern.sourceInformation);
        String providedMethod = PureGrammarParserUtility.fromIdentifier(methodCtx.identifier()).toUpperCase();
        List<String> supportedMethods = ListIterate.collect(Lists.mutable.with(HttpMethod.values()), HttpMethod::toString);
        if (!supportedMethods.contains(providedMethod))
        {
            throw new EngineException("Unsupported HTTP Method type - " + providedMethod + ". Supported types are - " + String.join(",", supportedMethods), this.walkerSourceInformation.getSourceInformation(methodCtx), EngineErrorType.PARSER);
        }
        serviceRequestPattern.method = HttpMethod.valueOf(providedMethod);

        //Url
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestUrlPatternContext urlCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceRequestUrlPattern(), "url", serviceRequestPattern.sourceInformation);
        if (urlCtx != null)
        {
            serviceRequestPattern.url = PureGrammarParserUtility.fromGrammarString(urlCtx.STRING().getText(), true);
        }

        //Url Path
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestUrlPathPatternContext urlPathCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceRequestUrlPathPattern(), "urlPath", serviceRequestPattern.sourceInformation);
        if (urlPathCtx != null)
        {
            serviceRequestPattern.urlPath = PureGrammarParserUtility.fromGrammarString(urlPathCtx.STRING().getText(), true);
        }

        // Query Parameters
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestQueryParametersPatternContext queryParamCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceRequestQueryParametersPattern(), "queryParameters", serviceRequestPattern.sourceInformation);
        if (queryParamCtx != null)
        {
            serviceRequestPattern.queryParams = Maps.mutable.empty();
            ListIterate.forEach(queryParamCtx.serviceRequestParameterPattern(), c -> {
                        Pair<String, StringValuePattern> p = visitServiceRequestParameterPattern(c);
                        if(serviceRequestPattern.queryParams.containsKey(p.getOne()))
                        {
                            throw new EngineException("Query Param : '" + p.getOne() + "' value should be defined only once", this.walkerSourceInformation.getSourceInformation(c), EngineErrorType.PARSER);
                        }
                        serviceRequestPattern.queryParams.put(p.getOne(), p.getTwo());
                    }
            );
        }

        // Header Parameters
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestHeaderParametersPatternContext headerParamCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceRequestHeaderParametersPattern(), "headerParameters", serviceRequestPattern.sourceInformation);
        if (headerParamCtx != null)
        {
            serviceRequestPattern.headerParams = Maps.mutable.empty();
            ListIterate.forEach(headerParamCtx.serviceRequestParameterPattern(), c -> {
                        Pair<String, StringValuePattern> p = visitServiceRequestParameterPattern(c);
                        if(serviceRequestPattern.headerParams.containsKey(p.getOne()))
                        {
                            throw new EngineException("Header Param : '" + p.getOne() + "' value should be defined only once", this.walkerSourceInformation.getSourceInformation(c), EngineErrorType.PARSER);
                        }
                        serviceRequestPattern.headerParams.put(p.getOne(), p.getTwo());
                    }
            );
        }

        // Body Patterns
        ServiceStoreEmbeddedDataParserGrammar.ServiceRequestBodyPatternsContext bodyCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceRequestBodyPatterns(), "bodyPatterns", serviceRequestPattern.sourceInformation);
        if (bodyCtx != null)
        {
            serviceRequestPattern.bodyPatterns = ListIterate.collect(bodyCtx.serviceRequestContentPattern(), this::visitStringValuePattern);
        }

        return serviceRequestPattern;
    }

    private ServiceResponseDefinition visitServiceResponseDefinition(ServiceStoreEmbeddedDataParserGrammar.ServiceResponseDefinitionContext ctx)
    {
        ServiceResponseDefinition serviceResponseDefinition = new ServiceResponseDefinition();
        serviceResponseDefinition.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        ServiceStoreEmbeddedDataParserGrammar.EmbeddedDataContext embeddedDataContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.embeddedData(), "body", serviceResponseDefinition.sourceInformation);
        EmbeddedData embeddedData = HelperEmbeddedDataGrammarParser.parseEmbeddedData(embeddedDataContext, walkerSourceInformation, extensions);
        if (!(embeddedData instanceof ExternalFormatData))
        {
            throw new EngineException("Service response body should be ExternalFormat Embedded Data", this.walkerSourceInformation.getSourceInformation(embeddedDataContext), EngineErrorType.PARSER);
        }
        serviceResponseDefinition.body = (ExternalFormatData) embeddedData;

        return serviceResponseDefinition;
    }

    private Pair<String, StringValuePattern> visitServiceRequestParameterPattern(ServiceStoreEmbeddedDataParserGrammar.ServiceRequestParameterPatternContext ctx)
    {
        String paramName;
        if (ctx.serviceRequestParameterName().unquotedIdentifier() != null)
        {
            paramName = PureGrammarParserUtility.fromIdentifier(ctx.serviceRequestParameterName().unquotedIdentifier());
        }
        else
        {
            paramName = PureGrammarParserUtility.fromGrammarString(ctx.serviceRequestParameterName().QUOTED_STRING().getText(), true);
        }
        StringValuePattern stringValuePattern = visitStringValuePattern(ctx.serviceRequestContentPattern());

        return Tuples.pair(paramName, stringValuePattern);
    }

    private StringValuePattern visitStringValuePattern(ServiceStoreEmbeddedDataParserGrammar.ServiceRequestContentPatternContext ctx)
    {
        return (StringValuePattern) HelperContentPatternGrammarParser.parseContentPattern(ctx, walkerSourceInformation, extensions);
    }
}
