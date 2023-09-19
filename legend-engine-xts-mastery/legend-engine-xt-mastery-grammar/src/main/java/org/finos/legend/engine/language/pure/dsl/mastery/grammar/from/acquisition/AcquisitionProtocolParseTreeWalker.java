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

import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.IMasteryParserExtension;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.acquisition.AcquisitionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.DESDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.Decryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaDataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.PGPDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AcquisitionProtocolParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors;

    public AcquisitionProtocolParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.credentialSecretProcessors = credentialSecretProcessors;
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

        // maxRetryTimeInMinutes
        AcquisitionParserGrammar.MaxRetryTimeInMinutesContext maxRetryTimeInMinutesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.maxRetryTimeInMinutes(), "maxRetryTimeMinutes", sourceInformation);
        if (maxRetryTimeInMinutesContext != null)
        {
            fileAcquisitionProtocol.maxRetryTimeInMinutes = Integer.parseInt(maxRetryTimeInMinutesContext.INTEGER().getText());
        }

        // encoding
        AcquisitionParserGrammar.EncodingContext encodingContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.encoding(), "encoding", sourceInformation);
        if (encodingContext != null)
        {
            fileAcquisitionProtocol.encoding = PureGrammarParserUtility.fromGrammarString(encodingContext.STRING().getText(), true);
        }

        // decryption
        AcquisitionParserGrammar.DecryptionContext decryptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.decryption(), "decryption", sourceInformation);
        if (decryptionContext != null)
        {
            fileAcquisitionProtocol.decryption = visitDecryption(decryptionContext);
        }

        return fileAcquisitionProtocol;
    }

    public Decryption visitDecryption(AcquisitionParserGrammar.DecryptionContext decryptionContext)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(decryptionContext);

        if (decryptionContext.desDecryption() != null)
        {
            return visitDesDecryption(decryptionContext.desDecryption(), sourceInformation);
        }

        if (decryptionContext.pgpDecryption() != null)
        {
            return visitPgpDecryption(decryptionContext.pgpDecryption(), sourceInformation);
        }

        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    public Decryption visitDesDecryption(AcquisitionParserGrammar.DesDecryptionContext desDecryptionContext, SourceInformation sourceInformation)
    {
        DESDecryption decryption = new DESDecryption();
        decryption.sourceInformation = sourceInformation;

        AcquisitionParserGrammar.DecryptionKeyContext decryptionKeyContext = PureGrammarParserUtility.validateAndExtractRequiredField(desDecryptionContext.decryptionKey(), "decryptionKey", sourceInformation);
        decryption.decryptionKey = IMasteryParserExtension.process(extraSpecificationCode(decryptionKeyContext.islandSpecification(), walkerSourceInformation), credentialSecretProcessors, "credential secret");

        AcquisitionParserGrammar.CapOptionContext capOptionContext = PureGrammarParserUtility.validateAndExtractRequiredField(desDecryptionContext.capOption(), "capOption", sourceInformation);
        decryption.capOption = evaluateBoolean(capOptionContext, (capOptionContext != null ? capOptionContext.boolean_value() : null), null);

        AcquisitionParserGrammar.UuEncodeContext uuEncodeContext = PureGrammarParserUtility.validateAndExtractRequiredField(desDecryptionContext.uuEncode(), "uuEncode", sourceInformation);
        decryption.uuEncode = evaluateBoolean(uuEncodeContext, (uuEncodeContext != null ? uuEncodeContext.boolean_value() : null), null);

        return decryption;
    }

    public Decryption visitPgpDecryption(AcquisitionParserGrammar.PgpDecryptionContext pgpDecryptionContext, SourceInformation sourceInformation)
    {
        PGPDecryption decryption = new PGPDecryption();
        decryption.sourceInformation = sourceInformation;

        AcquisitionParserGrammar.PrivateKeyContext privateKeyContext = PureGrammarParserUtility.validateAndExtractRequiredField(pgpDecryptionContext.privateKey(), "privateKey", sourceInformation);
        decryption.privateKey = IMasteryParserExtension.process(extraSpecificationCode(privateKeyContext.islandSpecification(), walkerSourceInformation), credentialSecretProcessors, "credential secret");

        AcquisitionParserGrammar.PassPhraseContext passPhraseContext = PureGrammarParserUtility.validateAndExtractRequiredField(pgpDecryptionContext.passPhrase(), "passPhrase", sourceInformation);
        decryption.passPhrase = IMasteryParserExtension.process(extraSpecificationCode(passPhraseContext.islandSpecification(), walkerSourceInformation), credentialSecretProcessors, "credential secret");

        return decryption;
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

    static SpecificationSourceCode extraSpecificationCode(AcquisitionParserGrammar.IslandSpecificationContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        StringBuilder text = new StringBuilder();
        AcquisitionParserGrammar.IslandValueContext islandValueContext = ctx.islandValue();
        if (islandValueContext != null)
        {
            for (AcquisitionParserGrammar.IslandValueContentContext fragment : islandValueContext.islandValueContent())
            {
                text.append(fragment.getText());
            }

            // prepare island grammar walker source information
            int startLine = islandValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + islandValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + islandValueContext.ISLAND_OPEN().getSymbol().getText().length();
            ParseTreeWalkerSourceInformation triggerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation triggerValueSourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), triggerValueSourceInformation, triggerValueWalkerSourceInformation);
        }
        else
        {
            SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), sourceInformation, walkerSourceInformation);
        }
    }

    private Boolean evaluateBoolean(ParserRuleContext context, AcquisitionParserGrammar.Boolean_valueContext booleanValueContext, Boolean defaultVal)
    {
        Boolean result;
        if (context == null)
        {
            result = defaultVal;
        }
        else if (booleanValueContext.TRUE() != null)
        {
            result = Boolean.TRUE;
        }
        else if (booleanValueContext.FALSE() != null)
        {
            result = Boolean.FALSE;
        }
        else
        {
            result = defaultVal;
        }
        return result;
    }
}
