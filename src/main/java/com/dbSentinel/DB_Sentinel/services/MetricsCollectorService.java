package com.dbSentinel.DB_Sentinel.services;

import com.dbSentinel.DB_Sentinel.model.MetricRecord;
import com.dbSentinel.DB_Sentinel.model.MetricsSnapshot;
import com.dbSentinel.DB_Sentinel.repo.MetricRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsCollectorService extends TextWebSocketHandler {

  @Autowired
  private IncidentNotifier incidentNotifier;


  private final JdbcTemplate jdbc;
  private final MetricRecordRepository repo;
  private final AlertService alerts;
  private final ObjectMapper mapper = new ObjectMapper();

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
  private volatile long lastQuestions = 0L;
  private volatile long lastTs = 0L;
  private final Map<String, Long> lastCounters = new ConcurrentHashMap<>();

  public MetricsCollectorService(JdbcTemplate jdbc, MetricRecordRepository repo, AlertService alerts) {
    this.jdbc = jdbc; this.repo = repo; this.alerts = alerts;
  }

  @Override public void afterConnectionEstablished(WebSocketSession s) { sessions.add(s); }
  @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) { sessions.remove(s); }

  public MetricsSnapshot collect() {
    long now = System.currentTimeMillis();

    Map<String, Long> status = jdbc.query(
      "SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Questions','Slow_queries','Com_select','Com_insert','Com_update','Com_delete')",
      rs -> { var m = new java.util.HashMap<String, Long>(); while (rs.next()) m.put(rs.getString("Variable_name"), rs.getLong("Value")); return m; });

    int threads = status.getOrDefault("Threads_connected", 0L).intValue();
    long questions = status.getOrDefault("Questions", 0L);
    long slow = status.getOrDefault("Slow_queries", 0L);

    double qps = 0.0;
    if (lastTs != 0 && now > lastTs) {
      qps = (questions - lastQuestions) / ((now - lastTs) / 1000.0);
      if (qps < 0) qps = 0;
    }
    lastQuestions = questions; lastTs = now;

    long prevSlow = lastCounters.getOrDefault("Slow_queries", slow);
    lastCounters.put("Slow_queries", slow);
    long slowDelta = Math.max(0, slow - prevSlow);
    double avgMs = slowDelta > 0 ? Math.max(2000.0, 2000.0 + slowDelta * 50)
                                 : Math.max(1.0, 1000.0 / Math.max(0.0001, qps));

    OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    double cpuPct = Math.max(0, os.getSystemCpuLoad() * 100.0);
    long totalMem = os.getTotalMemorySize();
    long freeMem = os.getFreeMemorySize();

    return MetricsSnapshot.builder()
        .threadsConnected(threads).questionsTotal(questions).slowQueriesTotal(slow)
        .qps(qps).avgQueryTimeMs(avgMs)
        .systemCpuLoadPct(cpuPct).freeMemoryBytes(freeMem).totalMemoryBytes(totalMem)
        .timestamp(now).build();
  }

  public void saveAndBroadcast(MetricsSnapshot s) {
    repo.save(MetricRecord.builder()
        .ts(s.getTimestamp())
        .threadsConnected(s.getThreadsConnected())
        .questionsTotal(s.getQuestionsTotal())
        .slowQueriesTotal(s.getSlowQueriesTotal())
        .qps(s.getQps())
        .avgQueryTimeMs(s.getAvgQueryTimeMs())
        .systemCpuLoadPct(s.getSystemCpuLoadPct())
        .freeMemoryBytes(s.getFreeMemoryBytes())
        .totalMemoryBytes(s.getTotalMemoryBytes())
        .build());

    alerts.maybeAlert(s.getThreadsConnected(), s.getAvgQueryTimeMs(), 0);

    try {
      String json = mapper.writeValueAsString(s);
      TextMessage msg = new TextMessage(json);
      for (WebSocketSession ws : sessions) try { ws.sendMessage(msg); } catch (Exception ignore) {}
    } catch (Exception e) {
    incidentNotifier.sendIncident("Exception during metric collection: " + e.getMessage());
  }

}

}
