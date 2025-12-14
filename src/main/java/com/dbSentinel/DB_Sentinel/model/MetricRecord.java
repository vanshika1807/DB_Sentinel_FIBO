package com.dbSentinel.DB_Sentinel.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metric_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetricRecord {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private long ts;

  private int threadsConnected;
  private long questionsTotal;
  private long slowQueriesTotal;
  private double qps;
  private double avgQueryTimeMs;

  private double systemCpuLoadPct;
  private long freeMemoryBytes;
  private long totalMemoryBytes;
}
