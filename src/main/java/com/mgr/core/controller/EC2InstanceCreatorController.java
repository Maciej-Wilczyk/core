package com.mgr.core.controller;

import com.amazonaws.SdkClientException;
import com.mgr.core.pojo.EC2Instance;
import com.mgr.core.pojo.ResultCostTime;
import com.mgr.core.service.EC2InstanceCreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EC2InstanceCreatorController {

    private final EC2InstanceCreatorService ec2InstanceCreatorService;
    private ResultCostTime resultCostTime;

    @PostMapping(path = "ec2", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> createEC2Instance (@RequestBody EC2Instance ec2Instance) throws  SdkClientException {
        ec2InstanceCreatorService.createEC2Instance(ec2Instance);
        return ResponseEntity.ok("Instance is creating");
    }

    @PostMapping("/results")
    public void ec2ReadyNotification() throws IOException {
        resultCostTime = ec2InstanceCreatorService.getResultAndTerminate();
        ec2InstanceCreatorService.setTaskDone(true);
    }

    @GetMapping("/results")
    public ResponseEntity<ResultCostTime> checkBuildStatus() {
        if (ec2InstanceCreatorService.isTaskDone()){
            ResponseEntity.ok(resultCostTime);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
