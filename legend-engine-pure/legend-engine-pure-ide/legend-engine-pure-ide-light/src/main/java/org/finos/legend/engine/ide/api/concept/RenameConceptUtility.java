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

package org.finos.legend.engine.ide.api.concept;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameConceptUtility
{
    private static final String CONCRETE_FUNCTION_DEFINITION = "ConcreteFunctionDefinition";
    private static final Pattern IDENTIFIER_REGEX = Pattern.compile("^(\\w[\\w$]*+)");
    private static final Pattern SIGNATURE_SUFFIX_REGEX = Pattern.compile("_[\\w$]+_");

    private RenameConceptUtility()
    {
    }

    public static MutableList<? extends org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry> removeInvalidReplaceConceptEntries(final String[] sourceCodeLines, MutableList<? extends org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry> entries)
    {
        return entries.select(new Predicate<org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry>()
        {
            @Override
            public boolean accept(org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry entry)
            {
                if (entry.getConceptColumnIndex() < 0 || entry.getConceptColumnIndex() >= sourceCodeLines[entry.getConceptLineIndex()].length())
                {
                    return false;
                }
                Matcher matcher = IDENTIFIER_REGEX.matcher(sourceCodeLines[entry.getConceptLineIndex()].substring(entry.getConceptColumnIndex()));
                if (matcher.find())
                {
                    String identifierAtThisPoint = matcher.group(1);
                    boolean isExactMatch = identifierAtThisPoint.equals(entry.getConceptName());
                    boolean isSignatureOfOriginal = CONCRETE_FUNCTION_DEFINITION.equals(entry.getConceptType()) && identifierAtThisPoint.startsWith(entry.getConceptName()) && SIGNATURE_SUFFIX_REGEX.matcher(identifierAtThisPoint.substring(entry.getConceptName().length())).matches();
                    return isExactMatch || isSignatureOfOriginal;
                }
                return false;
            }
        });
    }

    public static String replace(String[] sourceCodeLines, MutableList<? extends org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry> entries)
    {
        entries.sortThisBy(org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry::getReplaceColumnIndex).sortThisBy(org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry::getReplaceLineIndex);
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        for (int lineIndex = 0; lineIndex < sourceCodeLines.length; ++lineIndex)
        {
            int columnIndex = 0;
            while (counter < entries.size() && entries.get(counter).getReplaceLineIndex() == lineIndex)
            {
                AbstractRenameConceptEntry entry = entries.get(counter);
                stringBuilder.append(sourceCodeLines[lineIndex], columnIndex, entry.getReplaceColumnIndex()).append(entry.getNewReplaceString());
                columnIndex = entry.getReplaceColumnIndex() + entry.getOriginalReplaceString().length();
                counter++;
            }
            stringBuilder.append(sourceCodeLines[lineIndex].substring(columnIndex)).append("\n");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public static class RenameConceptInputSourceInformation
    {
        public String sourceId;
        public int line;
        public int column;
    }
}
