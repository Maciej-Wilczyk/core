package com.mgr.core.controller;

import com.mgr.core.pojo.Data;
import com.mgr.core.pojo.ResultFinal;
import com.mgr.core.service.EC2InstanceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/final")
@RequiredArgsConstructor
public class EC2InstanceManagementFinalController {

    private final EC2InstanceManagementService ec2InstanceManagementService;

    @PostMapping(path = "/create")
    public ResponseEntity<Data<String>> createEC2InstanceFinal(@RequestParam String instanceType) {
        ec2InstanceManagementService.createEC2Instance(instanceType, false);
        return ResponseEntity.ok(new Data<>("Final instance is created"));
    }

    @PostMapping(path = "/results/{instanceType}")
    public void ec2ReadyNotificationFinal(@PathVariable final String instanceType) {
        System.out.println("Notification: " + instanceType);
        ec2InstanceManagementService.fetchResultAndTerminate(instanceType, false);
    }

    @GetMapping(path = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Data<ResultFinal>> getFinalResults() {
        if (ec2InstanceManagementService.getResultFinal() != null) {
            return ResponseEntity.ok(new Data<>(ec2InstanceManagementService.getResultFinal()));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new Data<>());
    }
}
