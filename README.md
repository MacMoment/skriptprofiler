# SkriptProfiler

A production-ready Skript performance profiler plugin for Paper/Spigot servers.

## Features

### Core Profiling Capabilities
- **Execution Tracking**: Track execution time and frequency of Skript events, functions, and commands
- **Script Analysis**: Automatically load and analyze all Skript files from the scripts folder
- **File/Line Mapping**: Precise identification of performance issues with file and line number references
- **TPS-Aware Profiling**: Automatically monitors server TPS and adjusts profiling behavior

### Advanced Analysis
- **Slow Event Detection**: Identifies events that take too long to execute
- **Loop Analysis**: Detects inefficient loops with excessive iterations
- **Wait Statement Analysis**: Flags long wait/delay statements that may impact performance
- **Variable Access Tracking**: Monitors excessive variable access patterns
- **High Frequency Detection**: Identifies code that executes very frequently

### Performance & Architecture
- **Minimal Overhead**: Designed with low-impact profiling to avoid affecting server performance
- **Thread-Safe**: Uses concurrent data structures for reliable multi-threaded profiling
- **Modular Design**: Clean separation of concerns with dedicated classes for each responsibility
- **Configurable**: Extensive configuration options for thresholds and behavior

### Reporting
- **In-Game Reports**: View profiling results directly in-game with color-coded severity
- **Console Reports**: Detailed logs sent to server console
- **Detailed Breakdown**: Line-by-line analysis of script performance
- **Actionable Suggestions**: Specific recommendations for fixing each identified issue

## Installation

1. Download the SkriptProfiler.jar file
2. Place it in your server's `plugins` folder
3. Ensure Skript is installed and loaded
4. Restart your server

## Commands

- `/skprofile start` - Start profiling Skript execution
- `/skprofile stop` - Stop profiling
- `/skprofile report` - Generate a performance report
- `/skprofile report detailed` - Generate a detailed report with line-by-line breakdown
- `/skprofile status` - View profiler status and statistics
- `/skprofile reset` - Reset all profiling data
- `/skprofile help` - Display command help

**Aliases**: `/sp`, `/skprof`

## Permissions

- `skriptprofiler.use` - Access to all profiler commands (default: op)
- `skriptprofiler.admin` - Full administrative access (default: op)

## Configuration

The plugin creates a `config.yml` with extensive customization options:

### Profiling Settings
- `profiling.auto-start` - Automatically start profiling on server startup
- `profiling.sample-interval` - Sampling interval in milliseconds
- `profiling.max-duration` - Maximum profiling duration in seconds (0 = unlimited)
- `profiling.tps-aware` - Enable TPS-aware profiling adjustments
- `profiling.tps-threshold` - TPS threshold for warnings

### Analysis Thresholds
- `thresholds.slow-execution` - Execution time threshold (ms) for slow warnings
- `thresholds.very-slow-execution` - Threshold for critical slowness warnings
- `thresholds.loop-iterations` - Loop iteration count threshold
- `thresholds.long-wait` - Wait time threshold in ticks
- `thresholds.excessive-variables` - Variable access count threshold

### Reporting
- `reporting.detailed` - Include detailed line-by-line breakdown
- `reporting.max-issues` - Maximum issues to display per category
- `reporting.include-suggestions` - Show optimization suggestions
- `reporting.format` - Report output format (CONSOLE, IN_GAME, BOTH)

### Advanced Options
- `advanced.track-events` - Enable event execution tracking
- `advanced.track-functions` - Enable function call tracking
- `advanced.track-commands` - Enable command execution tracking
- `advanced.memory-profiling` - Enable memory profiling
- `advanced.cache-analysis` - Cache analyzed scripts

## How It Works

1. **Script Loading**: On startup or when profiling starts, the plugin scans the Skript scripts folder and loads all `.sk` files
2. **Analysis**: Each script is analyzed for events, functions, commands, loops, variable usage, and wait statements
3. **Execution Tracking**: During profiling, the plugin tracks execution time and frequency using high-precision nanosecond timers
4. **Bottleneck Detection**: The analyzer identifies performance issues based on configurable thresholds
5. **Report Generation**: Results are compiled into comprehensive reports with severity levels and actionable suggestions

## Architecture

The plugin follows a clean, modular architecture:

- **SkriptProfilerPlugin**: Main plugin class managing lifecycle
- **ProfilerManager**: Orchestrates profiling sessions and coordinates components
- **ScriptFileLoader**: Loads and analyzes Skript files from disk
- **ExecutionTracker**: Tracks execution time and frequency with thread-safe data structures
- **BottleneckAnalyzer**: Analyzes profile data to identify performance issues
- **ReportGenerator**: Creates human-readable reports with formatting
- **ProfilerCommand**: Command handler with tab completion
- **Model Classes**: Data structures for ProfileData, PerformanceIssue, and ScriptInfo

## Performance Considerations

- **Low Overhead**: Profiling uses atomic operations and efficient data structures
- **Minimal Allocations**: Reuses objects where possible to reduce GC pressure
- **Thread Safety**: All shared data structures are thread-safe
- **TPS Monitoring**: Automatically detects if profiling is impacting server performance

## Building from Source

Requirements:
- Java 17 or higher
- Maven 3.6+
- Spigot/Paper API (provided at runtime)

```bash
mvn clean package
```

The compiled JAR will be in the `target` directory.

## Compatibility

- **Minecraft**: 1.13.2 - 1.20.x
- **Server Software**: Spigot, Paper, Purpur, and derivatives
- **Required**: Skript 2.6+
- **Java**: 17+

## Example Usage

1. Start your server and let players interact normally
2. Run `/skprofile start` to begin profiling
3. Wait for scripts to execute (or trigger specific events)
4. Run `/skprofile stop` to end the profiling session
5. Run `/skprofile report detailed` to see a comprehensive analysis
6. Review the identified issues and apply suggested optimizations
7. Run `/skprofile reset` before starting a new profiling session

## Issue Types

The profiler detects several types of performance issues:

- **Slow Event Execution**: Events taking longer than configured thresholds
- **Inefficient Loops**: Loops with excessive iterations
- **Long Wait/Delay**: Wait statements that tie up execution
- **Excessive Variables**: High variable access counts
- **High Frequency**: Code executing very frequently
- **TPS Impact**: Operations correlated with TPS drops

Each issue includes:
- Severity level (LOW, MEDIUM, HIGH, CRITICAL)
- Exact file and line number
- Description of the problem
- Specific optimization suggestions

## Support

For issues, questions, or feature requests, please visit the project repository.

## License

This plugin is provided as-is for use with Minecraft servers running Skript.

