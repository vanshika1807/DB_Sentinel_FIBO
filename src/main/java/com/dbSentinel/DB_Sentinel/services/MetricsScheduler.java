package com.dbSentinel.DB_Sentinel.services;

import com.dbSentinel.DB_Sentinel.model.MetricsSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricsScheduler {
  private final MetricsCollectorService collector;
  public MetricsScheduler(MetricsCollectorService collector) { this.collector = collector; }

  @Scheduled(fixedDelayString = "${monitor.polling-interval-ms:5000}")
  public void tick() {
    try {
      MetricsSnapshot s = collector.collect();
      collector.saveAndBroadcast(s);
    } catch (Exception ignore) {}
  }
}
