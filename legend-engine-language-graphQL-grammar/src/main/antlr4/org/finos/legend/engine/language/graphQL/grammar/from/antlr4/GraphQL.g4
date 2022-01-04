/*
 The MIT License (MIT)

 Copyright (c) 2015 Joseph T. McBride

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute,
 sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or
 substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 GraphQL grammar derived from:

 GraphQL Draft Specification - July 2015

 http://facebook.github.io/graphql/ https://github.com/facebook/graphql

 AB:10-sep19: replaced type with type_ to resolve conflict for golang generator

 AB: 13-oct-19: added type system as per June 2018 specs
 AB: 26-oct-19: added ID type
 AB: 30-Oct-19: description, boolean, schema & Block string fix.
     now parses: https://raw.githubusercontent.com/graphql-cats/graphql-cats/master/scenarios/validation/validation.schema.graphql

 */
grammar GraphQL;

//https://spec.graphql.org/June2018/#sec-Language.Document
document: definition+ EOF;

definition:
	executableDefinition
	| typeSystemDefinition
	| typeSystemExtension;

//https://spec.graphql.org/June2018/#ExecutableDefinition
executableDefinition: operationDefinition | fragmentDefinition;

//https://spec.graphql.org/June2018/#sec-Language.Operations
operationDefinition:
	operationType name? variableDefinitions? directives? selectionSet
	| selectionSet
	;

operationType: QUERY | MUTATION | SUBSCRIPTION;

//https://spec.graphql.org/June2018/#sec-Selection-Sets
selectionSet: '{' selection+ '}';

selection: field
    | fragmentSpread
    | inlineFragment
    ;
//https://spec.graphql.org/June2018/#sec-Language.Fields
field: alias? name arguments? directives? selectionSet?;

//https://spec.graphql.org/June2018/#sec-Language.Arguments
arguments: '(' argument+ ')';
argument: name ':' value;

//https://spec.graphql.org/June2018/#sec-Field-Alias
alias: name ':';

//https://spec.graphql.org/June2018/#sec-Language.Fragments
fragmentSpread: '...' fragmentName directives?;
fragmentDefinition:
	FRAGMENT fragmentName typeCondition directives? selectionSet;
fragmentName: name; // except on

//https://spec.graphql.org/June2018/#sec-Type-Conditions
typeCondition: 'on' namedType;

//https://spec.graphql.org/June2018/#sec-Inline-Fragments
inlineFragment:	'...' typeCondition? directives? selectionSet;

//https://spec.graphql.org/June2018/#sec-Input-Values
value:
	 variable
	| intValue
	| floatValue
	| stringValue
	| booleanValue
	| nullValue
	| enumValue
	| listValue
	| objectValue
   ;

//https://spec.graphql.org/June2018/#sec-Int-Value
intValue: INT;

//https://spec.graphql.org/June2018/#sec-Float-Value
floatValue: FLOAT;

//https://spec.graphql.org/June2018/#sec-Boolean-Value
booleanValue
	:	TRUE
	|	FALSE
	;

//https://spec.graphql.org/June2018/#sec-String-Value
stringValue : STRING | BLOCK_STRING;

//https://spec.graphql.org/June2018/#sec-Null-Value
nullValue: 'null';

//https://spec.graphql.org/June2018/#sec-Enum-Value
enumValue: name; //{ not (nullValue | booleanValue) };

//https://spec.graphql.org/June2018/#sec-List-Value
listValue: '[' ']'
    | '[' value+ ']'
    ;

//https://spec.graphql.org/June2018/#sec-Input-Object-Values
objectValue: '{' objectField* '}';

objectField: name ':' value;

//https://spec.graphql.org/June2018/#sec-Language.Variables
variable: '$' name;
variableDefinitions: '(' variableDefinition+ ')';
variableDefinition: variable ':' type_ defaultValue?;
defaultValue: '=' value;

//https://spec.graphql.org/June2018/#sec-Type-References
type_: namedType '!'?
    | listType '!'?
    ;

namedType: name;
listType: '[' type_ ']';


//https://spec.graphql.org/June2018/#sec-Language.Directives
directives:  directive+;
directive: '@' name arguments?;

// https://graphql.github.io/graphql-spec/June2018/#TypeSystemDefinition
typeSystemDefinition: schemaDefinition
	| typeDefinition
	| directiveDefinition
	;

//https://spec.graphql.org/June2018/#TypeSystemExtension
typeSystemExtension: schemaExtension
    | typeExtension
    ;

// https://graphql.github.io/graphql-spec/June2018/#sec-Schema
schemaDefinition:
	 SCHEMA directives? '{' rootOperationTypeDefinition+ '}';

rootOperationTypeDefinition: operationType ':' namedType;

//https://spec.graphql.org/June2018/#sec-Schema-Extension
schemaExtension:
    EXTEND SCHEMA directives? '{' operationTypeDefinition+ '}'
    | EXTEND SCHEMA directives
    ;

//https://spec.graphql.org/June2018/#OperationTypeDefinition
operationTypeDefinition: operationType ':' namedType;


//https://spec.graphql.org/June2018/#sec-Descriptions
description: stringValue;

//https://spec.graphql.org/June2018/#sec-Types
typeDefinition:
	scalarTypeDefinition
	| objectTypeDefinition
	| interfaceTypeDefinition
	| unionTypeDefinition
	| enumTypeDefinition
	| inputObjectTypeDefinition;

//https://spec.graphql.org/June2018/#sec-Type-Extensions
typeExtension : scalarTypeExtension
    | objectTypeExtension
    | interfaceTypeExtension
    | unionTypeExtension
    | enumTypeExtension
    | inputObjectTypeExtension
    ;

//https://spec.graphql.org/June2018/#sec-Scalars
scalarTypeDefinition: description? SCALAR name directives?;

//https://spec.graphql.org/June2018/#sec-Scalar-Extensions
scalarTypeExtension:  EXTEND SCALAR name directives;

// https://graphql.github.io/graphql-spec/June2018/#sec-Objects
objectTypeDefinition :
    description?   TYPE name implementsInterfaces?  directives? fieldsDefinition?;

implementsInterfaces: 'implements' '&'? namedType
    | implementsInterfaces '&' namedType
    ;


fieldsDefinition: '{'  fieldDefinition+ '}';
fieldDefinition: description? name  argumentsDefinition? ':' type_  directives? ;

//https://spec.graphql.org/June2018/#sec-Field-Arguments
argumentsDefinition: '(' inputValueDefinition+ ')';
inputValueDefinition:  description? name ':' type_ defaultValue? directives?;

//https://spec.graphql.org/June2018/#sec-Object-Extensions
objectTypeExtension:
    EXTEND TYPE name implementsInterfaces? directives? fieldsDefinition
    | EXTEND TYPE name implementsInterfaces? directives
    | EXTEND TYPE name implementsInterfaces
    ;

//https://spec.graphql.org/June2018/#sec-Interfaces
interfaceTypeDefinition: description? INTERFACE name directives? fieldsDefinition?;

//https://spec.graphql.org/June2018/#sec-Interface-Extensions
interfaceTypeExtension:  EXTEND INTERFACE name directives? fieldsDefinition
    | EXTEND INTERFACE name directives
    ;

// https://graphql.github.io/graphql-spec/June2018/#sec-Unions
unionTypeDefinition:  description? UNION name directives? unionMemberTypes?;
unionMemberTypes: '=' '|'?  namedType ('|'namedType)* ;

//https://spec.graphql.org/June2018/#sec-Union-Extensions
unionTypeExtension : EXTEND UNION name directives? unionMemberTypes
    | EXTEND UNION name directives
    ;

//https://spec.graphql.org/June2018/#sec-Enums
enumTypeDefinition:  description? ENUM name directives? enumValuesDefinition?;
enumValuesDefinition: '{' enumValueDefinition+  '}';
enumValueDefinition: description? enumValue  directives?;

//https://spec.graphql.org/June2018/#sec-Enum-Extensions
enumTypeExtension: EXTEND ENUM name directives? enumValuesDefinition
    | EXTEND ENUM name directives
    ;

//https://spec.graphql.org/June2018/#sec-Input-Objects
inputObjectTypeDefinition: description? INPUT name directives? inputFieldsDefinition?;
inputFieldsDefinition: '{' inputValueDefinition+ '}';

//https://spec.graphql.org/June2018/#sec-Input-Object-Extensions
inputObjectTypeExtension:  EXTEND INPUT name directives? inputFieldsDefinition
    | EXTEND INPUT name directives
    ;

//https://spec.graphql.org/June2018/#sec-Type-System.Directives
directiveDefinition: description? 'directive' '@' name argumentsDefinition? 'on' directiveLocations;
directiveLocations: directiveLocation ('|' directiveLocation)*;
directiveLocation: executableDirectiveLocation | typeSystemDirectiveLocation;

executableDirectiveLocation:
     QUERY_U
    | MUTATION_U
    | SUBSCRIPTION_U
    | FIELD_U
    | FRAGMENT_DEFINITION_U
    | FRAGMENT_SPREAD_U
    | INLINE_FRAGMENT_U
    ;

typeSystemDirectiveLocation:
     'SCHEMA'
    | 'SCALAR'
    | 'OBJECT'
    | 'FIELD_DEFINITION'
    | 'ARGUMENT_DEFINITION'
    | 'INTERFACE'
    | 'UNION'
    | 'ENUM'
    | 'ENUM_VALUE'
    | 'INPUT_OBJECT'
    | 'INPUT_FIELD_DEFINITION'
    ;



name: NAME | TYPE | QUERY | MUTATION | SUBSCRIPTION | FRAGMENT |
      INTERFACE | ENUM | UNION | INPUT | SCALAR | SCHEMA | EXTEND | TRUE | FALSE |
      QUERY_U | MUTATION_U | SUBSCRIPTION_U | FIELD_U | FRAGMENT_DEFINITION_U | FRAGMENT_SPREAD_U | INLINE_FRAGMENT_U |
      SCHEMA_U | SCALAR_U | OBJECT_U | FIELD_DEFINITION_U | ARGUMENT_DEFINITION_U | INTERFACE_U | UNION_U | ENUM_U | ENUM_VALUE_U | INPUT_OBJECT_U | INPUT_FIELD_DEFINITION_U;


//Start lexer
TYPE : 'type';

QUERY : 'query';
QUERY_U : 'QUERY';

MUTATION : 'mutation';
MUTATION_U : 'MUTATION';

SUBSCRIPTION : 'subscription';
SUBSCRIPTION_U : 'SUBSCRIPTION';

FIELD_U : 'FIELD';
FRAGMENT_DEFINITION_U : 'FRAGMENT_DEFINITION';
FRAGMENT_SPREAD_U: 'FRAGMENT_SPREAD';
INLINE_FRAGMENT_U : 'INLINE_FRAGMENT';

FRAGMENT : 'fragment';

INTERFACE : 'interface';

ENUM : 'enum';

UNION : 'union';

INPUT : 'input';

SCALAR : 'scalar';

SCHEMA : 'schema';

EXTEND : 'extend';

TRUE : 'true';

FALSE : 'false';

SCHEMA_U : 'SCHEMA';
SCALAR_U : 'SCALAR';
OBJECT_U : 'OBJECT';
FIELD_DEFINITION_U : 'FIELD_DEFINITION';
ARGUMENT_DEFINITION_U :'ARGUMENT_DEFINITION';
INTERFACE_U : 'INTERFACE';
UNION_U : 'UNION';
ENUM_U : 'ENUM';
ENUM_VALUE_U : 'ENUM_VALUE';
INPUT_OBJECT_U : 'INPUT_OBJECT';
INPUT_FIELD_DEFINITION_U : 'INPUT_FIELD_DEFINITION';

NAME: [_A-Za-z] [_0-9A-Za-z]*;

fragment CHARACTER: ( ESC | ~ ["\\]);
STRING: '"' CHARACTER* '"';

BLOCK_STRING
    :   '"""' .*? '"""'
    ;

ID: STRING;

fragment ESC: '\\' ( ["\\/bfnrt] | UNICODE);

fragment UNICODE: 'u' HEX HEX HEX HEX;

fragment HEX: [0-9a-fA-F];

fragment NONZERO_DIGIT: [1-9];
fragment DIGIT: [0-9];
fragment FRACTIONAL_PART: '.' DIGIT+;
fragment EXPONENTIAL_PART: EXPONENT_INDICATOR SIGN? DIGIT+;
fragment EXPONENT_INDICATOR: [eE];
fragment SIGN: [+-];
fragment NEGATIVE_SIGN: '-';

FLOAT: INT FRACTIONAL_PART
    | INT EXPONENTIAL_PART
    | INT FRACTIONAL_PART EXPONENTIAL_PART
    ;

INT: NEGATIVE_SIGN? '0'
    | NEGATIVE_SIGN? NONZERO_DIGIT DIGIT*
    ;

PUNCTUATOR: '!'
    | '$'
    | '(' | ')'
    | '...'
    | ':'
    | '='
    | '@'
    | '[' | ']'
    | '{' | '}'
    | '|'
    ;

// no leading zeros

fragment EXP: [Ee] [+\-]? INT;

// \- since - means "range" inside [...]

WS: [ \t\n\r]+ -> skip;
COMMA: ',' -> skip;
LineComment
    :   '#' ~[\r\n]*
        -> skip
    ;

UNICODE_BOM: (UTF8_BOM
    | UTF16_BOM
    | UTF32_BOM
    ) -> skip
    ;

UTF8_BOM: '\uEFBBBF';
UTF16_BOM: '\uFEFF';
UTF32_BOM: '\u0000FEFF';
