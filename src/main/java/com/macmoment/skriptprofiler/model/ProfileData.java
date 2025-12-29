package com.macmoment.skriptprofiler.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents profiling data for a single script element (event, function, command, etc.)
 */
public class ProfileData {
    private final String scriptFile;
    private final int lineNumber;
    private final String elementType;
    private final String elementName;
    private final AtomicLong executionCount;
    private final AtomicLong totalExecutionTime;
    private volatile long maxExecutionTime;
    private volatile long minExecutionTime;
    
    public ProfileData(String scriptFile, int lineNumber, String elementType, String elementName) {
        this.scriptFile = scriptFile;
        this.lineNumber = lineNumber;
        this.elementType = elementType;
        this.elementName = elementName;
        this.executionCount = new AtomicLong(0);
        this.totalExecutionTime = new AtomicLong(0);
        this.maxExecutionTime = 0;
        this.minExecutionTime = Long.MAX_VALUE;
    }
    
    public void recordExecution(long executionTimeNanos) {
        executionCount.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeNanos);
        
        // Update min/max
        synchronized (this) {
            if (executionTimeNanos > maxExecutionTime) {
                maxExecutionTime = executionTimeNanos;
            }
            if (executionTimeNanos < minExecutionTime) {
                minExecutionTime = executionTimeNanos;
            }
        }
    }
    
    public long getExecutionCount() {
        return executionCount.get();
    }
    
    public long getTotalExecutionTimeNanos() {
        return totalExecutionTime.get();
    }
    
    public double getAverageExecutionTimeMs() {
        long count = executionCount.get();
        if (count == 0) return 0;
        return (totalExecutionTime.get() / (double) count) / 1_000_000.0;
    }
    
    public double getMaxExecutionTimeMs() {
        return maxExecutionTime / 1_000_000.0;
    }
    
    public double getMinExecutionTimeMs() {
        return (minExecutionTime == Long.MAX_VALUE) ? 0 : minExecutionTime / 1_000_000.0;
    }
    
    public String getScriptFile() {
        return scriptFile;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getElementType() {
        return elementType;
    }
    
    public String getElementName() {
        return elementName;
    }
    
    public String getLocationString() {
        return scriptFile + ":" + lineNumber;
    }
}
