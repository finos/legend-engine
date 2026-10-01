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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.ArrayList;
import java.util.Collection;
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
     * Parses {@code model} with the Rust parser. A result holding a section of a type that is not among
     * {@code sectionTypes}, those taken over, is unsupported: such a section is parsed as without the Rust parser.
     */
    static RustModelResult parse(RustModel model, boolean returnSourceInfo, Collection<String> sectionTypes)
    {
        String json;
        try
        {
            json = NativeGrammarParser.parseModel(model.code, model.sourceId, model.lineOffset, model.columnOffset);
        }
        catch (UnsatisfiedLinkError e)
        {
            // A library older than whole models.
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser library does not parse whole models: " + e.getMessage(), null);
        }
        if (json == null)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "the Rust parser returned no result", null);
        }
        try
        {
            JsonNode root = RustSectionResult.MAPPER.readTree(json);
            JsonNode error = root.get("error");
            if (error != null)
            {
                JsonNode si = error.get("sourceInformation");
                return error(error.path("kind").asText(RustSectionResult.INTERNAL_ERROR), error.path("message").asText(""), (si == null) ? null : RustSectionResult.MAPPER.treeToValue(si, SourceInformation.class));
            }
            String absentType = RustSectionResult.absentClassInstanceType(root.path("elements"));
            if (absentType != null)
            {
                return error(RustSectionResult.UNSUPPORTED, "no protocol extension for class instance type '" + absentType + "'", null);
            }
            ObjectMapper mapper = returnSourceInfo ? RustSectionResult.MAPPER : RustSectionResult.MAPPER_WITHOUT_SOURCE_INFORMATION;
            List<PackageableElement> elements = new ArrayList<>();
            for (JsonNode element : root.path("elements"))
            {
                elements.add(mapper.treeToValue(element, PackageableElement.class));
            }
            String notTakenOver = sectionTypeNotIn(elements, sectionTypes);
            if (notTakenOver != null)
            {
                return error(RustSectionResult.UNSUPPORTED, "the section type '" + notTakenOver + "' is not taken over", null);
            }
            return new RustModelResult(elements, null, null, null);
        }
        catch (InvalidTypeIdException e)
        {
            return error(RustSectionResult.UNSUPPORTED, "no protocol extension for type '" + e.getTypeId() + "'", null);
        }
        catch (JsonProcessingException e)
        {
            return error(RustSectionResult.INTERNAL_ERROR, "Rust protocol JSON does not match the engine protocol: " + e.getOriginalMessage(), null);
        }
    }

    /**
     * The first section type in the section index of {@code elements} that is not among {@code sectionTypes}, if any.
     */
    private static String sectionTypeNotIn(List<PackageableElement> elements, Collection<String> sectionTypes)
    {
        for (PackageableElement element : elements)
        {
            if (element instanceof SectionIndex)
            {
                for (Section section : ((SectionIndex) element).sections)
                {
                    if (!sectionTypes.contains(section.parserName))
                    {
                        return section.parserName;
                    }
                }
            }
        }
        return null;
    }

    private static RustModelResult error(String kind, String message, SourceInformation sourceInformation)
    {
        return new RustModelResult(Collections.emptyList(), kind, message, sourceInformation);
    }
}
