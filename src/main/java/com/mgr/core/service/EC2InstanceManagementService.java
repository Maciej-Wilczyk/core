package com.mgr.core.service;

import com.mgr.core.constant.InstanceType;
import com.mgr.core.pojo.BenchmarkCreateRequest;
import com.mgr.core.pojo.ResultBenchmark;
import com.mgr.core.pojo.ResultFinal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mgr.core.constant.Constant.*;

@Service
@Getter
@Setter
public class EC2InstanceManagementService {

    private Ec2Client ec2;
    private S3Client s3Client;
    private Map<String, ResultBenchmark> resultMapBenchmark = new ConcurrentHashMap<>();
    private ResultFinal resultFinal;
    private boolean isTaskDone = true;
    private String ec2PublicIp;
    private int resultsSize;
    private double benchmarkMultiplicator;


    public void initializeResultMap(final BenchmarkCreateRequest benchmarkCreateRequest) {
        this.ec2 = Ec2Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
        this.s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
        this.resultMapBenchmark = new ConcurrentHashMap<>();
        this.resultsSize = benchmarkCreateRequest.getInstanceTypes().size();
        this.benchmarkMultiplicator = 100. / benchmarkCreateRequest.getBenchmarkPercentage();
        this.resultFinal = null;
    }

    public void createEC2Instance(final String instanceType, boolean isBenchmark) {
        String amiId = "ami-05e411cf591b5c9f6";
        String keyName = "vockey";
        String securityGroupId = "sg-0ffb3da4cf47eb2f8";
        String endPartOfScript = isBenchmark ?
                "aws s3 cp " + instanceType + "_result.txt s3://test-s3-sum/" + BENCHMARK + "/result/\n" +
                        "aws s3 cp " + instanceType + "_setup_time.txt s3://test-s3-sum/" + BENCHMARK + "/setup/\n" +
                        "aws s3 cp " + instanceType + "_task_time.txt s3://test-s3-sum/" + BENCHMARK + "/task/\n" +
                        "aws s3 cp " + instanceType + "_launch_time.txt s3://test-s3-sum/" + BENCHMARK + "/launch/\n" +
                        "curl -X POST http://" + this.ec2PublicIp + ":8080/api/" + BENCHMARK + "/results/" + instanceType
                :
                "aws s3 cp " + instanceType + "_result.txt s3://test-s3-sum/" + FINAL + "/result/\n" +
                        "aws s3 cp " + instanceType + "_setup_time.txt s3://test-s3-sum/" + FINAL + "/setup/\n" +
                        "aws s3 cp " + instanceType + "_task_time.txt s3://test-s3-sum/" + FINAL + "/task/\n" +
                        "aws s3 cp " + instanceType + "_launch_time.txt s3://test-s3-sum/" + FINAL + "/launch/\n" +
                        "curl -X POST http://" + this.ec2PublicIp + ":8080/api/" + FINAL + "/results/" + instanceType;

        String userDataScript = "#!/bin/bash\n" +
                "date > " + instanceType + "_launch_time.txt\n" +
                "sudo yum update -y\n" +
                "sudo yum install -y docker\n" +
                "sudo service docker start\n" +
                "sudo usermod -a -G docker ec2-user\n" +
                "date > " + instanceType + "_setup_time.txt\n" +
                "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 445146526163.dkr.ecr.us-east-1.amazonaws.com\n" +
                "docker pull " + AWS_ECR_REPOSITORY_WITH_TAG + "\n" +
                "docker run " + AWS_ECR_REPOSITORY_WITH_TAG + " >" + instanceType + "_result.txt" + "\n" +
                "date > " + instanceType + "_task_time.txt\n" +
                endPartOfScript;

        String encodedUserData = Base64.getEncoder().encodeToString(userDataScript.getBytes());

        RunInstancesRequest request = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(instanceType)
                .keyName(keyName)
                .securityGroupIds(securityGroupId)
                .minCount(1)
                .maxCount(1)
                .userData(encodedUserData)
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder().key("Name").value(instanceType).build())
                        .build())
                .iamInstanceProfile(IamInstanceProfileSpecification.builder()
                        .arn("arn:aws:iam::445146526163:instance-profile/LabInstanceProfile")
                        .build())
                .build();

        this.ec2.runInstances(request);
    }

    public void fetchResultAndTerminate(final String instanceType, final boolean isBenchmark) {
        terminateInstance(instanceType);
        //result
        String rootDirectoryName = isBenchmark ? BENCHMARK : FINAL;
        String result = getResult(rootDirectoryName + "/result/" + instanceType + "_result.txt");
        //duration
        LocalDateTime setupTime = getTimeString(rootDirectoryName + "/setup/" + instanceType + "_setup_time.txt");
        LocalDateTime taskTime = getTimeString(rootDirectoryName + "/task/" + instanceType + "_task_time.txt");
        LocalDateTime launchTime = getTimeString(rootDirectoryName + "/launch/" + instanceType + "_launch_time.txt");
        Duration setupDuration = Duration.between(launchTime, setupTime);
        Duration taskDuration = Duration.between(setupTime, taskTime);
        InstanceType instanceTypeEnum = InstanceType.fromString(instanceType);
        if (isBenchmark) {
            double estimatedCost = taskDuration.getSeconds() / 3600. *
                    InstanceType.fromString(instanceType).getCostPerHour() * this.benchmarkMultiplicator;
            double estimatedTime = taskDuration.getSeconds() * this.benchmarkMultiplicator;
            this.resultMapBenchmark.put(instanceType, new ResultBenchmark(result, setupDuration.getSeconds(),
                    taskDuration.getSeconds(), estimatedTime, estimatedCost, instanceTypeEnum));
        } else {
            double cost = taskDuration.toSeconds() / 3600. * InstanceType.fromString(instanceType).getCostPerHour();
            this.resultFinal = new ResultFinal(result, setupDuration.getSeconds(),
                    taskDuration.getSeconds(), instanceTypeEnum, cost);
        }
    }



    private void terminateInstance(String instanceType) {
        this.ec2 = Ec2Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .filters(Filter.builder()
                        .name("tag:Name")
                        .values(instanceType)
                        .build())
                .build();
        String instanceId = this.ec2.describeInstances(describeInstancesRequest)
                .reservations().stream().flatMap(v-> v.instances()
                .stream()).filter(e-> e.state().code() == 16).findFirst().map(Instance::instanceId).orElseThrow();

        System.out.println("InstanceId:" + instanceId);

        TerminateInstancesRequest terminateInstancesRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        this.ec2.terminateInstances(terminateInstancesRequest);
    }

    private String getResult(final String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("test-s3-sum")
                .key(key)
                .build();
        return new String(this.s3Client.getObjectAsBytes(getObjectRequest).asByteArray(), StandardCharsets.UTF_8);
    }

    private LocalDateTime getTimeString(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("test-s3-sum")
                .key(key)
                .build();
        String timeString = new String(this.s3Client.getObjectAsBytes(getObjectRequest).asByteArray(), StandardCharsets.UTF_8)
                .replaceAll("\\n", "");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        return LocalDateTime.parse(timeString, formatter);
    }
}