package com.mgr.core.service;

import com.mgr.core.pojo.ResultBenchmark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultSolverService {

    private final EC2InstanceManagementService ec2InstanceManagementService;

    public Map<String, ResultBenchmark> getFilteredResult(final Double maxCost, final Double maxTime) {
        Map<String, ResultBenchmark> resultMap = ec2InstanceManagementService.getResultMapBenchmark();
        return resultMap.entrySet().stream()
                .filter(value -> {
                    if (maxTime != null) {
                        return value.getValue().getEstimatedFinalTaskDuration() < maxTime;
                    }
                    return true;
                }).filter(value -> {
                    if (maxCost != null) {
                        return value.getValue().getEstimatedFinalCost() < maxCost;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
