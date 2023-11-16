# Authentication Concepts

```
Notation : 

In the sections below, "Java" in brackets indicates a Java concept.  "Pure" in brackets indicates a Pure concept.

In some cases, a concept is implemented in both Pure and Java.
```

## Identity (Java)

```Identity``` represents the current 'user'. The user can be a human or system user. 

## Credential (Java)

An ```Identity``` carries one/more ```Credential```s as proof. 

## Authentication Specification (Pure, Java)

The platform connects to database, APIs and other systems that require authentication.

The type of authentication to be performed is described via an ```AuthenticationSpecification```.

## Credential Provider (Java)

Authentication requires presenting one/more ```Credential```s to the target system. These ```Credential```s can be obtained via one of the following ways :
* The current ```Identity``` already has the required ```Credential```.
* A new ```Credential``` has to be fetched/computed.

A ```CredentialProvider``` consumes an ```AuthenticationSpecification``` and produces a ```Credential```.

## Intermediation Rule (Java)

In some cases, given an ```AuthenticationSpecification``` there are many ways to compute the required ```Credential```.

Each of these ways is implemented as an ```IntermediationRule```. 

A ```CredentialProvider``` therefore composes ```IntermediationRule```s. 

## Credential Vault Secret (Pure, Java)

In many cases, authentication requires the use of secrets. 

These secrets are not inlined in the Legend model. Instead, model elements refer to a secret that is fetched from a credential vault at runtime.

A ```CredentialVaultSecret``` describes where a secret is stored. At runtime, the platform fetches and uses the secret.