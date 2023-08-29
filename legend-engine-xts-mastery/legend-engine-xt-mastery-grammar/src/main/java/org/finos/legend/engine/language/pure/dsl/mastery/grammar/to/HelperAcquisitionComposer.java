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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.LegendServiceAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.RestAcquisitionProtocol;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperAcquisitionComposer
{

    public static String renderAcquisition(AcquisitionProtocol acquisitionProtocol, int indentLevel, PureGrammarComposerContext context)
    {

        if (acquisitionProtocol instanceof RestAcquisitionProtocol)
        {
            return "REST;\n";
        }

        if (acquisitionProtocol instanceof FileAcquisitionProtocol)
        {
           return renderFileAcquisitionProtocol((FileAcquisitionProtocol) acquisitionProtocol, indentLevel, context);
        }

        if (acquisitionProtocol instanceof KafkaAcquisitionProtocol)
        {
            return renderKafkaAcquisitionProtocol((KafkaAcquisitionProtocol) acquisitionProtocol, indentLevel, context);
        }

        if (acquisitionProtocol instanceof LegendServiceAcquisitionProtocol)
        {
            return renderLegendServiceAcquisitionProtocol((LegendServiceAcquisitionProtocol) acquisitionProtocol);
        }

        return null;
    }

    public static String renderFileAcquisitionProtocol(FileAcquisitionProtocol acquisitionProtocol, int indentLevel, PureGrammarComposerContext context)
    {

        return "File #{\n" +
                getTabString(indentLevel + 2) + "fileType: " + acquisitionProtocol.fileType.name() + ";\n" +
                getTabString(indentLevel + 2) + "filePath: " + convertString(acquisitionProtocol.filePath, true) + ";\n" +
                getTabString(indentLevel + 2) + "headerLines: " + acquisitionProtocol.headerLines + ";\n" +
                (acquisitionProtocol.recordsKey == null ? "" : getTabString(indentLevel + 2) + "recordsKey: " + convertString(acquisitionProtocol.recordsKey, true) + ";\n") +
                renderFileSplittingKeys(acquisitionProtocol.fileSplittingKeys, indentLevel + 2) +
                getTabString(indentLevel + 2) + "connection: " + convertPath(acquisitionProtocol.connection) + ";\n" +
                getTabString(indentLevel + 1) + "}#;\n";
    }

    public static String renderKafkaAcquisitionProtocol(KafkaAcquisitionProtocol acquisitionProtocol, int indentLevel, PureGrammarComposerContext context)
    {

        return "Kafka #{\n" +
                getTabString(indentLevel + 2) + "dataType: " + acquisitionProtocol.kafkaDataType.name() + ";\n" +
                getTabString(indentLevel + 2) + "connection: " + convertPath(acquisitionProtocol.connection) + ";\n" +
                (acquisitionProtocol.recordTag == null ? "" : getTabString(indentLevel + 2) + "recordTag: " + convertString(acquisitionProtocol.recordTag, true) + ";\n") +
                getTabString(indentLevel + 1) + "}#;\n";
    }

    public static String renderLegendServiceAcquisitionProtocol(LegendServiceAcquisitionProtocol acquisitionProtocol)
    {

        return acquisitionProtocol.service + ";\n";
    }

    private static String renderFileSplittingKeys(List<String> fileSplittingKeys, int indentLevel)
    {

        if (fileSplittingKeys == null || fileSplittingKeys.isEmpty())
        {
            return "";
        }

        return getTabString(indentLevel) + "fileSplittingKeys: [ "
                + String.join(", ", ListIterate.collect(fileSplittingKeys, key -> convertString(key, true)))
                + " ];\n";
    }
}
