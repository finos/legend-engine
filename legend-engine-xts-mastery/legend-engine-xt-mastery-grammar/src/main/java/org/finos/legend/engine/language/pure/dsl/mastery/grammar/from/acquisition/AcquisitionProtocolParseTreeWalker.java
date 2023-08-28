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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.acquisition;

import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.acquisition.AcquisitionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaDataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.LegendServiceAcquisitionProtocol;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

public class AcquisitionProtocolParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public AcquisitionProtocolParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public AcquisitionProtocol visitAcquisitionProtocol(AcquisitionParserGrammar ctx)
    {

        AcquisitionParserGrammar.DefinitionContext definitionContext = ctx.definition();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(definitionContext);

        if (definitionContext.fileAcquisition() != null)
        {
           return visitFileAcquisitionProtocol(definitionContext.fileAcquisition());
        }

        if (definitionContext.kafkaAcquisition() != null)
        {
            return visitKafkaAcquisitionProtocol(definitionContext.kafkaAcquisition());
        }

        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    public FileAcquisitionProtocol visitFileAcquisitionProtocol(AcquisitionParserGrammar.FileAcquisitionContext ctx)
    {


        FileAcquisitionProtocol fileAcquisitionProtocol = new FileAcquisitionProtocol();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // connection
        AcquisitionParserGrammar.ConnectionContext connectionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connection(), "connection", sourceInformation);
        fileAcquisitionProtocol.connection = PureGrammarParserUtility.fromQualifiedName(connectionContext.qualifiedName().packagePath() == null ? Collections.emptyList() : connectionContext.qualifiedName().packagePath().identifier(), connectionContext.qualifiedName().identifier());

        // file Path
        AcquisitionParserGrammar.FilePathContext filePathContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.filePath(), "filePath", sourceInformation);
        fileAcquisitionProtocol.filePath = PureGrammarParserUtility.fromGrammarString(filePathContext.STRING().getText(), true);

        // file type
        AcquisitionParserGrammar.FileTypeContext fileTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.fileType(), "fileType", sourceInformation);
        String fileTypeString = fileTypeContext.fileTypeValue().getText();
        fileAcquisitionProtocol.fileType = FileType.valueOf(fileTypeString);

        // header lines
        AcquisitionParserGrammar.HeaderLinesContext headerLinesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.headerLines(), "headerLines", sourceInformation);
        fileAcquisitionProtocol.headerLines =  Integer.parseInt(headerLinesContext.INTEGER().getText());

        // file splitting keys
        AcquisitionParserGrammar.FileSplittingKeysContext fileSplittingKeysContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.fileSplittingKeys(), "fileSplittingKeys", sourceInformation);
        if (fileSplittingKeysContext != null)
        {
            fileAcquisitionProtocol.fileSplittingKeys = ListIterate.collect(fileSplittingKeysContext.STRING(), key -> PureGrammarParserUtility.fromGrammarString(key.getText(), true));
        }

        // recordsKey
        AcquisitionParserGrammar.RecordsKeyContext recordsKeyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.recordsKey(), "recordsKey", sourceInformation);
        if (recordsKeyContext != null)
        {
            fileAcquisitionProtocol.recordsKey = PureGrammarParserUtility.fromGrammarString(recordsKeyContext.STRING().getText(), true);
        }

        return fileAcquisitionProtocol;
    }

    public KafkaAcquisitionProtocol visitKafkaAcquisitionProtocol(AcquisitionParserGrammar.KafkaAcquisitionContext ctx)
    {

        KafkaAcquisitionProtocol kafkaAcquisitionProtocol = new KafkaAcquisitionProtocol();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // connection
        AcquisitionParserGrammar.ConnectionContext connectionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connection(), "connection", sourceInformation);
        kafkaAcquisitionProtocol.connection = PureGrammarParserUtility.fromQualifiedName(connectionContext.qualifiedName().packagePath() == null ? Collections.emptyList() : connectionContext.qualifiedName().packagePath().identifier(), connectionContext.qualifiedName().identifier());

        // data type
        AcquisitionParserGrammar.DataTypeContext dataTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dataType(), "dataType", sourceInformation);
        kafkaAcquisitionProtocol.kafkaDataType = KafkaDataType.valueOf(dataTypeContext.kafkaTypeValue().getText());

        // record tag
        AcquisitionParserGrammar.RecordTagContext recordTagContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.recordTag(), "recordTag", sourceInformation);

        if (recordTagContext != null)
        {
            kafkaAcquisitionProtocol.recordTag = PureGrammarParserUtility.fromGrammarString(recordTagContext.STRING().getText(), true);
        }

        return kafkaAcquisitionProtocol;
    }
}
