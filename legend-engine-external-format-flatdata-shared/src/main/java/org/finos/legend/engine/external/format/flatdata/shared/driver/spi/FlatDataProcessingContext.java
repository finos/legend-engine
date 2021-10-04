package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;

public interface FlatDataProcessingContext<T>
{
    String getDefiningPath();

    Connection getConnection();

    boolean hasVariableValue(String variableName);

    <X> X getVariableValue(String variableName);

    <X> X getVariableValue(String variableName, X defaultValue);

    <X> X setVariableValue(String variableName, X value);

    boolean isNextSectionReadyToStartAt(Cursor cursor);

    ParsedFlatDataToObject<? extends T> createToObjectFactory(FlatDataRecordType recordType);

    ObjectToParsedFlatData<? extends T> createFromObjectFactory(FlatDataRecordType recordType);
}
