package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.ProfileData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks execution time and frequency of Skript elements
 */
public class ExecutionTracker {
    
    private final SkriptProfilerPlugin plugin;
    private final Map<String, ProfileData> profileDataMap;
    private final ThreadLocal<Long> executionStartTime;
    private volatile boolean isTracking;
    private long trackingStartTime;
    private long trackingEndTime;
    
    public ExecutionTracker(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
        this.profileDataMap = new ConcurrentHashMap<>();
        this.executionStartTime = new ThreadLocal<>();
        this.isTracking = false;
    }
    
    /**
     * Starts tracking execution
     */
    public void startTracking() {
        if (!isTracking) {
            isTracking = true;
            trackingStartTime = System.currentTimeMillis();
            plugin.getLogger().info("Execution tracking started");
        }
    }
    
    /**
     * Stops tracking execution
     */
    public void stopTracking() {
        if (isTracking) {
            isTracking = false;
            trackingEndTime = System.currentTimeMillis();
            plugin.getLogger().info("Execution tracking stopped");
        }
    }
    
    /**
     * Records the start of an execution
     */
    public void recordExecutionStart(String identifier) {
        if (!isTracking) return;
        executionStartTime.set(System.nanoTime());
    }
    
    /**
     * Records the end of an execution and calculates duration
     */
    public void recordExecutionEnd(String scriptFile, int lineNumber, String elementType, String elementName) {
        if (!isTracking) return;
        
        Long startTime = executionStartTime.get();
        if (startTime == null) return;
        
        long endTime = System.nanoTime();
        long executionTime = endTime - startTime;
        
        String key = scriptFile + ":" + lineNumber + ":" + elementType;
        ProfileData data = profileDataMap.computeIfAbsent(key, 
            k -> new ProfileData(scriptFile, lineNumber, elementType, elementName));
        
        data.recordExecution(executionTime);
        executionStartTime.remove();
    }
    
    /**
     * Creates a profile entry for simulated tracking
     * This is used since we can't directly hook into Skript's execution
     */
    public ProfileData createOrGetProfileData(String scriptFile, int lineNumber, String elementType, String elementName) {
        String key = scriptFile + ":" + lineNumber + ":" + elementType;
        return profileDataMap.computeIfAbsent(key, 
            k -> new ProfileData(scriptFile, lineNumber, elementType, elementName));
    }
    
    /**
     * Simulates recording execution for demonstration purposes
     * In a real implementation, this would hook into Skript's execution
     */
    public void simulateExecution(String scriptFile, int lineNumber, String elementType, String elementName, long executionTimeMs) {
        if (!isTracking) return;
        
        ProfileData data = createOrGetProfileData(scriptFile, lineNumber, elementType, elementName);
        data.recordExecution(executionTimeMs * 1_000_000); // Convert ms to ns
    }
    
    /**
     * Gets all profile data
     */
    public Map<String, ProfileData> getProfileData() {
        return new HashMap<>(profileDataMap);
    }
    
    /**
     * Resets all profile data
     */
    public void reset() {
        profileDataMap.clear();
        trackingStartTime = 0;
        trackingEndTime = 0;
    }
    
    /**
     * Gets total profiling time in milliseconds
     */
    public long getTotalProfilingTime() {
        if (trackingStartTime == 0) return 0;
        long endTime = trackingEndTime > 0 ? trackingEndTime : System.currentTimeMillis();
        return endTime - trackingStartTime;
    }
    
    /**
     * Checks if tracking is active
     */
    public boolean isTracking() {
        return isTracking;
    }
}
