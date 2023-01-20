# Code Examples

## Configuration

```

// 1 - Configure and assemble platform vaults

        Properties properties = new Properties();
        properties.load(new FileInputStream(Paths.get("/my/file").toFile()));
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);
        
        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(propertiesFileCredentialVault)
                .build();
                
// 2 - Configure and assemble other vaults

        AWSSecretsManagerVault awsSecretsManagerVault = AWSSecretsManagerVault.builder()
                .with(platformCredentialVaultProvider)
                .build();

// 3 - Configure a vault provider 

        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                .with(awsSecretsManagerVault)
                .build();

// 4 - Configure and assemble various rules 

        IntermediationRuleProvider intermediationRuleProvider = IntermediationRuleProvider.builder()
                .with(new UserPasswordFromVaultRule(credentialVaultProvider))
                .with(new MyRule1(credentialVaultProvider))                
                .with(new MyRule2(credentialVaultProvider))                
                .build();
                
// 5 - Configure and assemble various credential providers

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(new UserPasswordCredentialProvider())
                .with(new ApiKeyCredentialProvider())                
                .with(new GCPWIFWithAWSIdPOAuthCredentialProvider())
                .with(intermediationRuleProvider)
                .build();                
```

## Making a credential 

```
// 1 - Obtain CredentialProviderProvider 

CredentialProviderProvider credentialProviderProvider = ... // configure and inject as needed

// 2 - Obtain runtime user identity 

Identity identity = ...  // from Pac4J profile etc 

// 3 - Obtain authentication specification 

AuthenticationSpecification authenticationSpecification = ... // from execution extension etc 

// 4 - Build a credential

Credential credential = CredentialBuilder.makeCredential(credentialProviderProvider, authenticationSpecification, identity);

// 5 - Use the credential 

// User the credential in JDBC, HTTP API calls etc

```