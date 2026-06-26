package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.recommendation.exception.RecommendationGenerationException;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationTiming;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.prompt.JazzAgentPromptBuilder;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolCall;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolDispatcher;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JazzAgentService {

    private static final int DEFAULT_MAX_TOOL_STEPS = 3;
    private static final int LOG_PREVIEW_LIMIT = 2_000;

    private final JazzToolDispatcher toolDispatcher;
    private final JazzAgentPromptBuilder promptBuilder;
    private final JazzAgentModelClient modelClient;

    public JazzAgentService(
            JazzToolDispatcher toolDispatcher,
            JazzAgentPromptBuilder promptBuilder,
            JazzAgentModelClient modelClient
    ) {
        this.toolDispatcher = toolDispatcher;
        this.promptBuilder = promptBuilder;
        this.modelClient = modelClient;
    }

    public RecommendationResult run(JazzAgentContext context) {
        var startedAt = System.nanoTime();
        var run = new JazzAgentRun(context, DEFAULT_MAX_TOOL_STEPS);
        var availableTools = toolDispatcher.registeredTools();
        var systemPrompt = promptBuilder.buildSystemPrompt(context, availableTools);
        if (systemPrompt.isBlank()) {
            throw new IllegalStateException("Jazz agent system prompt must not be blank");
        }

        var turn = modelClient.createInitialResponse(context, systemPrompt, availableTools);
        run.addUsage(turn.usage());
        logTurnUsage("initial", 0, turn);

        while (turn.hasToolCalls()) {
            if (!run.canContinueToolLoop()) {
                throw new RecommendationGenerationException(
                        "Jazz agent exceeded max tool steps: " + summarizeToolCalls(turn.toolCalls())
                );
            }

            run.incrementToolStepCount();
            logPlannedToolCalls(run.toolStepCount(), turn.toolCalls());
            var toolResults = turn.toolCalls().stream()
                    .map(toolCall -> executeTool(toolCall, run))
                    .toList();
            if (turn.responseId() == null || turn.responseId().isBlank()) {
                throw new RecommendationGenerationException("Jazz agent tool turn is missing responseId for follow-up");
            }
            turn = modelClient.createFollowUpResponse(
                    context,
                    systemPrompt,
                    turn.responseId(),
                    availableTools,
                    toolResults
            );
            run.addUsage(turn.usage());
            logTurnUsage("follow_up", run.toolStepCount(), turn);
        }

        if (!turn.hasFinalAnswer()) {
            throw new RecommendationGenerationException("Jazz agent finished without a structured final answer");
        }
        var finalAnswer = turn.finalAnswer();
        logFinalAnswer(finalAnswer, run);

        return new RecommendationResult(
                finalAnswer.assistantResponse(),
                finalAnswer.winners(),
                finalAnswer.recommendationType(),
                finalAnswer.suggestedChatTitle(),
                finalAnswer.updatedSessionSummary(),
                buildTiming(startedAt),
                run.usageEntries()
        );
    }

    public JazzToolDispatcher toolDispatcher() {
        return toolDispatcher;
    }

    private JazzAgentToolResult executeTool(JazzAgentToolCallRequest toolCall, JazzAgentRun run) {
        var execution = toolDispatcher.dispatch(
                new JazzToolCall(
                        toolCall.toolName(),
                        toolCall.arguments()
                ),
                run.context()
        );
        run.addToolExecution(execution);
        logToolExecution(run.toolStepCount(), toolCall, execution);
        return new JazzAgentToolResult(toolCall.callId(), execution);
    }

    private RecommendationTiming buildTiming(long startedAt) {
        var totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        return new RecommendationTiming(0L, totalMs, totalMs);
    }

    private String summarizeToolCalls(List<JazzAgentToolCallRequest> toolCalls) {
        return toolCalls.stream()
                .map(toolCall -> toolCall.toolName().name())
                .collect(Collectors.joining(", "));
    }

    private void logTurnUsage(String stage, int step, JazzAgentModelTurnResponse turn) {
        var usage = turn.usage();
        if (usage == null) {
            return;
        }
        log.info(
                """
                        
                        === Jazz Agent Turn ===
                        stage: {}
                        step: {}
                        responseId: {}
                        toolCalls: {}
                        toolNames: {}
                        inputTokens: {}
                        cachedInputTokens: {}
                        outputTokens: {}
                        totalTokens: {}
                        =======================
                        """,
                stage,
                step,
                turn.responseId(),
                turn.toolCalls().size(),
                turn.toolCalls().stream().map(toolCall -> toolCall.toolName().name()).toList(),
                safe(usage.inputTokens()),
                safe(usage.cachedInputTokens()),
                safe(usage.outputTokens()),
                safe(usage.inputTokens()) + safe(usage.outputTokens())
        );
    }

    private void logFinalAnswer(JazzAgentFinalAnswer finalAnswer, JazzAgentRun run) {
        log.info(
                """
                        
                        === Jazz Agent Final Answer ===
                        resultType: {}
                        recommendationType: {}
                        winners: {}
                        totalSteps: {}
                        totalToolsExecuted: {}
                        totalInputTokens: {}
                        totalCachedInputTokens: {}
                        totalOutputTokens: {}
                        ===============================
                        """,
                finalAnswer.resultType(),
                finalAnswer.recommendationType(),
                finalAnswer.winners().stream().map(winner -> winner.name() + " (" + winner.id() + ")").toList(),
                run.toolStepCount(),
                run.toolExecutions().size(),
                run.usageEntries().stream().mapToInt(usage -> safe(usage.inputTokens())).sum(),
                run.usageEntries().stream().mapToInt(usage -> safe(usage.cachedInputTokens())).sum(),
                run.usageEntries().stream().mapToInt(usage -> safe(usage.outputTokens())).sum()
        );
    }

    private void logPlannedToolCalls(int step, List<JazzAgentToolCallRequest> toolCalls) {
        log.info(
                """
                        
                        === Jazz Agent Tool Plan ===
                        step: {}
                        toolCalls: {}
                        ============================
                        """,
                step,
                toolCalls.stream()
                        .map(toolCall -> toolCall.toolName().name() + " args=" + preview(toolCall.arguments()))
                        .toList()
        );
    }

    private void logToolExecution(
            int step,
            JazzAgentToolCallRequest toolCall,
            com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolExecutionResult execution
    ) {
        log.info(
                """
                        
                        === Jazz Agent Tool Result ===
                        step: {}
                        callId: {}
                        tool: {}
                        arguments: {}
                        output: {}
                        metadata: {}
                        ==============================
                        """,
                step,
                toolCall.callId(),
                toolCall.toolName(),
                preview(toolCall.arguments()),
                preview(execution.content()),
                preview(execution.metadata())
        );
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String preview(Object value) {
        if (value == null) {
            return "null";
        }
        var rendered = String.valueOf(value).trim();
        if (rendered.length() <= LOG_PREVIEW_LIMIT) {
            return rendered;
        }
        return rendered.substring(0, LOG_PREVIEW_LIMIT) + "...[truncated]";
    }
}
