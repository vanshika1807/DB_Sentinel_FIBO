package com.dbSentinel.DB_Sentinel.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class AlertService {

  @Autowired
  private IncidentNotifier incidentNotifier;

  private final JavaMailSender mailSender;

  @Value("${monitor.alerts.enabled:true}") private boolean enabled;
  @Value("${monitor.alerts.recipients}") private String recipients;
  @Value("${monitor.alerts.maxActiveConnections:80}") private int maxConn;
  @Value("${monitor.alerts.maxAvgQueryTimeMs:2000}") private long maxAvgMs;
  @Value("${monitor.alerts.maxErrorRatePerMin:5}") private int maxErrRate;

  public AlertService(JavaMailSender mailSender) { this.mailSender = mailSender; }

  public void maybeAlert(int threadsConnected, double avgQueryTimeMs, int recentErrorsPerMin) {
    if (!enabled) return;
    StringBuilder sb = new StringBuilder(); boolean trigger = false;

    if (threadsConnected > maxConn) { trigger = true;
      sb.append("Active connections high: ").append(threadsConnected).append(" > ").append(maxConn).append("\n"); }
    if (avgQueryTimeMs > maxAvgMs) { trigger = true;
      sb.append("Avg query time high: ").append((long)avgQueryTimeMs).append("ms > ").append(maxAvgMs).append("ms\n"); }
    if (recentErrorsPerMin > maxErrRate) { trigger = true;
      sb.append("Error rate high: ").append(recentErrorsPerMin).append(" > ").append(maxErrRate).append("\n"); }

    if (trigger) {
      try {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(recipients.split(","));
        msg.setSubject("[DB Sentinel] Alert");
        msg.setText(sb.toString());
        mailSender.send(msg);
        incidentNotifier.sendIncident(sb.toString());
      } catch (Exception ignore) {}
    }
  }
}
