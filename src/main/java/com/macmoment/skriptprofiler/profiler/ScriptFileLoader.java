package com.macmoment.skriptprofiler.profiler;

import com.macmoment.skriptprofiler.SkriptProfilerPlugin;
import com.macmoment.skriptprofiler.model.ScriptInfo;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and analyzes Skript files from the scripts folder
 */
public class ScriptFileLoader {
    
    private final SkriptProfilerPlugin plugin;
    private final Map<String, ScriptInfo> loadedScripts;
    
    // Patterns for detecting Skript elements
    private static final Pattern EVENT_PATTERN = Pattern.compile("^\\s*on\\s+(.+):", Pattern.CASE_INSENSITIVE);
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*function\\s+([\\w_]+)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^\\s*command\\s+/?(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOP_PATTERN = Pattern.compile("^\\s*loop\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^}]+\\}");
    private static final Pattern WAIT_PATTERN = Pattern.compile("^\\s*wait\\s+(\\d+)\\s*(tick|second|minute)", Pattern.CASE_INSENSITIVE);
    
    public ScriptFileLoader(SkriptProfilerPlugin plugin) {
        this.plugin = plugin;
        this.loadedScripts = new ConcurrentHashMap<>();
    }
    
    /**
     * Loads all Skript files from the scripts folder
     */
    public void loadAllScripts() {
        loadedScripts.clear();
        
        File scriptsFolder = getSkriptFolder();
        if (scriptsFolder == null || !scriptsFolder.exists()) {
            plugin.getLogger().warning("Skript scripts folder not found!");
            return;
        }
        
        plugin.getLogger().info("Loading Skript files from: " + scriptsFolder.getAbsolutePath());
        
        try {
            loadScriptsRecursively(scriptsFolder.toPath());
            plugin.getLogger().info("Loaded " + loadedScripts.size() + " script file(s)");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading scripts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the Skript scripts folder
     */
    private File getSkriptFolder() {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File skriptFolder = new File(pluginsFolder, "Skript");
        return new File(skriptFolder, "scripts");
    }
    
    /**
     * Recursively loads scripts from a directory
     */
    private void loadScriptsRecursively(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        
        Files.walk(directory)
            .filter(path -> path.toString().endsWith(".sk"))
            .forEach(this::loadScript);
    }
    
    /**
     * Loads and analyzes a single script file
     */
    private void loadScript(Path scriptPath) {
        try {
            List<String> lines = Files.readAllLines(scriptPath);
            String fileName = scriptPath.getFileName().toString();
            String filePath = scriptPath.toString();
            
            ScriptInfo scriptInfo = new ScriptInfo(filePath, fileName, lines);
            analyzeScript(scriptInfo, lines);
            
            loadedScripts.put(filePath, scriptInfo);
            
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load script: " + scriptPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Analyzes a script for various elements
     */
    private void analyzeScript(ScriptInfo scriptInfo, List<String> lines) {
        int eventCount = 0;
        int functionCount = 0;
        int commandCount = 0;
        int loopCount = 0;
        int variableAccessCount = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            
            // Detect events
            Matcher eventMatcher = EVENT_PATTERN.matcher(line);
            if (eventMatcher.find()) {
                String eventName = eventMatcher.group(1).trim();
                scriptInfo.addLineElement(lineNumber, "Event: " + eventName);
                eventCount++;
            }
            
            // Detect functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                String functionName = functionMatcher.group(1);
                scriptInfo.addLineElement(lineNumber, "Function: " + functionName);
                functionCount++;
            }
            
            // Detect commands
            Matcher commandMatcher = COMMAND_PATTERN.matcher(line);
            if (commandMatcher.find()) {
                String commandName = commandMatcher.group(1);
                scriptInfo.addLineElement(lineNumber, "Command: " + commandName);
                commandCount++;
            }
            
            // Detect loops
            if (LOOP_PATTERN.matcher(line).find()) {
                scriptInfo.addLineElement(lineNumber, "Loop");
                loopCount++;
            }
            
            // Count variable accesses
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            while (variableMatcher.find()) {
                variableAccessCount++;
            }
            
            // Detect wait statements
            Matcher waitMatcher = WAIT_PATTERN.matcher(line);
            if (waitMatcher.find()) {
                scriptInfo.addLineElement(lineNumber, "Wait: " + waitMatcher.group(1) + " " + waitMatcher.group(2));
            }
        }
        
        scriptInfo.setTotalEventCount(eventCount);
        scriptInfo.setTotalFunctionCount(functionCount);
        scriptInfo.setTotalCommandCount(commandCount);
        scriptInfo.setTotalLoopCount(loopCount);
        scriptInfo.setTotalVariableAccess(variableAccessCount);
    }
    
    /**
     * Gets loaded script information
     */
    public Map<String, ScriptInfo> getLoadedScripts() {
        return new HashMap<>(loadedScripts);
    }
    
    /**
     * Gets a specific script by file path
     */
    public ScriptInfo getScript(String filePath) {
        return loadedScripts.get(filePath);
    }
    
    /**
     * Clears the script cache
     */
    public void clearCache() {
        loadedScripts.clear();
    }
}
