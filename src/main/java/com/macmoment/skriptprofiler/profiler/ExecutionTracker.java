package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.ProfileData;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks execution time and frequency of Skript elements
 */
public class ExecutionTracker implements Listener {
    
    private static final int MAX_EVENT_TYPES = 100;
    
    private final SkriptProfilerPlugin plugin;
    private final Map<String, ProfileData> profileDataMap;
    private final Set<String> trackedEventTypes;
    private final ThreadLocal<Long> executionStartTime;
    private volatile boolean isTracking;
    private volatile boolean isRegistered;
    private long trackingStartTime;
    private long trackingEndTime;
    
    public ExecutionTracker(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
        this.profileDataMap = new ConcurrentHashMap<>();
        this.trackedEventTypes = ConcurrentHashMap.newKeySet();
        this.executionStartTime = new ThreadLocal<>();
        this.isTracking = false;
        this.isRegistered = false;
    }
    
    /**
     * Starts tracking execution
     */
    public void startTracking() {
        if (!isTracking) {
            isTracking = true;
            trackingStartTime = System.currentTimeMillis();
            if (!isRegistered) {
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                isRegistered = true;
            }
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
            // Unregister handlers to prevent memory leaks
            if (isRegistered) {
                HandlerList.unregisterAll(this);
                isRegistered = false;
            }
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
     * Generic event handler to track event executions
     * This provides basic tracking for all events.
     * 
     * IMPORTANT LIMITATION: This handler cannot measure actual Skript event processing time.
     * As a MONITOR priority handler, it runs after all other handlers have completed.
     * To measure actual Skript execution time, integration with Skript's internal trigger
     * system would be required. This implementation tracks:
     * - Event occurrence count (useful for identifying high-frequency events)
     * 
     * The script file analysis (via ScriptFileLoader) provides the main profiling data.
     * 
     * Performance considerations:
     * - Event tracking is only enabled if advanced.track-events is true in config
     * - Limited to MAX_EVENT_TYPES unique event types to prevent unbounded memory growth
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEvent(Event event) {
        if (!isTracking) return;
        
        // Check if event tracking is enabled in config
        if (!plugin.getConfig().getBoolean("advanced.track-events", true)) {
            return;
        }
        
        String eventType = event.getClass().getSimpleName();
        
        // Limit the number of tracked event types to prevent unbounded memory growth
        if (!trackedEventTypes.contains(eventType)) {
            if (trackedEventTypes.size() >= MAX_EVENT_TYPES) {
                return; // Skip tracking new event types once limit is reached
            }
            trackedEventTypes.add(eventType);
        }
        
        // Record the event occurrence - this primarily tracks event frequency
        // Use "system:events" as scriptFile to distinguish from actual script files
        ProfileData data = createOrGetProfileData("system:events", 0, "event", eventType);
        
        // Increment execution count without timing data
        data.incrementExecutionCount();
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
        trackedEventTypes.clear();
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
