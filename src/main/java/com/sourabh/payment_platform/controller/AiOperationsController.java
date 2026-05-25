package com.sourabh.payment_platform.controller;

import com.sourabh.payment_platform.ai.AnalyzeFailureRequest;
import com.sourabh.payment_platform.ai.FailureAnalysisResponse;
import com.sourabh.payment_platform.ai.FailureAnalysisService;
import com.sourabh.payment_platform.ai.GenerateIncidentReportRequest;
import com.sourabh.payment_platform.ai.IncidentReportResponse;
import com.sourabh.payment_platform.ai.IncidentReportService;
import com.sourabh.payment_platform.ai.LogSummarizationRequest;
import com.sourabh.payment_platform.ai.LogSummarizationService;
import com.sourabh.payment_platform.ai.LogSummaryResponse;
import com.sourabh.payment_platform.ai.OperationalQueryRequest;
import com.sourabh.payment_platform.ai.OperationalQueryResponse;
import com.sourabh.payment_platform.ai.OperationalQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiOperationsController {

    private final FailureAnalysisService failureAnalysisService;
    private final IncidentReportService incidentReportService;
    private final LogSummarizationService logSummarizationService;
    private final OperationalQueryService operationalQueryService;

    @PostMapping("/analyze-failure")
    public FailureAnalysisResponse analyzeFailure(@Valid @RequestBody AnalyzeFailureRequest request) {
        return failureAnalysisService.analyzeFailure(request.transactionId());
    }

    @PostMapping("/generate-incident-report")
    public IncidentReportResponse generateIncidentReport(@Valid @RequestBody GenerateIncidentReportRequest request) {
        return incidentReportService.generateIncidentReport(request.minutes());
    }

    @PostMapping("/summarize-logs")
    public LogSummaryResponse summarizeLogs(@Valid @RequestBody LogSummarizationRequest request) {
        return logSummarizationService.summarizeLogs(request.logs());
    }

    @PostMapping("/query")
    public OperationalQueryResponse answerOperationalQuestion(@Valid @RequestBody OperationalQueryRequest request) {
        return operationalQueryService.answerOperationalQuestion(request.question());
    }
}
