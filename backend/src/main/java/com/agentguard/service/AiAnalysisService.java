package com.agentguard.service;

/**
 * AI 分析模块预留接口，后续可接入真实模型服务。
 */
public interface AiAnalysisService {

    String analyze(String projectContext, String instruction);
}
