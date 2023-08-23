//package com.mgr.core.service;
//
//import com.amazonaws.SdkClientException;
//import com.amazonaws.auth.AWSCredentialsProvider;
//import com.amazonaws.auth.profile.ProfileCredentialsProvider;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.ec2.AmazonEC2;
//import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
//import com.amazonaws.services.ec2.model.*;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.mgr.core.constant.InstanceType;
//import com.mgr.core.pojo.UserRequestInput;
//import com.mgr.core.pojo.ResultCostTime;
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.time.Duration;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//import static com.mgr.core.constant.Constant.AWS_ECR_REPOSITORY_WITH_TAG;
//
//@Service
//@Getter
//@Setter
//public class EC2InstanceCreatorServiceOld {
//
//    private Instance newEc2Instance;
//    private AWSCredentialsProvider credentialsProvider;
//    private AmazonEC2 ec2;
//    private UserRequestInput userRequestInput;
//
//    private Map<String, ResultCostTime> results;
//
//    private boolean isTaskDone = true;
//
//    private String ec2PublicIp;
//
//    public void initializeResultMap(List<String> instanceTypes) {
//        this.results = new HashMap<>();
//        instanceTypes.forEach(instanceType -> this.results.put(instanceType, null));
//    }
//
//
//    public void createEC2Instance(String instanceType, UserRequestInput userRequestInput) {
//        this.userRequestInput = userRequestInput;
//        this.isTaskDone = false;
//        String amiId = "ami-05e411cf591b5c9f6";
//        String keyName = "vockey";
//        String securityGroupId = "sg-0ffb3da4cf47eb2f8";
//        String userDataScript = "#!/bin/bash\n" +
//                "sudo yum update -y\n" +
//                "sudo yum install -y docker\n" +
//                "sudo service docker start\n" +
//                "sudo usermod -a -G docker ec2-user\n" +
//                "date > setup_time.txt\n" +
//                "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 445146526163.dkr.ecr.us-east-1.amazonaws.com\n" +
//                "docker pull " + AWS_ECR_REPOSITORY_WITH_TAG + "\n" +
//                "docker run " + AWS_ECR_REPOSITORY_WITH_TAG + " > result.txt" + "\n" +
//                "date > task_time.txt\n" +
//                "aws s3 cp result.txt s3://test-s3-sum/result/\n" +
//                "aws s3 cp setup_time.txt s3://test-s3-sum/setup/\n" +
//                "aws s3 cp task_time.txt s3://test-s3-sum/task/\n" +
//                "curl -X POST -H \"Content-Type: application/json\" -d '{\"key1\":\"value1\", \"key2\":\"value2\"}' http://" + this.ec2PublicIp + ":8080/results";
//
//        String encodedUserData = Base64.getEncoder().encodeToString(userDataScript.getBytes());
//
//        credentialsProvider = new ProfileCredentialsProvider();
//        this.ec2 = AmazonEC2ClientBuilder.standard()
//                .withCredentials(credentialsProvider)
//                .build();
//
//        RunInstancesRequest request = new RunInstancesRequest()
//                .withImageId(amiId)
//                .withInstanceType(instanceType)
//                .withKeyName(keyName)
//                .withSecurityGroupIds(securityGroupId)
//                .withMinCount(1)
//                .withMaxCount(1)
//                .withUserData(encodedUserData)
//                .withTagSpecifications(new TagSpecification().withResourceType(ResourceType.Instance)
//                        .withTags(new Tag().withKey("Name").withValue("worker")))
//                .withIamInstanceProfile(new IamInstanceProfileSpecification()
//                        .withArn("arn:aws:iam::445146526163:instance-profile/LabInstanceProfile"));
//
//        this.newEc2Instance = this.ec2.runInstances(request).getReservation().getInstances().get(0);
//    }
//
//    public ResultCostTime getResultAndTerminate() throws SdkClientException, IOException {
//        String instanceId = this.newEc2Instance.getInstanceId();
//        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//                .withCredentials(credentialsProvider)
//                .withRegion(Regions.US_EAST_1)
//                .build();
//        Date launchTime = this.newEc2Instance.getLaunchTime();
//
//        System.out.println(launchTime);
//        System.out.println(launchTime.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
//
//        //result
//        String result = getResult(s3Client);
//
//        //duration
//        LocalDateTime setupTime = getTimeString(s3Client, "setup/setup_time.txt");
//        LocalDateTime taskTime = getTimeString(s3Client, "task/task_time.txt");
//        Duration setupDuration = Duration.between(launchTime.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(), setupTime);
//        Duration taskDuration = Duration.between(setupTime, taskTime);
//
//        //cost
//        Duration totalDuration = Duration.between(launchTime.toInstant(), Instant.now());
//        double totalCost = totalDuration.toSeconds() / 3600. * InstanceType.fromString(this.userRequestInput.getInstanceType())
//                .getCostPerHour();
//
//        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
//                .withInstanceIds(instanceId);
//        this.ec2.terminateInstances(terminateInstancesRequest);
//        this.isTaskDone = true;
//        this.results.put(instanceId, new ResultCostTime(
//                result, setupDuration.toSeconds(), taskDuration.toSeconds(), totalDuration.toSeconds(),
//                Double.toString(totalCost)));
//        return new ResultCostTime(
//                result, setupDuration.toSeconds(), taskDuration.toSeconds(), totalDuration.toSeconds(),
//                Double.toString(totalCost));
//    }
//
//    private String getResult(AmazonS3 s3Client) throws IOException {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(
//                s3Client.getObject(new GetObjectRequest("test-s3-sum", "result/result.txt"))
//                        .getObjectContent()));
//
//        StringBuilder content = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            content.append(line);
//            content.append(System.lineSeparator());
//        }
//        return content.toString();
//    }
//
//    private LocalDateTime getTimeString(AmazonS3 s3Client, String path) {
//        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss 'UTC' yyyy", Locale.ENGLISH);
//        BufferedReader reader = new BufferedReader(new InputStreamReader(
//                s3Client.getObject(new GetObjectRequest("test-s3-sum", path))
//                        .getObjectContent()));
//        String timeString;
//        try {
//            timeString = reader.readLine();
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to read time from S3 bucket", e);
//        }
//
//        return LocalDateTime.parse(timeString, formatter);
//    }
//}
