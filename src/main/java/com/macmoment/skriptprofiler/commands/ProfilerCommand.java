package com.macmoment.skriptprofiler.commands;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.profiler.ProfilerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for the profiler
 */
public class ProfilerCommand implements CommandExecutor, TabCompleter {
    
    private final SkriptProfilerPlugin plugin;
    private final ProfilerManager profilerManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "start", "stop", "report", "reset", "help", "status"
    );
    
    public ProfilerCommand(SkriptProfilerPlugin plugin, ProfilerManager profilerManager) {
        this.plugin = plugin;
        this.profilerManager = profilerManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skriptprofiler.use")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "report" -> handleReport(sender, args);
            case "reset" -> handleReset(sender);
            case "status" -> handleStatus(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use '/skprofile help' for help.");
                return true;
            }
        }
        
        return true;
    }
    
    /**
     * Handles the start subcommand
     */
    private void handleStart(CommandSender sender) {
        if (profilerManager.isProfiling()) {
            sender.sendMessage(ChatColor.YELLOW + "Profiler is already running!");
            return;
        }
        
        boolean success = profilerManager.startProfiling();
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Profiling started! Use '/skprofile report' to view results.");
            sender.sendMessage(ChatColor.GRAY + "The profiler is now tracking Skript execution...");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to start profiling.");
        }
    }
    
    /**
     * Handles the stop subcommand
     */
    private void handleStop(CommandSender sender) {
        if (!profilerManager.isProfiling()) {
            sender.sendMessage(ChatColor.YELLOW + "Profiler is not running!");
            return;
        }
        
        boolean success = profilerManager.stopProfiling();
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Profiling stopped! Use '/skprofile report' to view results.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to stop profiling.");
        }
    }
    
    /**
     * Handles the report subcommand
     */
    private void handleReport(CommandSender sender, String[] args) {
        boolean detailed = args.length > 1 && args[1].equalsIgnoreCase("detailed");
        
        sender.sendMessage(ChatColor.GRAY + "Generating performance report...");
        
        String report = profilerManager.generateReport(detailed);
        
        // Send report based on configuration
        String format = plugin.getConfig().getString("reporting.format", "BOTH");
        
        if (sender instanceof Player && (format.equalsIgnoreCase("IN_GAME") || format.equalsIgnoreCase("BOTH"))) {
            // Send to player
            for (String line : report.split("\n")) {
                sender.sendMessage(line);
            }
        }
        
        if (format.equalsIgnoreCase("CONSOLE") || format.equalsIgnoreCase("BOTH")) {
            // Log to console
            plugin.getLogger().info("=== Performance Report ===");
            for (String line : report.split("\n")) {
                // Strip color codes for console
                plugin.getLogger().info(ChatColor.stripColor(line));
            }
        }
        
        if (sender instanceof Player && format.equalsIgnoreCase("CONSOLE")) {
            sender.sendMessage(ChatColor.GREEN + "Report generated and logged to console!");
        }
    }
    
    /**
     * Handles the reset subcommand
     */
    private void handleReset(CommandSender sender) {
        if (profilerManager.isProfiling()) {
            sender.sendMessage(ChatColor.YELLOW + "Stop profiling before resetting!");
            return;
        }
        
        profilerManager.reset();
        sender.sendMessage(ChatColor.GREEN + "Profiling data reset!");
    }
    
    /**
     * Handles the status subcommand
     */
    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Profiler Status ===");
        sender.sendMessage(String.format("%sStatus: %s%s",
            ChatColor.AQUA,
            profilerManager.isProfiling() ? ChatColor.GREEN + "RUNNING" : ChatColor.RED + "STOPPED",
            ChatColor.RESET
        ));
        sender.sendMessage(String.format("%sCurrent TPS: %s%.2f",
            ChatColor.AQUA,
            ChatColor.WHITE,
            profilerManager.getCurrentTPS()
        ));
        sender.sendMessage(String.format("%sScripts Loaded: %s%d",
            ChatColor.AQUA,
            ChatColor.WHITE,
            profilerManager.getScriptLoader().getLoadedScripts().size()
        ));
        
        if (profilerManager.isProfiling()) {
            sender.sendMessage(ChatColor.GRAY + "Use '/skprofile stop' to stop profiling");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Use '/skprofile start' to begin profiling");
        }
    }
    
    /**
     * Sends help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SkriptProfiler Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile start" + ChatColor.GRAY + " - Start profiling");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile stop" + ChatColor.GRAY + " - Stop profiling");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile report [detailed]" + ChatColor.GRAY + " - Generate report");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile reset" + ChatColor.GRAY + " - Reset profiling data");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile status" + ChatColor.GRAY + " - Show profiler status");
        sender.sendMessage(ChatColor.YELLOW + "/skprofile help" + ChatColor.GRAY + " - Show this help");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("skriptprofiler.use")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("report")) {
            return Arrays.asList("detailed").stream()
                .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
