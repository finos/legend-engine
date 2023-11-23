# Change Tokens

## Introduction

Change Tokens module propose is to generate Java code with **upcast** and **downcast** functions that will convert one entity version to another,
which simplifies deployment of the services as there is no need for versioned api. Instant, the payload from the client of serialized request
has a version of the entity schema – server will upcast the request before deserialization to business object, process the request – and downcast
response entity to client version before serializing on wire.

## Change Tokens Generation
**TODO**: Tool that will compare all releases of entities jars and create required **versions** JSON.

Tool should automatically detect fields that ware most likely renamed or aggregated/extracted.
For those kind of detected operations user confirmation should be required, in opposition of simple field addition or removal.

## Change Token Types
- **AddedClass** / **RemovedClass** - current no-op - should be emitted if a class of entity was added/removed.
- **RenamedClass** - token describing that a class was renamed or moved to another package.
- **AddField** / **RemoveField** - token describing that a field was added or removed to specific entity class.
  This token should provide **fieldName**, **fieldType** and **class** properties together with **defaultValue**, of e.g. **ConstValue** **@type**.
  The **value** should be of expected primitive or nested object type (in case of object type **@type** property is mandatory).

E.g.
```json
{
	"@type": "meta::pure::changetoken::AddField",
	"fieldName": "abc",
	"fieldType": "String[1]",
	"defaultValue": {
		"@type": "meta::pure::changetoken::ConstValue",
		"value": "UNKNOWN"
	},
	"class": "meta::pure::changetoken::tests::SampleClass"
}
```
is a change token of **AddField** type, that during **upcast** of *SampleClass* will add to it *abc* field, of *String[1]* type (note: type is omitted 
in serialized payload, but required for sanity checks) and give it *UNKNOWN* as string value.

So:
```json
{
	"@type": "meta::pure::changetoken::tests::SampleClass",
	"xyz": "someValue"
}
```
will become:
```json
{
	"@type": "meta::pure::changetoken::tests::SampleClass",
	"abc": "UNKNOWN",
	"xyz": "someValue"
}
```
During **downcast** the *abc* field will be removed only if it's value is *UNKNOWN*, if the *abc* field will be present
with different value an error will be thrown.
Similarly, for **RemoveField** token during **upcast** a field will be removed only if it's content is as described in **defaultValue** - or an error occurs.

Basically **AddField** **upcast** is **RemoveField** **downcast** - something is added, and **AddField** **downcast** is **RemoveField** **upcast** - something
is removed. When something is added it will have **defaultValue** **value**, and if something is removed it has to have value of **defaultValue** **value**.

- **RenameField** tokens allows simple renaming of the field, or it's movement to/from tested field.

When field is moved the destination property has to exist, e.g. has to be created with **AddField** token.

Examples:
```json
{
	"@type": "meta::pure::changetoken::RenameField",
	"oldFieldName": [
		"abc"
	],
	"newFieldName": [
		"xyz"
	],
	"class": "meta::pure::changetoken::tests::SampleClass"
}
```
This change token will rename field *abc* to *xyz* on *SampleClass* entity.
So:
```json
{
	"@type": "meta::pure::changetoken::tests::SampleClass",
	"abc": "someValue"
}
```
will become:
```json
{
	"@type": "meta::pure::changetoken::tests::SampleClass",
	"xyz": "someValue"
}
```
**RenameField** token can be also used for aggregation/extraction of fields between different versions of entities.

For example:
```json
{
	"@type": "meta::pure::changetoken::RenameField",
	"oldFieldName": [
		"abc"
	],
	"newFieldName": [
		"nested", "abc"
	],
	"class": "meta::pure::changetoken::tests::SampleClass"
}
```
This change token will move field *abc* to *nested* in *SampleClass* entity.
So:
```json
{
	"@type": "meta::pure::changetoken::tests::SampleClass",
	"abc": "someValue",
	"nested": {
		"@type": "meta::pure::changetoken::tests::OtherClass",
		"rst": "someOtherValue"
	}
}
```
will become:
```json
{
  "@type": "meta::pure::changetoken::tests::SampleClass",
  "nested": {
    "@type": "meta::pure::changetoken::tests::OtherClass",
    "abc": "someValue",
    "rst": "someOtherValue"
  }
}
```
Please note that *nested* property has to already exist in *SampleClass* (added with **AddField** token occurred before **RenameField** in this or previous **version**).
Also, the *abc* cannot exist in *nested* as it would overwrite existing field value - in that case an error would be thrown during the **upcast**.
During **downcast** this change token will cause a reverse movement

As mentioned, if fields are aggregated into new field, that field should be created with **AddField** before, and if they are extracted that field should be removed
with **RemoveField** if field becomes obsolete and doesn't have anymore meaningful properties.

- **ChangeFieldType** allows change of field type, currently a **String[1]** can be converted to **Integer[i]** and vice versa.

Another allowed conversion is changing **SomeType[1]** to **SomeType[0..1]**, which is a no-op meaning that field becomes optional.

Changing between other types is not implemented, and will be most likely handled by user registering with generator a pure functions that can handle those type of conversions. 

## Versions Grammar

To generate Java code a JSON specifying chain of versions is needed. There must be a **versions** property in JSON that is a sorted array of all versions,
from initial to the latest one.
First version entry should be an object with just **version** property.

Example:
```json
{
	"versions": [
		{
			"version": "one"
		}
	]
}
```

As mentioned the list of version needs to be sorted, and every version beside first one have to reference previous version in **prevVersion** property.

Example:
```json
{
	"versions": [
		{
			"version": "one"
		},
		{
			"prevVersion": "one",
			"version": "two"
		},
		{
			"prevVersion": "two",
			"version": "three"
		}
	]
}
```

If in example above **version** tokens would be in wrong order an error would be thrown.

Each **version** should have a **changeTokens** property specifying what kind of operations are needed to convert from previous to next version.

Tokens in **changeTokens** should be an ordered list of operations that are desired to be executed.

So if two properties are aggregated in new field, first there should be **AddField** token that would create that field, followed by two **RenameField** tokens.
If two properties are extracted from old field, first there should be two **RenameField** tokens, followed by **RemoveField**.
Of course if new field already exist there is no need for **AddField**, and if old field is not yet obsolete and carries some additional information no **RemoveField** should be emitted. 

Example:

In version *two* of project *someProperty* of type *String[1]* was added to *my::project::FirstClass* entity with *n/a* string as default value.
In version *three* of project *someProperty* was renamed to *actualName*.
```json
{
	"versions": [
		{
			"version": "one"
		},
		{
			"prevVersion": "one",
			"version": "two",
			"changeTokens": [
				{
					"@type": "meta::pure::changetoken::AddField",
					"class": "my::project::FirstClass",
					"fieldName": "someProperty",
					"fieldType": "String[1]",
					"defaultValue": {
						"@type": "meta::pure::changetoken::ConstValue",
						"value": "n/a"
					}
				}
			]
		},
		{
			"prevVersion": "two",
			"version": "three",
			"changeTokens": [
				{
					"@type": "meta::pure::changetoken::RenameField",
					"class": "my::project::FirstClass",
					"oldFieldName": [
						"someProperty"
					],
					"newFieldName": [
						"actualName"
					]
				}
			]
		}
	]
}
```

So upcast of:
```json
{
	"@type": "my::project::FirstClass",
	"version": "one"
}
```

should give:
```json
{
	"@type": "my::project::FirstClass",
	"version": "two",
	"someProperty": "n/a"
}
```
followed by conversion to:
```json
{
	"@type": "my::project::FirstClass",
	"version": "three",
	"actualName": "n/a"
}
```

Downcast of 
```json
{
	"@type": "my::project::FirstClass",
	"version": "three",
	"actualName": "Actual Name"
}
```

should give:
```json
{
	"@type": "my::project::FirstClass",
	"version": "two",
	"someProperty": "Actual Name"
}
```
however downcast to version *one* is not possible as *someProperty* has a different value then default one *n/a*

Downcast is only possible if no data is lost, which means that only properties with default values can undergo downcast.

## Additional Information

For more examples please have a look at [unit tests](legend-engine-xt-changetoken-compiler/src/test/java/org/finos/legend/engine/changetoken/generation) directory.

Where more complicated cases are given - setupSuite gives **versions** JSON, and then there are tests of **upcast** and **downcast** methods.
