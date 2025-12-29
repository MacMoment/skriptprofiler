package com.macmoment.skriptprofiler.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Skript file that has been analyzed
 */
public class ScriptInfo {
    private final String filePath;
    private final String fileName;
    private final List<String> lines;
    private final Map<Integer, String> lineElements; // line number -> element description
    private int totalEventCount;
    private int totalFunctionCount;
    private int totalCommandCount;
    private int totalLoopCount;
    private int totalVariableAccess;
    
    public ScriptInfo(String filePath, String fileName, List<String> lines) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.lines = new ArrayList<>(lines);
        this.lineElements = new HashMap<>();
    }
    
    public void addLineElement(int lineNumber, String element) {
        lineElements.put(lineNumber, element);
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
    
    public String getLineContent(int lineNumber) {
        if (lineNumber > 0 && lineNumber <= lines.size()) {
            return lines.get(lineNumber - 1);
        }
        return "";
    }
    
    public String getElementAtLine(int lineNumber) {
        return lineElements.get(lineNumber);
    }
    
    public int getTotalEventCount() {
        return totalEventCount;
    }
    
    public void setTotalEventCount(int totalEventCount) {
        this.totalEventCount = totalEventCount;
    }
    
    public int getTotalFunctionCount() {
        return totalFunctionCount;
    }
    
    public void setTotalFunctionCount(int totalFunctionCount) {
        this.totalFunctionCount = totalFunctionCount;
    }
    
    public int getTotalCommandCount() {
        return totalCommandCount;
    }
    
    public void setTotalCommandCount(int totalCommandCount) {
        this.totalCommandCount = totalCommandCount;
    }
    
    public int getTotalLoopCount() {
        return totalLoopCount;
    }
    
    public void setTotalLoopCount(int totalLoopCount) {
        this.totalLoopCount = totalLoopCount;
    }
    
    public int getTotalVariableAccess() {
        return totalVariableAccess;
    }
    
    public void setTotalVariableAccess(int totalVariableAccess) {
        this.totalVariableAccess = totalVariableAccess;
    }
}
