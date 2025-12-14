package com.dbSentinel.DB_Sentinel.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricsSnapshot {
  // MySQL
  private int threadsConnected;
  private long questionsTotal;
  private long slowQueriesTotal;
  private double qps;
  private double avgQueryTimeMs;

  // System (coarse from JVM/OS)
  private double systemCpuLoadPct;
  private long freeMemoryBytes;
  private long totalMemoryBytes;

  private long timestamp;
}
