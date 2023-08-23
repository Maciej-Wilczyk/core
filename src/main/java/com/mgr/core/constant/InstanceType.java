package com.mgr.core.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.mgr.core.constant.Constant.*;

@AllArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum InstanceType {

    T2_MICRO("t2.micro", 0.0116, GENERAL_PURPOSE, 1, 1),
    T2_SMALL("t2.small", 0.023, GENERAL_PURPOSE, 1, 2),
    T2_MEDIUM ("t2.medium", 0.0464, GENERAL_PURPOSE, 2, 4),
    T2_LARGE ("t2.large", 0.1856, GENERAL_PURPOSE, 4, 8),
    C7G_MEDIUM ("c7g.medium", 0.0363, COMPUTE_OPTIMIZED, 1, 2),
    C7G_LARGE ("c7g.large", 0.0725, COMPUTE_OPTIMIZED, 2, 4),
    C7G_XLARGE ("c7g.xlarge", 0.145, COMPUTE_OPTIMIZED, 4, 8),
    R7G_MEDIUM ("r7g.medium", 0.0536, MEMORY_OPTIMIZED, 1, 8),
    R7G_LARGE ("r7g.large", 0.1071, MEMORY_OPTIMIZED, 2, 16),
    R7G_XLARGE ("r7g.xlarge", 0.2142, MEMORY_OPTIMIZED, 4, 32);

    private String type;
    private double costPerHour;
    private String category;
    private int numberOfVCPU;
    private int memoryInGiB;

    public static InstanceType fromString(String value) {
        for (InstanceType instanceType : InstanceType.values()) {
            if (instanceType.getType().equalsIgnoreCase(value)) {
                return instanceType;
            }
        }
        throw new IllegalArgumentException("No matching enum constant for value: " + value);
    }
}
