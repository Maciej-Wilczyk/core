package com.mgr.core.pojo;

import com.mgr.core.constant.InstanceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResultBenchmark {
    private String result;
    private long setupDuration;
    private long taskDuration;
    private double estimatedFinalTaskDuration;
    private double estimatedFinalCost;
    private InstanceType instanceType;
}
