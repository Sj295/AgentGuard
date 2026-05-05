package com.agentguard.cache;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisKeyBuilder {

    private static final String PREFIX = "agentguard";
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String rateLimitMinute(Long projectId) {
        return PREFIX + ":rate:ai:project:" + projectId + ":minute:" + LocalDateTime.now().format(MINUTE_FMT);
    }

    public String rateLimitDay(Long projectId) {
        return PREFIX + ":rate:ai:project:" + projectId + ":day:" + LocalDateTime.now().format(DAY_FMT);
    }

    public String aiGitDiffAnalysis(Long gitAuditReportId) {
        return PREFIX + ":ai:result:git-diff:" + gitAuditReportId;
    }

    public String aiRiskExplain(Long reportId) {
        return PREFIX + ":ai:result:risk-explain:" + reportId;
    }

    public String aiReportSummary(Long projectId, String markdown) {
        return PREFIX + ":ai:result:report-summary:" + projectId + ":" + sha256(markdown);
    }

    public static String sha256(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
