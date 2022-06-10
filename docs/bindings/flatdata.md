# FlatData

Flat data is a type of Binding used to process many data formats that are not described by well-known Schemas.
Examples are:

* Comma Separated Values (CSV) and alike (`DelimitedWithHeadings` and `DelimitedWithoutHeadings`)
* Fixed Width data
* Bloomberg extracts

## Schema Syntax

See [FlatData Grammar](https://legend.finos.org/docs/studio/create-flat-data-schema#flat-data-grammar)
for an explanation of the syntax for Flat Data.

## Drivers

The code to interpret a schema for a given section is defined in its driver implementation.

Each driver provides implementations of:

`FlatDataDriverDescription` which describes the driver to the rest of the system.
`FlatDataReadDriver` to handle deserialization for the format
`FlatDataWriteDriver` to handle serialization for the format

It's not necessary to implement both `FlatDataReadDriver` and `FlatDataWriteDriver` for a 
driver (but it's meaningless to implement neither). That is a driver can support read only
or write only.

A `FlatDataProcessingContext` carries state for the processing while a driver is in use.
State consists of `FlatDataVariable`s.  Drivers of different types can use that state to
share information (such as a line number) across sections of a FlatData file/stream.
The context also provides access to object factories (`ParsedFlatDataToObject` and
`ObjectToParsedFlatData`).  The factories themselves are implemented by Java code generated
for the execution plan.

## Data Types

The Flat Data data types are common to all drivers.  Common code is therefore provided
to convert between values of those types (as represented by Java types) and textual representations
in Strings.

The correlation between Flat Data types and Java is:

| Flat Data type | Java type                                                                                      |
|----------------|------------------------------------------------------------------------------------------------|
| BOOLEAN        | `boolean`                                                                                      |
| INTEGER        | `long`                                                                                         |
| DECIMAL        | `double` or `BigDecimal` depending on whether Pure defines a `Float` or `Decimal` in the model |
| STRING         | `String`                                                                                       |
| DATE           | `LocalDate`                                                                                    |
| DATETIME       | `Instant`                                                                                      |

Note that `LocalData` and `Instant` are converted to `PureDate` for classes in generated Java.
_If the generated Java migrates to `LocalData` and `Instant` at some point this conversion can be dropped._

## Flat Data representations

There are two levels of representing data in Flat Data:

* `RawFlatData`
* `ParsedFlatData`

The former represents the data as extracted directly from the file/stream.  For example in a CSV file
(`DelimitedWithHeadings` driver) the `RawFlatData` represents the text value of each column for a row of the file.  
It will contain all the values extracted for a row even if they are not ultimately used.

The latter represents the data translated according to the record type defined for the section.  For example in
a CSV file this will be the values translated into the data types described in the record type.
It will only contain values described in the record type.

The `ParsedFlatData` is used to translate to and from the Pure model instances (as represented in the bound 
execution plan - that is in Java).  The translation is performed by the factories as mentioned in the
`FlatDataProcessingContext` above.

