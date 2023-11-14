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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.DESDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.Decryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.LegendServiceAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.PGPDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.RestAcquisitionProtocol;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_RestAcquisitionProtocol_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_file_DESDecryption_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_file_Decryption;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_file_PGPDecryption_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_FileConnection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_KafkaConnection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class HelperAcquisitionBuilder
{

    public static Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol buildAcquisition(AcquisitionProtocol acquisitionProtocol, CompileContext context)
    {


        if (acquisitionProtocol instanceof RestAcquisitionProtocol)
        {
            return new Root_meta_pure_mastery_metamodel_acquisition_RestAcquisitionProtocol_Impl("");
        }

        if (acquisitionProtocol instanceof FileAcquisitionProtocol)
        {
           return buildFileAcquisitionProtocol((FileAcquisitionProtocol) acquisitionProtocol, context);
        }

        if (acquisitionProtocol instanceof KafkaAcquisitionProtocol)
        {
            return buildKafkaAcquisitionProtocol((KafkaAcquisitionProtocol) acquisitionProtocol, context);
        }

        if (acquisitionProtocol instanceof LegendServiceAcquisitionProtocol)
        {
            return buildLegendServiceAcquisitionProtocol((LegendServiceAcquisitionProtocol) acquisitionProtocol, context);
        }

        return null;
    }

    public static Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol buildFileAcquisitionProtocol(FileAcquisitionProtocol acquisitionProtocol, CompileContext context)
    {
        validateFileAcquisitionProtocol(acquisitionProtocol);
        Root_meta_pure_mastery_metamodel_connection_FileConnection fileConnection;
        PackageableElement packageableElement = context.resolvePackageableElement(acquisitionProtocol.connection, acquisitionProtocol.sourceInformation);
        if (packageableElement instanceof Root_meta_pure_mastery_metamodel_connection_FileConnection)
        {
            fileConnection =  (Root_meta_pure_mastery_metamodel_connection_FileConnection) packageableElement;
        }
        else
        {
            throw new EngineException(format("File (HTTP or FTP) Connection '%s' is not defined", acquisitionProtocol.connection), acquisitionProtocol.sourceInformation, EngineErrorType.COMPILATION);
        }

        return new Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol_Impl("")
                ._connection(fileConnection)
                ._filePath(acquisitionProtocol.filePath)
                ._fileType(context.resolveEnumValue("meta::pure::mastery::metamodel::acquisition::file::FileType", acquisitionProtocol.fileType.name()))
                ._headerLines(acquisitionProtocol.headerLines)
                ._recordsKey(acquisitionProtocol.recordsKey)
                ._fileSplittingKeys(acquisitionProtocol.fileSplittingKeys == null ? null : Lists.fixedSize.ofAll(acquisitionProtocol.fileSplittingKeys))
                ._maxRetryTimeInMinutes(acquisitionProtocol.maxRetryTimeInMinutes == null ? null : Long.valueOf(acquisitionProtocol.maxRetryTimeInMinutes))
                ._encoding(acquisitionProtocol.encoding)
                ._decryption(acquisitionProtocol.decryption == null ? null : buildDecryption(acquisitionProtocol.decryption, context));
    }

    private static void validateFileAcquisitionProtocol(FileAcquisitionProtocol fileAcquisitionProtocol)
    {
        if (fileAcquisitionProtocol.fileType == FileType.JSON && isEmpty(fileAcquisitionProtocol.recordsKey))
        {
            throw new EngineException("'recordsKey' must be specified when file type is JSON", fileAcquisitionProtocol.sourceInformation, EngineErrorType.COMPILATION);
        }
    }


    public static Root_meta_pure_mastery_metamodel_acquisition_file_Decryption buildDecryption(Decryption decryption, CompileContext context)
    {
        if (decryption instanceof PGPDecryption)
        {
            return new Root_meta_pure_mastery_metamodel_acquisition_file_PGPDecryption_Impl("")
                        ._privateKey(HelperAuthenticationBuilder.buildCredentialSecret(((PGPDecryption) decryption).privateKey, context))
                        ._passPhrase(HelperAuthenticationBuilder.buildCredentialSecret(((PGPDecryption) decryption).passPhrase, context));
        }
        if (decryption instanceof DESDecryption)
        {
           return new Root_meta_pure_mastery_metamodel_acquisition_file_DESDecryption_Impl("")
            ._decryptionKey(HelperAuthenticationBuilder.buildCredentialSecret(((DESDecryption) decryption).decryptionKey, context))
            ._capOption(((DESDecryption) decryption).capOption)
            ._uuEncode(((DESDecryption) decryption).uuEncode);
        }
        return null;
    }

    public static Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol buildKafkaAcquisitionProtocol(KafkaAcquisitionProtocol acquisitionProtocol, CompileContext context)
    {

        Root_meta_pure_mastery_metamodel_connection_KafkaConnection kafkaConnection;
        PackageableElement packageableElement = context.resolvePackageableElement(acquisitionProtocol.connection, acquisitionProtocol.sourceInformation);
        if (packageableElement instanceof Root_meta_pure_mastery_metamodel_connection_KafkaConnection)
        {
            kafkaConnection =  (Root_meta_pure_mastery_metamodel_connection_KafkaConnection) packageableElement;
        }
        else
        {
            throw new EngineException(format("Kafka Connection '%s' is not defined", acquisitionProtocol.connection), acquisitionProtocol.sourceInformation, EngineErrorType.COMPILATION);
        }

        return new Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol_Impl("")
                ._connection(kafkaConnection)
                ._dataType(context.resolveEnumValue("meta::pure::mastery::metamodel::acquisition::kafka::KafkaDataType", acquisitionProtocol.kafkaDataType.name()))
                ._recordTag(acquisitionProtocol.recordTag);
    }

    public static Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol buildLegendServiceAcquisitionProtocol(LegendServiceAcquisitionProtocol acquisitionProtocol, CompileContext context)
    {

        return new Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol_Impl("")
                ._service(BuilderUtil.buildService(acquisitionProtocol.service, context, acquisitionProtocol.sourceInformation));
    }
}
