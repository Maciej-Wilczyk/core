package com.mgr.core.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.mgr.core.pojo.EC2Instance;
import org.springframework.stereotype.Service;

@Service
public class EC2InstanceCreatorService {

    public void createEC2Instance (EC2Instance ec2Instance) throws SdkClientException {
        String amiId = "ami-05e411cf591b5c9f6";
        String instanceType = ec2Instance.getInstanceType();
        String keyName = "vockey";
        String securityGroupId = "sg-0ffb3da4cf47eb2f8";

        AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .build();

        RunInstancesRequest request = new RunInstancesRequest()
                .withImageId(amiId)
                .withInstanceType(instanceType)
                .withKeyName(keyName)
                .withSecurityGroupIds(securityGroupId)
                .withMinCount(1)
                .withMaxCount(1);
        ec2.runInstances(request);

        // Authenticate with ECR
        GetAuthorizationTokenRequest authRequest = new GetAuthorizationTokenRequest();
        GetAuthorizationTokenResult authResult = ec2.getAuthorizationToken(authRequest);
        String authorizationToken = authResult.getAuthorizationData().get(0).getAuthorizationToken();

// Decode the authentication token
        String decodedToken = new String(Base64.decodeBase64(authorizationToken));
        String[] tokenParts = decodedToken.split(":");

// Set the Docker credentials
        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withUsername(tokenParts[0])
                .withPassword(tokenParts[1])
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();

// Pull the Docker image from ECR
        String ecrImageUri = "123456789012.dkr.ecr.us-east-1.amazonaws.com/my-ecr-repo:my-image-tag";
        String dockerImageName = "my-image-tag";
        dockerClient.pullImageCmd(ecrImageUri).start().awaitCompletion();

// Create a Docker container from the pulled image
        String containerName = "my-container";
        CreateContainerResponse container = dockerClient.createContainerCmd(dockerImageName)
                .withName(containerName)
                .exec();

// Start the Docker container
        dockerClient.startContainerCmd(container.getId()).exec();
    }
}
