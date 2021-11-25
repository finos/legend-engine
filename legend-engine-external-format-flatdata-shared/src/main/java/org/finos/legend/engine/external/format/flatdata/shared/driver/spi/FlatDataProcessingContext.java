package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;

public interface FlatDataProcessingContext
{
    String getDefiningPath();

    Connection getConnection();

    boolean hasVariableValue(String variableName);

    <X> X getVariableValue(String variableName);

    <X> X getVariableValue(String variableName, X defaultValue);

    <X> X setVariableValue(String variableName, X value);

    boolean isNextSectionReadyToStartAt(Cursor cursor);

    <T> ParsedFlatDataToObject<? extends T> createToObjectFactory(FlatDataRecordType recordType);

    <T> ObjectToParsedFlatData<? extends T> createFromObjectFactory(FlatDataRecordType recordType);
}
