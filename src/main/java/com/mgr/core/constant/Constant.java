package com.mgr.core.constant;

public interface Constant {
    // String TAG = "sum-test:latest";
     String TAG = "test-2:latest";

     String AWS_ECR_REPOSITORY = "445146526163.dkr.ecr.us-east-1.amazonaws.com";

     String AWS_ECR_REPOSITORY_WITH_TAG = AWS_ECR_REPOSITORY + "/" + TAG;

     String GENERAL_PURPOSE = "General Purpose";

     String COMPUTE_OPTIMIZED = "Compute Optimized";

     String MEMORY_OPTIMIZED = "Memory Optimized";

     String BENCHMARK = "benchmark";

     String FINAL = "final";
}
