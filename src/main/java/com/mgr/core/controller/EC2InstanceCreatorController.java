package com.mgr.core.controller;

import com.mgr.core.constant.InstanceType;
import com.mgr.core.pojo.BenchmarkCreateRequest;
import com.mgr.core.pojo.ResultBenchmark;
import com.mgr.core.pojo.ResultFinal;
import com.mgr.core.service.EC2InstanceManagementService;
import com.mgr.core.service.ResultSolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EC2InstanceCreatorController {

    private final EC2InstanceManagementService ec2InstanceManagementService;
    private final ResultSolverService resultSolverService;


    @GetMapping(path = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceType[]> getInstances() {
        return ResponseEntity.ok(InstanceType.values());
    }

    @PostMapping(path = "benchmark/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createEC2InstanceBenchmark(@RequestBody final BenchmarkCreateRequest benchmarkCreateRequest) {
        ec2InstanceManagementService.initializeResultMap(benchmarkCreateRequest);
        benchmarkCreateRequest.getInstanceTypes().forEach(instanceType ->
                ec2InstanceManagementService.createEC2Instance(instanceType, true));
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/benchmark/results/{instanceType}")
    public void ec2ReadyNotificationBenchmark(@PathVariable final String instanceType) {
        ec2InstanceManagementService.fetchResultAndTerminate(instanceType, true);
    }

    @GetMapping(path = "/benchmark/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, ResultBenchmark>> getBenchmarkResults(
            @RequestParam(required = false) Double maxCost, @RequestParam(required = false) Double maxTime) {
        if (ec2InstanceManagementService.getResultMapBenchmark().size() == ec2InstanceManagementService.getResultsSize()) {
            return ResponseEntity.ok(resultSolverService.getFilteredResult(maxCost, maxTime));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping(path = "final/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createEC2InstanceFinal(@RequestParam String instanceType) {
        ec2InstanceManagementService.createEC2Instance(instanceType, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/final/results/{instanceType}")
    public void ec2ReadyNotificationFinal(@PathVariable final String instanceType) {
        ec2InstanceManagementService.fetchResultAndTerminate(instanceType, false);
    }

    @GetMapping(path = "/final/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultFinal> getFinalResults() {
        if (ec2InstanceManagementService.getResultFinal() != null) {
            return ResponseEntity.ok(ec2InstanceManagementService.getResultFinal());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
