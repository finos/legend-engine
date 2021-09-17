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

package org.finos.legend.engine.language.pure.dsl.dataSpace.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;
import java.util.function.Consumer;

public class DataSpaceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public DataSpaceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(DataSpaceParserGrammar.DefinitionContext ctx)
    {
        ctx.dataSpaceElement().stream().map(this::visitText).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private DataSpace visitText(DataSpaceParserGrammar.DataSpaceElementContext ctx)
    {
        DataSpace dataSpace = new DataSpace();
        dataSpace.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        dataSpace._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        dataSpace.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // Group ID
        DataSpaceParserGrammar.GroupIdContext groupIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.groupId(), "groupId", dataSpace.sourceInformation);
        dataSpace.groupId = PureGrammarParserUtility.fromGrammarString(groupIdContext.STRING().getText(), true);
        // Artifact ID
        DataSpaceParserGrammar.ArtifactIdContext artifactIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.artifactId(), "artifactId", dataSpace.sourceInformation);
        dataSpace.artifactId = PureGrammarParserUtility.fromGrammarString(artifactIdContext.STRING().getText(), true);
        // Version ID
        DataSpaceParserGrammar.VersionIdContext versionIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.versionId(), "versionId", dataSpace.sourceInformation);
        dataSpace.versionId = PureGrammarParserUtility.fromGrammarString(versionIdContext.STRING().getText(), true);
        // Mapping
        DataSpaceParserGrammar.MappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mapping(), "mapping", dataSpace.sourceInformation);
        dataSpace.mapping = PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier());
        // Runtime
        DataSpaceParserGrammar.RuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.runtime(), "runtime", dataSpace.sourceInformation);
        dataSpace.runtime = PureGrammarParserUtility.fromQualifiedName(runtimeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : runtimeContext.qualifiedName().packagePath().identifier(), runtimeContext.qualifiedName().identifier());
        // Description
        DataSpaceParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", dataSpace.sourceInformation);
        dataSpace.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;
        // Diagrams
        DataSpaceParserGrammar.DiagramsContext diagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.diagrams(), "diagrams", dataSpace.sourceInformation);
        dataSpace.diagrams = diagramsContext != null ? ListIterate.collect(diagramsContext.qualifiedName(), diagramPathContext -> PureGrammarParserUtility.fromQualifiedName(diagramPathContext.packagePath() == null ? Collections.emptyList() : diagramPathContext.packagePath().identifier(), diagramPathContext.identifier())) : null;
        // Support Email
        DataSpaceParserGrammar.SupportEmailContext supportEmailContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.supportEmail(), "supportEmail", dataSpace.sourceInformation);
        dataSpace.supportEmail = supportEmailContext != null ? PureGrammarParserUtility.fromGrammarString(supportEmailContext.STRING().getText(), true) : null;
        return dataSpace;
    }
}
