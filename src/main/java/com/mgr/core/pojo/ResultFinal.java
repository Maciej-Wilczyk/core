package com.mgr.core.pojo;

import com.mgr.core.constant.InstanceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResultFinal {
    private String result;
    private long setupDuration;
    private long taskDuration;
    private InstanceType instanceType;
    private double cost;
}
