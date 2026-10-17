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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.UnknownClassInstanceTypeException;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Rust parser's result for a whole model, read from the JSON {@link NativeGrammarParser#parseModel} returns and
 * deserialized into protocol objects: its sections' elements, then the section index.
 */
final class RustModelResult
{
    final List<PackageableElement> elements;
    final String errorKind;
    final String errorMessage;
    final SourceInformation errorSourceInformation;

    private RustModelResult(List<PackageableElement> elements, String errorKind, String errorMessage, SourceInformation errorSourceInformation)
    {
        this.elements = elements;
        this.errorKind = errorKind;
        this.errorMessage = errorMessage;
        this.errorSourceInformation = errorSourceInformation;
    }

    boolean isError()
    {
        return this.errorKind != null;
    }

    boolean isUnsupported()
    {
        return RustSectionResult.UNSUPPORTED.equals(this.errorKind);
    }

    /**
     * The model, as protocol.
     */
    PureModelContextData toPureModelContextData()
    {
        return PureModelContextData.newBuilder().withElements(this.elements).build();
    }

    /**
     * Parses {@code model} with the Rust parser.
     */
    static RustModelResult parse(RustModel model, boolean returnSourceInfo)
    {
        byte[] json;
        try
        {
            json = NativeGrammarParser.parseModel(model.code, model.sourceId, model.lineOffset, model.columnOffset);
        }
        catch (UnsatisfiedLinkError e)
        {
            // A library older than whole models: every model falls back, as its log says.
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser library does not parse whole models (it predates them; rebuild it): " + e.getMessage(), null);
        }
        return read(json, returnSourceInfo);
    }

    /**
     * Reads the JSON {@link NativeGrammarParser#parseModel} returns.
     */
    static RustModelResult read(byte[] json, boolean returnSourceInfo)
    {
        if (json == null)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser returned no result", null);
        }
        ObjectMapper mapper = RustSectionResult.mapper(returnSourceInfo);
        try (JsonParser parser = mapper.getFactory().createParser(json))
        {
            List<PackageableElement> elements = new ArrayList<>();
            for (String field = RustSectionResult.firstField(parser); field != null; field = RustSectionResult.nextField(parser))
            {
                if ("error".equals(field))
                {
                    RustSectionResult.RustError error = RustSectionResult.RustError.read(parser);
                    return error(error.kind, error.message, error.sourceInformation);
                }
                if ("elements".equals(field))
                {
                    RustSectionResult.readElements(parser, mapper, elements);
                }
                else
                {
                    parser.skipChildren();
                }
            }
            return new RustModelResult(elements, null, null, null);
        }
        catch (UnknownClassInstanceTypeException e)
        {
            return error(RustSectionResult.UNSUPPORTED, RustSectionResult.unsupported(e), null);
        }
        catch (InvalidTypeIdException e)
        {
            return error(RustSectionResult.UNSUPPORTED, RustSectionResult.unsupported(e), null);
        }
        catch (IOException e)
        {
            return error(RustSectionResult.INTERNAL_ERROR, RustSectionResult.divergence(e), null);
        }
    }

    private static RustModelResult error(String kind, String message, SourceInformation sourceInformation)
    {
        return new RustModelResult(Collections.emptyList(), kind, message, sourceInformation);
    }
}
