# Jsk: Java Swiss Knife

You can build from source or you can use official S3 based maven repo.

For S3 repo :

    <repositories>
        <repository>
            <id>jsk-repo</id>
            <name>S3 jsk repo</name>
            <url>https://jsk-maven-repository.s3.eu-north-1.amazonaws.com/release</url>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>jsk-repo</id>
            <name>S3 jsk repo</name>
            <url>https://jsk-maven-repository.s3.eu-north-1.amazonaws.com/release</url>
        </pluginRepository>
    </pluginRepositories>
    
If you want to fork JSK and put it to S3 as well, you should do this:
1. Create S3 bucket with some name like x_bucket and create folders snapshot and release inside
2. Create IAM user and give him rights for S3 bucket, also create programmatic access for him to get access key and secret.

        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "some_id",
              "Effect": "Allow",
              "Action": "s3:*",
              "Resource": [
                "arn:aws:s3:::x_bucket",
                "arn:aws:s3:::x_bucket/*"
              ]
            }
          ]
        }

3. If you want your fork to be publicly available, you have to give public policy to the bucket like this:

        {
          "Id": "Policy1397027253868",
          "Statement": [
            {
              "Sid": "Stmt1397027243665",
              "Action": [
                "s3:ListBucket"
              ],
              "Effect": "Allow",
              "Resource": "arn:aws:s3:::x_bucket",
              "Principal": {
                "AWS": [
                  "*"
                ]
              }
            },
            {
              "Sid": "Stmt1397027177153",
              "Action": [
                "s3:GetObject"
              ],
              "Effect": "Allow",
              "Resource": "arn:aws:s3:::x_bucket/*",
              "Principal": {
                "AWS": [
                  "*"
                ]
              }
            }
          ]
        }

4. In your maven settings.xml (linux: ~/.m2/settings.xml) you should have

        ...
        <servers>
          <server>
            <id>s3-jsk</id>
            <username>${iam-user-access-key-id}</username>
            <password>${iam-user-secret-key}</password>
          </server>
        </servers>
        ...

5. In JSK pom we already have everything you need, the only thing you have to do is to change bucket name here:
        
        <distributionManagement>
            <snapshotRepository>
                <id>s3-jsk</id>
                <url>s3://x_bucket/snapshot</url>
            </snapshotRepository>
            <repository>
                <id>s3-jsk</id>
                <url>s3://x_bucket/release</url>
            </repository>
        </distributionManagement>
        
6. To deploy artifact to S3 you have to use command:
               
        mvn --settings settings.xml clean deploy -Diam-user-access-key-id=X_IAM_ACCESS_KEY_X -Diam-user-secret-key=X_IAM_SECRET_X  -Daws.region=us-east-1;
 
-Daws.region parameter is necessary if you don't have AWS configured locally.

You can find more info here:

https://github.com/jeugene/aws-s3-maven

https://tech.asimio.net/2018/06/27/Using-an-AWS-S3-Bucket-as-your-Maven-Repository.html