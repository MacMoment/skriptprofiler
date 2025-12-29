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
        
        // Update min/max only for non-zero times
        if (executionTimeNanos > 0) {
            synchronized (this) {
                if (executionTimeNanos > maxExecutionTime) {
                    maxExecutionTime = executionTimeNanos;
                }
                if (executionTimeNanos < minExecutionTime) {
                    minExecutionTime = executionTimeNanos;
                }
            }
        }
    }
    
    /**
     * Increments the execution count without recording timing data.
     * Useful for tracking event occurrences where timing cannot be measured.
     * 
     * Note: Using this method will affect average execution time calculations.
     * The average time will be lower since execution count increases without
     * corresponding time. This is intentional for event occurrence counting
     * where timing data is not meaningful.
     */
    public void incrementExecutionCount() {
        executionCount.incrementAndGet();
    }
    
    public long getExecutionCount() {
        return executionCount.get();
    }
    
    public long getTotalExecutionTimeNanos() {
        return totalExecutionTime.get();
    }
    
    /**
     * Returns the average execution time in milliseconds.
     * Note: If incrementExecutionCount() was used, this may return artificially
     * low values since those executions don't contribute to total time.
     */
    public double getAverageExecutionTimeMs() {
        long count = executionCount.get();
        long totalTime = totalExecutionTime.get();
        if (count == 0 || totalTime == 0) return 0;
        return (totalTime / (double) count) / 1_000_000.0;
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
