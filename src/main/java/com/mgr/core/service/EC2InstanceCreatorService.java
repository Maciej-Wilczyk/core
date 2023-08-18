package com.mgr.core.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.costexplorer.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.mgr.core.pojo.EC2Instance;
import com.mgr.core.pojo.ResultCostTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;

import static com.mgr.core.constant.Constant.AWS_ECR_REPOSITORY_WITH_TAG;

@Service
@Getter
@Setter
public class EC2InstanceCreatorService {

    private Instance newEc2Instance;
    private AWSCredentialsProvider credentialsProvider;
    private AmazonEC2 ec2;

    private boolean isTaskDone = true;


    public void createEC2Instance(EC2Instance ec2Instance) {
        isTaskDone = false;
        String amiId = "ami-05e411cf591b5c9f6";
        String instanceType = ec2Instance.getInstanceType();
        String keyName = "vockey";
        String securityGroupId = "sg-0ffb3da4cf47eb2f8";
        String userDataScript = "#!/bin/bash\n" +
                "yum update -y\n" +
                "yum install -y docker\n" +
                "service docker start\n" +
                "usermod -a -G docker ec2-user\n" +
                "setup_time=$(date -u +'%Y-%m-%d %H:%M:%S')\n" +
                "echo \"setup_time\" > setup_time.txt\n" +
                "docker pull " + AWS_ECR_REPOSITORY_WITH_TAG + "\n" +
                "docker run " + AWS_ECR_REPOSITORY_WITH_TAG + " > result.txt" + "\n" +
                "task_time=$(date -u +'%Y-%m-%d %H:%M:%S')\n" +
                "echo \"task_time\" > /tmp/task_time.txt\n" +
                "aws s3 cp result.txt s3://test-s3-sum/result/\n" +
                "aws s3 cp setup_time.txt s3://test-s3-sum/setup/\n" +
                "aws s3 cp task_time.txt s3://test-s3-sum/task/\n" +
                "curl -X POST http://<EC2_INSTANCE_A_PUBLIC_IP>:<PORT>/ec2ReadyNotification";

        String encodedUserData = Base64.getEncoder().encodeToString(userDataScript.getBytes());

        credentialsProvider = new ProfileCredentialsProvider();
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .build();

        RunInstancesRequest request = new RunInstancesRequest()
                .withImageId(amiId)
                .withInstanceType(instanceType)
                .withKeyName(keyName)
                .withSecurityGroupIds(securityGroupId)
                .withMinCount(1)
                .withMaxCount(1)
                .withUserData(encodedUserData)
                .withTagSpecifications(new TagSpecification().withResourceType(ResourceType.Instance)
                        .withTags(new Tag().withKey("Name").withValue("worker")))
                .withIamInstanceProfile(new IamInstanceProfileSpecification()
                        .withArn("arn:aws:iam::445146526163:instance-profile/LabInstanceProfile"));

        newEc2Instance = ec2.runInstances(request).getReservation().getInstances().get(0);
    }

    public ResultCostTime getResultAndTerminate() throws SdkClientException, IOException {

        String instanceId = newEc2Instance.getInstanceId();

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date launchTime = newEc2Instance.getLaunchTime();

        //result
        String result = getResult(s3Client);

        //duration
        LocalDateTime setupTime = getTimeString(s3Client, "setup/setup_time.txt");
        LocalDateTime taskTime = getTimeString(s3Client, "task/task_time.txt");
        Duration setupDuration = Duration.between(launchTime.toInstant(), setupTime.atZone(ZoneOffset.UTC).toInstant());
        Duration taskDuration = Duration.between(setupTime, taskTime.atZone(ZoneOffset.UTC).toInstant());


        //cost
        AWSCostExplorer costExplorerClient = AWSCostExplorerClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();
        GetCostAndUsageRequest costRequest = new GetCostAndUsageRequest()
                .withTimePeriod(new DateInterval()
                        .withStart(dateFormat.format(launchTime))
                        .withEnd(dateFormat.format(new Date())))
                .withGranularity(Granularity.HOURLY)
                .withMetrics("BlendedCost")
                .withFilter(new Expression()
                        .withDimensions(new DimensionValues()
                                .withKey("InstanceId")
                                .withValues(newEc2Instance.getInstanceId())
                        )
                );

        GetCostAndUsageResult costResult = costExplorerClient.getCostAndUsage(costRequest);

        double totalCost = costResult.getResultsByTime().stream()
                .flatMap(list -> list.getGroups().stream())
                .flatMap(map -> map.getMetrics().values().stream())
                .map(metricValue -> Double.parseDouble(metricValue.getAmount()))
                .reduce(0.0, Double::sum);

        Duration totalDuration = Duration.between(launchTime.toInstant(), Instant.now());

        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
                .withInstanceIds(instanceId);

        ec2.terminateInstances(terminateInstancesRequest);
        return new ResultCostTime(
                result, setupDuration.toString(), taskDuration.toString(), totalDuration.toString(),
                Double.toString(totalCost));
    }

    private String getResult(AmazonS3 s3Client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                s3Client.getObject(new GetObjectRequest("test-s3-sum", "result/result.txt"))
                        .getObjectContent()));

        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
            content.append(System.lineSeparator());
        }
        return content.toString();
    }

    private LocalDateTime getTimeString(AmazonS3 s3Client, String path) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                s3Client.getObject(new GetObjectRequest("test-s3-sum", path))
                        .getObjectContent()));
        String timeString;
        try {
            timeString = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read time from S3 bucket", e);
        }

        return LocalDateTime.parse(timeString, formatter);
    }
}
