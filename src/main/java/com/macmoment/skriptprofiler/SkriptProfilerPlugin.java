package com.macmoment.skriptprofiler;

import com.macmoment.skriptprofiler.commands.ProfilerCommand;
import com.macmoment.skriptprofiler.profiler.ProfilerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for SkriptProfiler.
 * A production-ready Skript performance profiler.
 */
public class SkriptProfilerPlugin extends JavaPlugin {
    
    private static SkriptProfilerPlugin instance;
    private ProfilerManager profilerManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Check if Skript is loaded
        if (getServer().getPluginManager().getPlugin("Skript") == null) {
            getLogger().severe("Skript not found! This plugin requires Skript to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize profiler manager
        profilerManager = new ProfilerManager(this);
        
        // Register commands
        ProfilerCommand command = new ProfilerCommand(this, profilerManager);
        getCommand("skprofile").setExecutor(command);
        getCommand("skprofile").setTabCompleter(command);
        
        // Auto-start if configured
        if (getConfig().getBoolean("profiling.auto-start", false)) {
            getLogger().info("Auto-starting profiler...");
            profilerManager.startProfiling();
        }
        
        getLogger().info("SkriptProfiler v" + getDescription().getVersion() + " enabled!");
    }
    
    @Override
    public void onDisable() {
        if (profilerManager != null && profilerManager.isProfiling()) {
            profilerManager.stopProfiling();
        }
        
        getLogger().info("SkriptProfiler disabled!");
    }
    
    public static SkriptProfilerPlugin getInstance() {
        return instance;
    }
    
    public ProfilerManager getProfilerManager() {
        return profilerManager;
    }
}
