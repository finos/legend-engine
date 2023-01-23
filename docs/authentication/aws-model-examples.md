# AWS Model Examples 

Elements that source secrets from AWS Secrets Manager can define their secret via the ```AWSSecretsManagerSecret``` model element.

```
{
    mySecret: AWSSecretsManagerSecret
    {
        secretId: 'arn:aws:secretsmanager:us-east-1:123456789:secret:foo-bar';
        ...      
    }
}
```

However, fetching a secret from AWS Secrets Manager requires authenticating with AWS/AWS Secrets Manager. 

These secrets have to be configured as well. The metamodel currently supports three ways of configuring these secrets. 

## AWS Default Secrets 

In some cloud execution environments, AWS secrets are already configured in the environment via AWS configuration/credential files etc.  

To allow Legend to make use of these secrets, configure the ```awsCredentials``` to use "default" credentials as follows. 

```
{
    mySecret: AWSSecretsManagerSecret
    {
        secretId: 'arn:aws:secretsmanager:us-east-1:123456789:secret:foo-bar';
        awsCredentials: Default {
        }      
    }
}
```
In this mode, at runtime, Legend simply wires the AWS SDK to use the AWS `DefaultCredentialsProvider` API.

## AWS Static Secrets 

In some cases, AWS secrets are explicitly configured. 

To allow Legend to use these secrets, configure the ``awsCredentials``` to use "Static" credentials as follows. Since the credentials are static, they can be injected via one of the supported static injection schemes.

```
{
    mySecret: AWSSecretsManagerSecret
    {
        secretId: 'arn:aws:secretsmanager:us-east-1:123456789:secret:foo-bar';
        awsCredentials: Static {
            accessKeyId: SystemPropertySecret {
                systemPropertyName: 'my.accessKeyId';
            }
            secretAccessKey: SystemPropertySecret {
                systemPropertyName: 'my.secretAccessKey';
            }            
        }      
    }
}
```

## AWS STS Assume Role

In some cases, long lives AWS secrets are used to get temporary STS secrets. These are in turn used to fetch secrets from secrets manager. 

To allow Legend to use STS, configure ``awsCredentials`` to use "STSAssumeRole" credentials as follows. The long lived credentials can be injected via one of the supported static injection schemes.


```
{
    mySecret: AWSSecretsManagerSecret
    {
        secretId: 'arn:aws:secretsmanager:us-east-1:123456789:secret:foo-bar';
        awsCredentials: STSAssumeRole {
            roleArn: 'role1';
            roleSessionName: 'roleSession1';
            awsCredentials: Static {
                accessKeyId: SystemPropertySecret {
                    systemPropertyName: 'my.accessKeyId';
                }
                secretAccessKey: SystemPropertySecret {
                    systemPropertyName: 'my.secretAccessKey';
                }
            }            
        }      
    }
}
```