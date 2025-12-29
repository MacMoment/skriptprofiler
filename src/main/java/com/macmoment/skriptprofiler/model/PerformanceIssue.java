package com.macmoment.skriptprofiler.model;

/**
 * Represents a performance issue detected by the profiler
 */
public class PerformanceIssue {
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum IssueType {
        SLOW_EVENT("Slow Event Execution"),
        INEFFICIENT_LOOP("Inefficient Loop"),
        LONG_WAIT("Excessive Wait/Delay"),
        EXCESSIVE_VARIABLES("Excessive Variable Access"),
        HIGH_FREQUENCY("High Execution Frequency"),
        TPS_IMPACT("TPS Impact Detected");
        
        private final String displayName;
        
        IssueType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final IssueType type;
    private final Severity severity;
    private final String scriptFile;
    private final int lineNumber;
    private final String description;
    private final String suggestion;
    private final ProfileData relatedData;
    
    public PerformanceIssue(IssueType type, Severity severity, String scriptFile, int lineNumber,
                           String description, String suggestion, ProfileData relatedData) {
        this.type = type;
        this.severity = severity;
        this.scriptFile = scriptFile;
        this.lineNumber = lineNumber;
        this.description = description;
        this.suggestion = suggestion;
        this.relatedData = relatedData;
    }
    
    public IssueType getType() {
        return type;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public String getScriptFile() {
        return scriptFile;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public ProfileData getRelatedData() {
        return relatedData;
    }
    
    public String getLocationString() {
        return scriptFile + ":" + lineNumber;
    }
}
