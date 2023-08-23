package com.mgr.core.pojo;

import lombok.Getter;

import java.util.List;

@Getter
public class BenchmarkCreateRequest {
    private List<String> instanceTypes;
    private double benchmarkPercentage;
}
