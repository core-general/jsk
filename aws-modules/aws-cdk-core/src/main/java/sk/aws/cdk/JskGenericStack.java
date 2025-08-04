package sk.aws.cdk;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.jetbrains.annotations.NotNull;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.elasticache.CfnSubnetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.targets.InstanceTarget;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.List;

public abstract class JskGenericStack extends Stack {
    protected final JskGenericStackParams params;

    public JskGenericStack(final Construct scope, final JskGenericStackParams params) {
        super(scope, params.getAppPrefix() + "Stack", StackProps.builder().env(Environment.builder()
                .account(params.getAccountId())
                .region(params.getRegion())
                .build()).build());
        this.params = params;
    }

    /** USE THIS */
    protected void createAlbApp() {
        Vpc vpc = createVpc(params.getAppPrefix());
        Instance ec2 = createEc2(params.getAppPrefix(), vpc, params.getAllowedIps(), params.getEc2Img(),
                params.isElasticIpForInstance());
        ApplicationLoadBalancer alb = createAlb(params.getAppPrefix(), vpc,
                params.getAlbDomain().orElseThrow(() -> new RuntimeException("For alb setup must be domain name")),
                params.getHealthCheckAlb().orElseThrow(() -> new RuntimeException("For alb setup must have healthCheck")),
                O.of(ec2));
        DatabaseInstance rdsPg = createRdsPg(params.getAppPrefix(), vpc, params.getPgVer(), params.isRdsEncrypted(), Cc.l(ec2),
                params.getAllowedIps(),
                params.isCantDeleteRds());

        if (params.isCreateRedisCacheCluster()) {
            CfnCacheCluster redisCluster =
                    createRedisCluster(params.getAppPrefix(), vpc, params.getRedisVersion(), params.getRedisPort(),
                            params.getRedisCacheNodeCount(), Cc.l(ec2), params.getAllowedIps());
        }
    }

    //region LOW LEVEL
    protected @NotNull Vpc createVpc(String prefixCamelCase) {
        return Vpc.Builder.create(this, prefixCamelCase + "Vpc")
                .ipProtocol(IpProtocol.DUAL_STACK)
                .maxAzs(99)  // Use all Availability Zones for better availability
                .natGateways(0)  // No NAT Gateways for cost-saving
                .ipAddresses(IpAddresses.cidr("10.10.0.0/16"))
                .ipv6Addresses(Ipv6Addresses.amazonProvided())
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("PublicSubnet")
                                .subnetType(SubnetType.PUBLIC)
                                .ipv6AssignAddressOnCreation(true)
                                .build()
                ))
                .build();
    }

    protected Instance createEc2(String prefixCamelCase, Vpc vpc,
            List<String> allowedIps,
            IMachineImage img,
            boolean elasticIp) {

        // Create a security group for the EC2 instance
        SecurityGroup ec2SecurityGroup = SecurityGroup.Builder.create(this, prefixCamelCase + "Ec2Sg")
                .vpc(vpc)
                .description("Security Group for EC2 instance")
                .allowAllOutbound(true)
                .build();

        // Create the IAM role for EC2 instance
        Role ec2Role = Role.Builder.create(this, prefixCamelCase + "Ec2Role")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore")
                ))
                .build();

        // Create a key pair for SSH access
        KeyPair keyPair = KeyPair.Builder.create(this, prefixCamelCase + "KeyPair")
                .keyPairName(prefixCamelCase + "-key-pair")
                .format(KeyPairFormat.PEM)
                .build();

        // Create the EC2 instance with public IP
        Instance webServer = Instance.Builder.create(this, prefixCamelCase + "Srv")
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MICRO)) // t3.micro (Free Tier eligible)
                .machineImage(img)
                .securityGroup(ec2SecurityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .role(ec2Role)
                .allowAllOutbound(true)
                .keyPair(keyPair) // Key pair for SSH access - create this in AWS console first
                .ipv6AddressCount(1) // Explicitly assign public IPV6 address
                .blockDevices(List.of(
                        BlockDevice.builder()
                                .deviceName("/dev/sda1")
                                .volume(BlockDeviceVolume.ebs(20))
                                .build()
                ))
                .build();

        allowedIps.forEach(
                $ -> webServer.getConnections().allowFrom(Peer.ipv4($), Port.tcp(22), "Allow SSH traffic from specific IP"));

        // Output the key pair name for reference
        CfnOutput.Builder.create(this, prefixCamelCase + "KeyPairName")
                .description("The keypair protected key:")
                .value(keyPair.getPrivateKey().getParameterName())
                .build();


        if (elasticIp) {
            webServer.getConnections().allowFrom(Peer.anyIpv4(), Port.tcp(443), "Allow HTTPS traffic");
            // Create an Elastic IP
            CfnEIP elasticIP = CfnEIP.Builder.create(this, prefixCamelCase + "ServerElasticIP")
                    .domain("vpc")
                    .build();

            // Associate the Elastic IP with the EC2 instance
            CfnEIPAssociation eipAssociation = CfnEIPAssociation.Builder.create(this, prefixCamelCase + "EipAssociation")
                    .allocationId(elasticIP.getAttrAllocationId())
                    .instanceId(webServer.getInstanceId())
                    .build();
            // Output the Elastic IP for reference
            CfnOutput.Builder.create(this, prefixCamelCase + "ServerElasticIpAddress")
                    .description("Elastic IP address assigned to the server:")
                    .value(elasticIP.getRef())
                    .build();
        }
        return webServer;
    }

    protected Certificate createCertificate(String prefixCamelCase, String domainName) {
        // Create a DNS validated certificate for the domain
        Certificate certificate = Certificate.Builder.create(this, prefixCamelCase + "Certificate")
                .domainName(domainName)
                .validation(CertificateValidation.fromDns()) // DNS validation
                .build();

        // Output the certificate ARN for reference
        CfnOutput.Builder.create(this, prefixCamelCase + "CertificateArn")
                .description("Certificate ARN:")
                .value(certificate.getCertificateArn())
                .build();

        return certificate;
    }

    protected ApplicationLoadBalancer createAlb(String prefixCamelCase, Vpc vpc, String domainName,
            String healtCHeckPath, O<Instance> instanceToAddOn8080) {
        SecurityGroup albSecurityGroup = SecurityGroup.Builder.create(this, prefixCamelCase + "AlbSg")
                .vpc(vpc)
                .description("Security Group for Application Load Balancer")
                .allowAllOutbound(true)
                .build();

        ApplicationTargetGroup targetGroup = createTargetGroup(prefixCamelCase, vpc, healtCHeckPath);

        // Allow HTTP and HTTPS traffic from anywhere
        albSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80), "Allow HTTP traffic");
        albSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(443), "Allow HTTPS traffic");

        // Create the ALB
        ApplicationLoadBalancer alb = ApplicationLoadBalancer.Builder.create(this, prefixCamelCase + "Alb")
                .vpc(vpc)
                .internetFacing(true)
                .securityGroup(albSecurityGroup)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .build();

        ICertificate certificate = createCertificate(prefixCamelCase, domainName);

        // Add a listener for HTTPS traffic
        ApplicationListener httpsListener = alb.addListener(prefixCamelCase + "HttpsListener",
                BaseApplicationListenerProps.builder()
                        .port(443)
                        .protocol(ApplicationProtocol.HTTPS)
                        .certificates(List.of(ListenerCertificate.fromCertificateManager(certificate)))
                        .defaultTargetGroups(List.of(targetGroup))
                        .build());

        // Output the ALB DNS name for reference
        CfnOutput.Builder.create(this, prefixCamelCase + "AlbDnsName")
                .description("ALB DNS Name:")
                .value(alb.getLoadBalancerDnsName())
                .build();

        if (instanceToAddOn8080.isPresent()) {
            Instance inst = instanceToAddOn8080.get();
            inst.getConnections().allowFrom(alb, Port.tcp(8080), "Allow HTTP traffic for " + prefixCamelCase + "Alb");
            targetGroup.addTarget(new InstanceTarget(inst, 8080));
        }


        return alb;
    }

    protected @NotNull ApplicationTargetGroup createTargetGroup(String prefixCamelCase, Vpc vpc, String healtCHeckPath) {
        return ApplicationTargetGroup.Builder.create(this, prefixCamelCase + "Group")
                .vpc(vpc)
                .port(8080)
                .protocol(ApplicationProtocol.HTTP)
                .targetType(TargetType.INSTANCE)
                .healthCheck(HealthCheck.builder()
                        .path(healtCHeckPath)
                        .port("8080")
                        .healthyHttpCodes("200-299")
                        .enabled(true)
                        .protocol(Protocol.HTTP)
                        .healthyThresholdCount(2)
                        .unhealthyThresholdCount(2)
                        .interval(Duration.seconds(5))
                        .timeout(Duration.seconds(3))
                        .build())
                .build();
    }

    protected DatabaseInstance createRdsPg(String prefixCamelCase, Vpc vpc,
            PostgresEngineVersion pgVer,
            boolean storageEncrypted,
            List<Instance> allowedServers,
            List<String> allowedIps,
            boolean deletionProtectionRds) {
        // Create a security group for the RDS instance
        SecurityGroup rdsSecurityGroup = SecurityGroup.Builder.create(this, prefixCamelCase + "RdsSg")
                .vpc(vpc)
                .description("Security Group for RDS instance")
                .allowAllOutbound(false)
                .build();

        // Create the RDS PostgreSQL instance
        DatabaseInstance rdsInstance = DatabaseInstance.Builder.create(this, prefixCamelCase + "Db")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(pgVer)
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MICRO)) // t3.micro (Free Tier eligible)
                .vpc(vpc)
                .publiclyAccessible(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .securityGroups(List.of(rdsSecurityGroup))
                .credentials(Credentials.fromGeneratedSecret("postgres")) // Generate random password
                .allocatedStorage(20) // 20GB storage (minimum)
                .databaseName(prefixCamelCase.toLowerCase() + "db")
                .multiAz(false) // No Multi-AZ for free tier
                .storageEncrypted(storageEncrypted)
                .deletionProtection(deletionProtectionRds) // Set to true in production
                .build();

        allowedServers.forEach($ -> rdsInstance.getConnections().allowFrom(
                $, Port.tcp(5432), "Allow PostgreSQL traffic from specific servers"));
        allowedIps.forEach($ -> rdsInstance.getConnections().allowFrom(
                Peer.ipv4($), Port.tcp(5432), "Allow PostgreSQL traffic from specific IP"));


        CfnOutput.Builder.create(this, prefixCamelCase + "DbSecretName")
                .description("The name of the secret with password:")
                .value(rdsInstance.getSecret().getSecretName())
                .build();
        return rdsInstance;
    }

    protected CfnCacheCluster createRedisCluster(String prefixCamelCase, Vpc vpc,
            String redisVersion,
            int port,
            int cacheNodeCount,
            List<Instance> allowedServers,
            List<String> allowedIps) {
        // Create a security group for the ElastiCache cluster
        SecurityGroup elasticacheSecurityGroup = SecurityGroup.Builder.create(this, prefixCamelCase + "RedisSecurityGroup")
                .vpc(vpc)
                .description("Security Group for Redis ElastiCache Cluster")
                .allowAllOutbound(false)
                .build();

        // Create a subnet group for the ElastiCache cluster
        CfnSubnetGroup subnetGroup = CfnSubnetGroup.Builder.create(this, prefixCamelCase + "RedisSubnetGroup")
                .description("Subnet group for Redis ElastiCache Cluster")
                .subnetIds(vpc.selectSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build()).getSubnetIds())
                .build();

        // Create the Redis ElastiCache cluster
        CfnCacheCluster redisCluster = CfnCacheCluster.Builder.create(this, prefixCamelCase + "RedisCluster")
                .engine("redis")
                .engineVersion(redisVersion)
                .cacheNodeType("cache.t3.micro") // Free tier eligible
                .numCacheNodes(cacheNodeCount)
                .port(port)
                .cacheSubnetGroupName(subnetGroup.getRef())
                .vpcSecurityGroupIds(List.of(elasticacheSecurityGroup.getSecurityGroupId()))
                .azMode(cacheNodeCount > 1 ? "cross-az" : "single-az")
                .build();

        // Allow connections from EC2 instances and specific IPs
        allowedServers.forEach($ -> elasticacheSecurityGroup.addIngressRule(
                Peer.securityGroupId($.getConnections().getSecurityGroups().get(0).getSecurityGroupId()),
                Port.tcp(port), "Allow Redis traffic from EC2 instance"));
        allowedIps.forEach($ -> elasticacheSecurityGroup.addIngressRule(
                Peer.ipv4($), Port.tcp(port), "Allow Redis traffic from specific IP"));

        // Output the cluster endpoint for reference
        CfnOutput.Builder.create(this, prefixCamelCase + "RedisEndpoint")
                .description("Redis ElastiCache Endpoint:")
                .value(redisCluster.getAttrRedisEndpointAddress() + ":" + redisCluster.getAttrRedisEndpointPort())
                .build();

        return redisCluster;
    }
    //endregion
}
