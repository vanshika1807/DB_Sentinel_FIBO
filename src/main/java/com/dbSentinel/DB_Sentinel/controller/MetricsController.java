package com.dbSentinel.DB_Sentinel.controller;

import com.dbSentinel.DB_Sentinel.model.MetricRecord;
import com.dbSentinel.DB_Sentinel.repo.MetricRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class MetricsController {
  private final MetricRecordRepository repo;
  public MetricsController(MetricRecordRepository repo) { this.repo = repo; }

  @GetMapping("/api/metrics/latest")
  public MetricRecord latest() {
    return repo.findTop120ByOrderByTsDesc().stream()
        .max(Comparator.comparingLong(MetricRecord::getTs)).orElse(null);
  }

  @GetMapping("/api/metrics/history")
  public List<MetricRecord> history() {
    List<MetricRecord> list = repo.findTop120ByOrderByTsDesc();
    list.sort(Comparator.comparingLong(MetricRecord::getTs));
    return list;
  }
}
