package org.finos.legend.engine.authentication.cloud;

public class AWSConfig
{
    public String getRegion()
    {
        return region;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public String getRole()
    {
        return role;
    }

    public String getAwsAccessKeyIdVaultReference()
    {
        return awsAccessKeyIdVaultReference;
    }

    public String getAwsSecretAccessKeyVaultReference()
    {
        return awsSecretAccessKeyVaultReference;
    }

    public AWSConfig(String region, String accountId, String role, String awsAccessKeyIdVaultReference, String awsSecretAccessKeyVaultReference)
    {
        this.region = region;
        this.accountId = accountId;
        this.role = role;
        this.awsAccessKeyIdVaultReference = awsAccessKeyIdVaultReference;
        this.awsSecretAccessKeyVaultReference = awsSecretAccessKeyVaultReference;
    }

    private String region;
    private String accountId;
    private String role;
    private String awsAccessKeyIdVaultReference;
    private String awsSecretAccessKeyVaultReference;

    public AWSConfig()
    {
        // jackson
    }
}
