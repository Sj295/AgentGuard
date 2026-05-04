package com.agentguard.audit;

public interface AuditService {

    void record(String action, String operator);
}
