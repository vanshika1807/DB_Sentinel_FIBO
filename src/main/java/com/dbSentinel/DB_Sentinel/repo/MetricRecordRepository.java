package com.dbSentinel.DB_Sentinel.repo;

import com.dbSentinel.DB_Sentinel.model.MetricRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetricRecordRepository extends JpaRepository<MetricRecord, Long> {
  List<MetricRecord> findTop120ByOrderByTsDesc();
}
