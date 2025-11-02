package com.zoomtranscriber.platform.linux;

import com.zoomtranscriber.core.detection.ProcessMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Linux-specific implementation of process monitoring using /proc filesystem.
 * Provides process detection and window title monitoring for Linux systems.
 */
@Component
public class LinuxProcessMonitor extends ProcessMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(LinuxProcessMonitor.class);
    
    // Patterns for process detection
    private static final Pattern ZOOM_PROCESS_PATTERN = Pattern.compile(
        ".*[Zz]oom.*|.*zoom\\.us.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZOOM_WINDOW_PATTERN = Pattern.compile(
        ".*[Zz]oom.*[Mm]eeting.*|.*Zoom.*", Pattern.CASE_INSENSITIVE);
    
    // Paths for system information
    private static final Path PROC_PATH = Paths.get("/proc");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    protected Mono<List<ProcessInfo>> getCurrentProcesses() {
        return Mono.fromCallable(() -> {
            var processes = new ArrayList<ProcessInfo>();
            
            try (Stream<Path> procDirs = Files.list(PROC_PATH)) {
                procDirs.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(this::isNumeric)
                    .forEach(pid -> {
                        try {
                            var processInfo = getProcessInfo(pid);
                            if (processInfo != null && isZoomProcess(processInfo.processName())) {
                                processes.add(processInfo);
                            }
                        } catch (Exception e) {
                            logger.debug("Error reading process info for PID {}", pid, e);
                        }
                    });
            } catch (IOException e) {
                logger.error("Error listing processes", e);
            }
            
 return (List<ProcessInfo>) processes;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    @Override
    protected Mono<String> getWindowTitle(String processId) {
        return Mono.fromCallable(() -> {
            try {
                // Use xdotool to get window titles (if available)
                var process = Runtime.getRuntime().exec(
                    new String[]{"xdotool", "search", "--pid", processId, "--name", ".*"});
                var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                var windowId = reader.readLine();
                reader.close();
                process.waitFor();
                
                if (windowId != null && !windowId.trim().isEmpty()) {
                    // Get window title for the found window
                    var titleProcess = Runtime.getRuntime().exec(
                        new String[]{"xdotool", "getwindowname", windowId.trim()});
                    var titleReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(titleProcess.getInputStream()));
                    
                    var title = titleReader.readLine();
                    titleReader.close();
                    titleProcess.waitFor();
                    
                    return title != null ? title.trim() : null;
                }
                
                // Fallback: try wmctrl
                var wmctrlProcess = Runtime.getRuntime().exec(
                    new String[]{"wmctrl", "-l"});
                var wmctrlReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(wmctrlProcess.getInputStream()));
                
                String line;
                while ((line = wmctrlReader.readLine()) != null) {
                    var parts = line.split("\\s+", 4);
                    if (parts.length >= 4) {
                        var windowTitle = parts[3];
                        if (windowTitle != null && ZOOM_WINDOW_PATTERN.matcher(windowTitle).matches()) {
                            // Try to get PID for this window
                            var pidProcess = Runtime.getRuntime().exec(
                                new String[]{"xprop", "-id", parts[0], "_NET_WM_PID"});
                            var pidReader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(pidProcess.getInputStream()));
                            
                            String pidLine;
                            while ((pidLine = pidReader.readLine()) != null) {
                                if (pidLine.contains("_NET_WM_PID(CARDINAL)")) {
                                    var pidValue = pidLine.split("=")[1].trim();
                                    if (pidValue.equals(processId)) {
                                        pidReader.close();
                                        pidProcess.waitFor();
                                        wmctrlReader.close();
                                        wmctrlProcess.waitFor();
                                        return windowTitle;
                                    }
                                }
                            }
                            pidReader.close();
                            pidProcess.waitFor();
                        }
                    }
                }
                wmctrlReader.close();
                wmctrlProcess.waitFor();
                
            } catch (Exception e) {
                logger.debug("Error getting window title for process {}", processId, e);
            }
            
            return null;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Gets process information from /proc filesystem.
     * 
     * @param pid the process ID
     * @return ProcessInfo object or null if not found
     */
    private ProcessInfo getProcessInfo(String pid) {
        try {
            var procPath = PROC_PATH.resolve(pid);
            
            // Read process name from comm file
            var commPath = procPath.resolve("comm");
            if (!Files.exists(commPath)) {
                return null;
            }
            
            var processName = Files.readString(commPath).trim();
            
            // Read command line from cmdline file
            var cmdlinePath = procPath.resolve("cmdline");
            var commandLine = "";
            if (Files.exists(cmdlinePath)) {
                commandLine = Files.readString(cmdlinePath).replace("\0", " ").trim();
            }
            
            // Read stat file for timing information
            var statPath = procPath.resolve("stat");
            var statContent = Files.readString(statPath);
            var statParts = statContent.split(" ");
            
            if (statParts.length < 22) {
                return null;
            }
            
            // Parse start time (field 22 in stat file)
            var startTimeTicks = Long.parseLong(statParts[21]);
            var startTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(
                    (startTimeTicks * 1000) / 100), // Convert to milliseconds (approximate)
                ZoneId.systemDefault());
            
            // Read status file for CPU and memory usage
            var statusPath = procPath.resolve("status");
            var statusContent = Files.readString(statusPath);
            var cpuUsage = parseCpuUsage(statusContent);
            var memoryUsage = parseMemoryUsage(statusContent);
            
            return new ProcessInfo(
                pid,
                processName,
                commandLine,
                startTime,
                cpuUsage,
                memoryUsage
            );
            
        } catch (Exception e) {
            logger.debug("Error reading process info for PID {}", pid, e);
            return null;
        }
    }
    
    /**
     * Parses CPU usage from /proc/[pid]/status content.
     * 
     * @param statusContent the status file content
     * @return CPU usage percentage
     */
    private double parseCpuUsage(String statusContent) {
        try {
            var lines = statusContent.split("\n");
            for (var line : lines) {
                if (line.startsWith("cpu_usage:")) {
                    var parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        return Double.parseDouble(parts[1]);
                    }
                }
            }
            
            // Fallback: calculate from utime and stime
            long utime = 0, stime = 0, totalTime = 0;
            for (var line : lines) {
                if (line.startsWith("utime:")) {
                    utime = Long.parseLong(line.split("\\s+")[1]);
                } else if (line.startsWith("stime:")) {
                    stime = Long.parseLong(line.split("\\s+")[1]);
                }
            }
            
            totalTime = utime + stime;
            return totalTime > 0 ? (double) totalTime / 100.0 : 0.0; // Convert to percentage
            
        } catch (Exception e) {
            logger.debug("Error parsing CPU usage", e);
            return 0.0;
        }
    }
    
    /**
     * Parses memory usage from /proc/[pid]/status content.
     * 
     * @param statusContent the status file content
     * @return memory usage in bytes
     */
    private long parseMemoryUsage(String statusContent) {
        try {
            var lines = statusContent.split("\n");
            for (var line : lines) {
                if (line.startsWith("VmRSS:")) {
                    var parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        // VmRSS is in kB, convert to bytes
                        return Long.parseLong(parts[1]) * 1024;
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            logger.debug("Error parsing memory usage", e);
            return 0;
        }
    }
    
    /**
     * Checks if a string represents a numeric value.
     * 
     * @param str the string to check
     * @return true if numeric
     */
    private boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Checks if a process name indicates a Zoom process.
     * 
     * @param processName the process name
     * @return true if it's a Zoom process
     */
    private boolean isZoomProcess(String processName) {
        return processName != null && 
               (ZOOM_PROCESS_PATTERN.matcher(processName).matches() ||
                processName.toLowerCase().contains("zoom"));
    }
    
    /**
     * Gets additional process information using system commands.
     * 
     * @param processId the process ID
     * @return enhanced ProcessInfo with additional details
     */
    private ProcessInfo getEnhancedProcessInfo(String processId) {
        try {
            // Use ps command for additional information
            var process = Runtime.getRuntime().exec(
                new String[]{"ps", "-p", processId, "-o", "pid,comm,lstart,%cpu,rss"});
            var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            // Skip header line
            reader.readLine();
            var line = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (line != null) {
                return parsePsLine(line);
            }
            
        } catch (Exception e) {
            logger.debug("Error getting enhanced process info for {}", processId, e);
        }
        
        return null;
    }
    
    /**
     * Parses a line from ps command output.
     * 
     * @param line the ps output line
     * @return ProcessInfo object
     */
    private ProcessInfo parsePsLine(String line) {
        try {
            // Expected format: PID COMMAND START_TIME %CPU RSS
            var parts = line.trim().split("\\s+", 6);
            if (parts.length < 6) {
                return null;
            }
            
            var processId = parts[0];
            var processName = parts[1];
            var cpuUsage = Double.parseDouble(parts[parts.length - 2]);
            var memoryUsage = Long.parseLong(parts[parts.length - 1]) * 1024; // Convert KB to bytes
            
            // Parse start time (simplified)
            var startTime = LocalDateTime.now().minusDays(1); // Placeholder
            
            return new ProcessInfo(
                processId,
                processName,
                "",
                startTime,
                cpuUsage,
                memoryUsage
            );
            
        } catch (Exception e) {
            logger.debug("Error parsing ps line: {}", line, e);
            return null;
        }
    }
}