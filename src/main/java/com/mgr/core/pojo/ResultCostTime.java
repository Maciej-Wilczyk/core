package com.mgr.core.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ResultCostTime {
    private String result;
    private String setupDuration;
    private String taskDuration;
    private String totalDuration;
    private String totalCost;
}
