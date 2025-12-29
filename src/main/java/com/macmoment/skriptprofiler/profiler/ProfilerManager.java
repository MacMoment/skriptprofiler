package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.ProfileData;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the profiling lifecycle and coordinates all profiler components
 */
public class ProfilerManager {
    
    private final SkriptProfilerPlugin plugin;
    private final ScriptFileLoader scriptLoader;
    private final ExecutionTracker executionTracker;
    private final BottleneckAnalyzer bottleneckAnalyzer;
    private final ReportGenerator reportGenerator;
    
    private volatile boolean isProfiling;
    private long profilingStartTime;
    private BukkitTask tpsMonitorTask;
    private double currentTPS;
    
    public ProfilerManager(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
        this.scriptLoader = new ScriptFileLoader(plugin);
        this.executionTracker = new ExecutionTracker(plugin);
        this.bottleneckAnalyzer = new BottleneckAnalyzer(plugin);
        this.reportGenerator = new ReportGenerator(plugin);
        this.isProfiling = false;
        this.currentTPS = 20.0;
    }
    
    /**
     * Starts the profiling session
     */
    public synchronized boolean startProfiling() {
        if (isProfiling) {
            return false;
        }
        
        plugin.getLogger().info("Starting profiling session...");
        
        // Load all script files
        scriptLoader.loadAllScripts();
        
        // Reset tracker
        executionTracker.reset();
        
        // Start tracking
        executionTracker.startTracking();
        
        // Start TPS monitoring if enabled
        if (plugin.getConfig().getBoolean("profiling.tps-aware", true)) {
            startTPSMonitoring();
        }
        
        isProfiling = true;
        profilingStartTime = System.currentTimeMillis();
        
        // Schedule auto-stop if duration is set
        int maxDuration = plugin.getConfig().getInt("profiling.max-duration", 0);
        if (maxDuration > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, this::stopProfiling, maxDuration * 20L);
        }
        
        plugin.getLogger().info("Profiling session started!");
        return true;
    }
    
    /**
     * Stops the profiling session
     */
    public synchronized boolean stopProfiling() {
        if (!isProfiling) {
            return false;
        }
        
        plugin.getLogger().info("Stopping profiling session...");
        
        // Stop tracking
        executionTracker.stopTracking();
        
        // Stop TPS monitoring
        if (tpsMonitorTask != null) {
            tpsMonitorTask.cancel();
            tpsMonitorTask = null;
        }
        
        isProfiling = false;
        
        plugin.getLogger().info("Profiling session stopped!");
        return true;
    }
    
    /**
     * Generates a performance report
     */
    public String generateReport(boolean detailed) {
        Map<String, ProfileData> profileData = executionTracker.getProfileData();
        
        if (profileData.isEmpty()) {
            return "No profiling data available. Start profiling first!";
        }
        
        // Analyze for bottlenecks
        bottleneckAnalyzer.analyze(profileData, scriptLoader.getLoadedScripts());
        
        // Generate report
        long duration = isProfiling ? 
            (System.currentTimeMillis() - profilingStartTime) : 
            executionTracker.getTotalProfilingTime();
            
        return reportGenerator.generateReport(
            profileData,
            bottleneckAnalyzer.getIssues(),
            scriptLoader.getLoadedScripts(),
            duration,
            currentTPS,
            detailed
        );
    }
    
    /**
     * Resets all profiling data
     */
    public void reset() {
        executionTracker.reset();
        bottleneckAnalyzer.reset();
        scriptLoader.clearCache();
    }
    
    /**
     * Checks if profiling is currently active
     */
    public boolean isProfiling() {
        return isProfiling;
    }
    
    /**
     * Gets the current TPS
     */
    public double getCurrentTPS() {
        return currentTPS;
    }
    
    /**
     * Starts monitoring TPS
     */
    private void startTPSMonitoring() {
        tpsMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Calculate TPS using Bukkit's TPS method
            try {
                double[] recentTps = Bukkit.getTPS();
                currentTPS = recentTps[0]; // 1-minute average
            } catch (Exception e) {
                // Fallback if TPS method is not available
                currentTPS = 20.0;
            }
            
            // Adjust sampling if TPS is low
            if (plugin.getConfig().getBoolean("profiling.tps-aware", true)) {
                double threshold = plugin.getConfig().getDouble("profiling.tps-threshold", 18.0);
                if (currentTPS < threshold) {
                    // Low TPS detected - profiler may be contributing
                    plugin.getLogger().warning(
                        String.format("Low TPS detected: %.2f (threshold: %.2f)", currentTPS, threshold)
                    );
                }
            }
        }, 0L, 20L); // Update every second
    }
    
    public ExecutionTracker getExecutionTracker() {
        return executionTracker;
    }
    
    public ScriptFileLoader getScriptLoader() {
        return scriptLoader;
    }
}
