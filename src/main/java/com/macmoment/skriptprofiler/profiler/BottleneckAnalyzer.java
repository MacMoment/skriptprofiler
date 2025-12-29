package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.PerformanceIssue;
import com.macmoment.skriptprofiler.model.ProfileData;
import com.macmoment.skriptprofiler.model.ScriptInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes profiling data to identify performance bottlenecks
 */
public class BottleneckAnalyzer {
    
    private final SkriptProfilerPlugin plugin;
    private final List<PerformanceIssue> detectedIssues;
    
    // Regex patterns for code analysis
    private static final Pattern LOOP_PATTERN = Pattern.compile("loop\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAIT_PATTERN = Pattern.compile("wait\\s+(\\d+)\\s*(tick|second|minute)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^}]+\\}");
    
    public BottleneckAnalyzer(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
        this.detectedIssues = new ArrayList<>();
    }
    
    /**
     * Analyzes profile data and script information to detect bottlenecks
     */
    public void analyze(Map<String, ProfileData> profileData, Map<String, ScriptInfo> scripts) {
        detectedIssues.clear();
        
        // Get thresholds from config
        double slowThreshold = plugin.getConfig().getDouble("thresholds.slow-execution", 50.0);
        double verySlowThreshold = plugin.getConfig().getDouble("thresholds.very-slow-execution", 200.0);
        int loopThreshold = plugin.getConfig().getInt("thresholds.loop-iterations", 1000);
        int waitThreshold = plugin.getConfig().getInt("thresholds.long-wait", 100);
        int variableThreshold = plugin.getConfig().getInt("thresholds.excessive-variables", 500);
        
        // Analyze execution times
        analyzeExecutionTimes(profileData, slowThreshold, verySlowThreshold);
        
        // Analyze script contents
        analyzeScriptContents(scripts, profileData, loopThreshold, waitThreshold, variableThreshold);
        
        // Sort issues by severity
        detectedIssues.sort((i1, i2) -> i2.getSeverity().compareTo(i1.getSeverity()));
        
        plugin.getLogger().info("Analysis complete. Found " + detectedIssues.size() + " potential issue(s)");
    }
    
    /**
     * Analyzes execution times to find slow operations
     */
    private void analyzeExecutionTimes(Map<String, ProfileData> profileData, double slowThreshold, double verySlowThreshold) {
        for (ProfileData data : profileData.values()) {
            double avgTime = data.getAverageExecutionTimeMs();
            double maxTime = data.getMaxExecutionTimeMs();
            
            // Check for very slow execution
            if (maxTime >= verySlowThreshold) {
                detectedIssues.add(new PerformanceIssue(
                    PerformanceIssue.IssueType.SLOW_EVENT,
                    PerformanceIssue.Severity.CRITICAL,
                    data.getScriptFile(),
                    data.getLineNumber(),
                    String.format("Very slow execution detected: %.2fms average, %.2fms max", avgTime, maxTime),
                    "Consider optimizing this code block. Break down complex operations, reduce database queries, or use async operations.",
                    data
                ));
            } else if (avgTime >= slowThreshold) {
                detectedIssues.add(new PerformanceIssue(
                    PerformanceIssue.IssueType.SLOW_EVENT,
                    PerformanceIssue.Severity.HIGH,
                    data.getScriptFile(),
                    data.getLineNumber(),
                    String.format("Slow execution detected: %.2fms average", avgTime),
                    "Review this code for potential optimizations. Consider caching results or reducing complexity.",
                    data
                ));
            }
            
            // Check for high execution frequency
            if (data.getExecutionCount() > 1000) {
                double totalTime = data.getTotalExecutionTimeNanos() / 1_000_000.0;
                detectedIssues.add(new PerformanceIssue(
                    PerformanceIssue.IssueType.HIGH_FREQUENCY,
                    totalTime > 1000 ? PerformanceIssue.Severity.HIGH : PerformanceIssue.Severity.MEDIUM,
                    data.getScriptFile(),
                    data.getLineNumber(),
                    String.format("High execution frequency: %d times (%.2fms total)", data.getExecutionCount(), totalTime),
                    "This code executes very frequently. Even small optimizations can have significant impact.",
                    data
                ));
            }
        }
    }
    
    /**
     * Analyzes script contents for potential issues
     */
    private void analyzeScriptContents(Map<String, ScriptInfo> scripts, Map<String, ProfileData> profileData,
                                       int loopThreshold, int waitThreshold, int variableThreshold) {
        for (ScriptInfo script : scripts.values()) {
            List<String> lines = script.getLines();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNumber = i + 1;
                
                // Check for loops
                if (LOOP_PATTERN.matcher(line).find()) {
                    // Find related profile data
                    ProfileData relatedData = findProfileDataForLine(profileData, script.getFilePath(), lineNumber);
                    
                    if (relatedData != null && relatedData.getExecutionCount() > loopThreshold) {
                        detectedIssues.add(new PerformanceIssue(
                            PerformanceIssue.IssueType.INEFFICIENT_LOOP,
                            PerformanceIssue.Severity.MEDIUM,
                            script.getFilePath(),
                            lineNumber,
                            "Loop with high iteration count detected",
                            "Consider using list operations, filtering, or limiting the loop size. Review if all iterations are necessary.",
                            relatedData
                        ));
                    }
                }
                
                // Check for wait statements
                Matcher waitMatcher = WAIT_PATTERN.matcher(line);
                if (waitMatcher.find()) {
                    int waitTime = Integer.parseInt(waitMatcher.group(1));
                    String unit = waitMatcher.group(2).toLowerCase();
                    
                    // Convert to ticks
                    int ticks = switch (unit) {
                        case "second", "seconds" -> waitTime * 20;
                        case "minute", "minutes" -> waitTime * 1200;
                        default -> waitTime;
                    };
                    
                    if (ticks > waitThreshold) {
                        detectedIssues.add(new PerformanceIssue(
                            PerformanceIssue.IssueType.LONG_WAIT,
                            PerformanceIssue.Severity.LOW,
                            script.getFilePath(),
                            lineNumber,
                            String.format("Long wait statement: %d %s", waitTime, unit),
                            "Consider if this wait is necessary. Long waits can tie up script execution threads.",
                            null
                        ));
                    }
                }
            }
            
            // Check for excessive variable access
            if (script.getTotalVariableAccess() > variableThreshold) {
                detectedIssues.add(new PerformanceIssue(
                    PerformanceIssue.IssueType.EXCESSIVE_VARIABLES,
                    PerformanceIssue.Severity.MEDIUM,
                    script.getFilePath(),
                    1,
                    String.format("Excessive variable access: %d occurrences", script.getTotalVariableAccess()),
                    "High variable usage can impact performance. Consider reducing variable operations or using local variables.",
                    null
                ));
            }
        }
    }
    
    /**
     * Finds profile data for a specific line
     */
    private ProfileData findProfileDataForLine(Map<String, ProfileData> profileData, String filePath, int lineNumber) {
        for (ProfileData data : profileData.values()) {
            if (data.getScriptFile().equals(filePath) && data.getLineNumber() == lineNumber) {
                return data;
            }
        }
        return null;
    }
    
    /**
     * Gets all detected issues
     */
    public List<PerformanceIssue> getIssues() {
        return new ArrayList<>(detectedIssues);
    }
    
    /**
     * Resets detected issues
     */
    public void reset() {
        detectedIssues.clear();
    }
}
