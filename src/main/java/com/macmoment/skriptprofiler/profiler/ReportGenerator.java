package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.PerformanceIssue;
import com.macmoment.skriptprofiler.model.ProfileData;
import com.macmoment.skriptprofiler.model.ScriptInfo;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates performance reports from profiling data
 */
public class ReportGenerator {
    
    private final SkriptProfilerPlugin plugin;
    
    public ReportGenerator(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Generates a comprehensive performance report
     */
    public String generateReport(Map<String, ProfileData> profileData,
                                 List<PerformanceIssue> issues,
                                 Map<String, ScriptInfo> scripts,
                                 long duration,
                                 double currentTPS,
                                 boolean detailed) {
        StringBuilder report = new StringBuilder();
        
        // Header
        report.append(formatHeader("SKRIPT PROFILER REPORT"));
        report.append("\n");
        
        // Summary section
        appendSummary(report, profileData, scripts, duration, currentTPS);
        report.append("\n");
        
        // Top performers (slowest operations)
        appendTopPerformers(report, profileData, 10);
        report.append("\n");
        
        // Issues section
        if (!issues.isEmpty()) {
            appendIssues(report, issues);
            report.append("\n");
        }
        
        // Detailed breakdown if requested
        if (detailed) {
            appendDetailedBreakdown(report, profileData, scripts);
        }
        
        // Footer with recommendations
        appendRecommendations(report, issues);
        
        return report.toString();
    }
    
    /**
     * Formats a header
     */
    private String formatHeader(String text) {
        String line = "═".repeat(60);
        return ChatColor.GOLD + line + "\n" +
               ChatColor.YELLOW + "  " + text + "\n" +
               ChatColor.GOLD + line + ChatColor.RESET;
    }
    
    /**
     * Appends summary section
     */
    private void appendSummary(StringBuilder report, Map<String, ProfileData> profileData,
                               Map<String, ScriptInfo> scripts, long duration, double currentTPS) {
        report.append(ChatColor.AQUA).append("Summary:\n").append(ChatColor.RESET);
        report.append(String.format("  Duration: %.2f seconds\n", duration / 1000.0));
        report.append(String.format("  Current TPS: %.2f\n", currentTPS));
        report.append(String.format("  Scripts Analyzed: %d\n", scripts.size()));
        report.append(String.format("  Total Events/Functions Tracked: %d\n", profileData.size()));
        
        // Calculate total executions
        long totalExecutions = profileData.values().stream()
            .mapToLong(ProfileData::getExecutionCount)
            .sum();
        report.append(String.format("  Total Executions: %d\n", totalExecutions));
        
        // Calculate total time spent
        double totalTimeMs = profileData.values().stream()
            .mapToDouble(d -> d.getTotalExecutionTimeNanos() / 1_000_000.0)
            .sum();
        report.append(String.format("  Total Execution Time: %.2fms\n", totalTimeMs));
    }
    
    /**
     * Appends top performers (slowest operations)
     */
    private void appendTopPerformers(StringBuilder report, Map<String, ProfileData> profileData, int limit) {
        report.append(ChatColor.AQUA).append("\nTop Slowest Operations:\n").append(ChatColor.RESET);
        
        List<ProfileData> sorted = profileData.values().stream()
            .sorted((d1, d2) -> Double.compare(d2.getAverageExecutionTimeMs(), d1.getAverageExecutionTimeMs()))
            .limit(limit)
            .collect(Collectors.toList());
        
        if (sorted.isEmpty()) {
            report.append("  No execution data available.\n");
            return;
        }
        
        int rank = 1;
        for (ProfileData data : sorted) {
            String color = data.getAverageExecutionTimeMs() > 100 ? ChatColor.RED.toString() :
                          data.getAverageExecutionTimeMs() > 50 ? ChatColor.YELLOW.toString() :
                          ChatColor.GREEN.toString();
            
            report.append(String.format("  %s%d. %s:%d - %s\n",
                color, rank++, getShortFileName(data.getScriptFile()), data.getLineNumber(), data.getElementName()));
            report.append(String.format("     Avg: %.2fms | Max: %.2fms | Count: %d\n",
                data.getAverageExecutionTimeMs(), data.getMaxExecutionTimeMs(), data.getExecutionCount()));
            report.append(ChatColor.RESET);
        }
    }
    
    /**
     * Appends issues section
     */
    private void appendIssues(StringBuilder report, List<PerformanceIssue> issues) {
        report.append(ChatColor.AQUA).append("\nPerformance Issues Detected:\n").append(ChatColor.RESET);
        
        int maxIssues = plugin.getConfig().getInt("reporting.max-issues", 10);
        List<PerformanceIssue> limitedIssues = issues.stream().limit(maxIssues).collect(Collectors.toList());
        
        for (PerformanceIssue issue : limitedIssues) {
            String color = switch (issue.getSeverity()) {
                case CRITICAL -> ChatColor.DARK_RED.toString();
                case HIGH -> ChatColor.RED.toString();
                case MEDIUM -> ChatColor.YELLOW.toString();
                case LOW -> ChatColor.WHITE.toString();
            };
            
            report.append(String.format("\n  %s[%s] %s\n",
                color, issue.getSeverity(), issue.getType().getDisplayName()));
            report.append(String.format("  Location: %s:%d\n",
                getShortFileName(issue.getScriptFile()), issue.getLineNumber()));
            report.append(String.format("  Issue: %s\n", issue.getDescription()));
            
            if (plugin.getConfig().getBoolean("reporting.include-suggestions", true)) {
                report.append(ChatColor.GREEN).append("  Suggestion: ").append(issue.getSuggestion()).append("\n");
            }
            report.append(ChatColor.RESET);
        }
        
        if (issues.size() > maxIssues) {
            report.append(String.format("\n  ... and %d more issue(s)\n", issues.size() - maxIssues));
        }
    }
    
    /**
     * Appends detailed breakdown
     */
    private void appendDetailedBreakdown(StringBuilder report, Map<String, ProfileData> profileData,
                                         Map<String, ScriptInfo> scripts) {
        report.append(ChatColor.AQUA).append("\nDetailed Breakdown by Script:\n").append(ChatColor.RESET);
        
        Map<String, List<ProfileData>> byScript = profileData.values().stream()
            .collect(Collectors.groupingBy(ProfileData::getScriptFile));
        
        for (Map.Entry<String, List<ProfileData>> entry : byScript.entrySet()) {
            String scriptFile = entry.getKey();
            List<ProfileData> scriptData = entry.getValue();
            
            report.append(String.format("\n  %s%s:\n", ChatColor.YELLOW, getShortFileName(scriptFile)));
            
            ScriptInfo info = scripts.get(scriptFile);
            if (info != null) {
                report.append(String.format("    Events: %d | Functions: %d | Commands: %d\n",
                    info.getTotalEventCount(), info.getTotalFunctionCount(), info.getTotalCommandCount()));
            }
            
            scriptData.stream()
                .sorted((d1, d2) -> Double.compare(d2.getAverageExecutionTimeMs(), d1.getAverageExecutionTimeMs()))
                .limit(5)
                .forEach(data -> {
                    report.append(String.format("    Line %d: %.2fms avg (%d executions)\n",
                        data.getLineNumber(), data.getAverageExecutionTimeMs(), data.getExecutionCount()));
                });
            
            report.append(ChatColor.RESET);
        }
    }
    
    /**
     * Appends recommendations
     */
    private void appendRecommendations(StringBuilder report, List<PerformanceIssue> issues) {
        report.append(ChatColor.AQUA).append("\nGeneral Recommendations:\n").append(ChatColor.RESET);
        
        if (issues.stream().anyMatch(i -> i.getType() == PerformanceIssue.IssueType.SLOW_EVENT)) {
            report.append("  • Optimize slow event handlers to improve server responsiveness\n");
        }
        if (issues.stream().anyMatch(i -> i.getType() == PerformanceIssue.IssueType.INEFFICIENT_LOOP)) {
            report.append("  • Review loops for unnecessary iterations or complex operations\n");
        }
        if (issues.stream().anyMatch(i -> i.getType() == PerformanceIssue.IssueType.EXCESSIVE_VARIABLES)) {
            report.append("  • Consider reducing variable operations or using more efficient data structures\n");
        }
        
        report.append("  • Use '/skprofile report detailed' for line-by-line analysis\n");
        report.append("  • Consider async operations for I/O-heavy tasks\n");
        report.append("  • Cache frequently accessed data when possible\n");
    }
    
    /**
     * Gets short filename from full path
     */
    private String getShortFileName(String fullPath) {
        if (fullPath == null) return "unknown";
        int lastSeparator = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? fullPath.substring(lastSeparator + 1) : fullPath;
    }
}
