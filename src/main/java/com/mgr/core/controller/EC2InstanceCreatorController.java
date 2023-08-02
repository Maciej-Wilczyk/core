package com.mgr.core.controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.mgr.core.pojo.EC2Instance;
import com.mgr.core.service.EC2InstanceCreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EC2InstanceCreatorController {

    private final EC2InstanceCreatorService ec2InstanceCreatorService;

    @PostMapping(path = "ec2", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> createEC2Instance (@RequestBody EC2Instance ec2Instance) throws  SdkClientException {
        ec2InstanceCreatorService.createEC2Instance(ec2Instance);
        return ResponseEntity.ok("Instance is creating");
    }
}
