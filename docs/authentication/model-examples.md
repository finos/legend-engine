# Model Examples 

```AuthenticationSpecification``` elements can be used in any model element that models authentication.

```
AuthenticationDemo demo::demo1
{
    authentication: UserPassword
    {
        ...
    }
}

RelationalDatabaseConnection demo::demo2
{
    authentication: UserPassword
    {
        ...
    }
}
```

```CredentialVaultSecret``` elements can be used in any model element that models a secret.

```
AuthenticationDemo demo::demo1
{
    authentication: UserPassword
    {
        username: 'alice';
        password: PropertiesFileSecret
        {
            reference: 'reference1';
        }
    }
}

RelationalDatabaseConnection demo::demo2
{
    authentication: UserPassword
    {
        username: 'alice';
        password: PropertiesFileSecret
        {
            reference: 'reference1';
        }
    }
}
```