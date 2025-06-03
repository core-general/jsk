package sk.aws.cdk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;

import java.util.List;


@Getter
@With
@AllArgsConstructor
@RequiredArgsConstructor
public class JskGenericStackParams {
    private final String accountId;
    private final String region;
    private final String appPrefix;
    private final List<String> allowedIps;

    private O<String> albDomain = O.empty();
    private O<String> healthCheckAlb = O.empty();
    private IMachineImage ec2Img = MachineImage.genericLinux(Cc.m(
            //Verified provider ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-20250516
            "eu-west-1", "ami-0286d0aea4d6c7a34"
    ));
    private boolean elasticIpForInstance = false;
    private PostgresEngineVersion pgVer = PostgresEngineVersion.VER_17;
    private boolean cantDeleteRds = true;
    private boolean rdsEncrypted = true;
}
