package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolExecutionResult;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class JazzAgentRun {

    @Getter
    private final JazzAgentContext context;
    @Getter
    private final int maxToolSteps;
    private final List<ModelUsage> usageEntries = new ArrayList<>();
    private final List<JazzToolExecutionResult> toolExecutions = new ArrayList<>();
    @Getter
    private int toolStepCount;

    public JazzAgentRun(JazzAgentContext context, int maxToolSteps) {
        if (context == null) {
            throw new IllegalArgumentException("Jazz agent run requires context");
        }
        if (maxToolSteps < 1) {
            throw new IllegalArgumentException("Jazz agent run requires maxToolSteps >= 1");
        }
        this.context = context;
        this.maxToolSteps = maxToolSteps;
    }

    public boolean canContinueToolLoop() {
        return toolStepCount < maxToolSteps;
    }

    public void incrementToolStepCount() {
        toolStepCount++;
    }

    public void addUsage(ModelUsage usage) {
        if (usage != null) {
            usageEntries.add(usage);
        }
    }

    public void addToolExecution(JazzToolExecutionResult executionResult) {
        if (executionResult != null) {
            toolExecutions.add(executionResult);
        }
    }

    public List<ModelUsage> usageEntries() {
        return List.copyOf(usageEntries);
    }

    public List<JazzToolExecutionResult> toolExecutions() {
        return List.copyOf(toolExecutions);
    }
}
