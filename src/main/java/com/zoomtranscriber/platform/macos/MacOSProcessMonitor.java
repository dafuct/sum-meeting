package com.zoomtranscriber.platform.macos;

import com.zoomtranscriber.core.detection.ProcessMonitor;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * macOS-specific implementation of process monitoring using native system APIs.
 * Uses JNA to access macOS process and window information.
 */
@Component
public class MacOSProcessMonitor extends ProcessMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(MacOSProcessMonitor.class);
    
    // Native library interfaces
    private static final MacSystemLib SYSTEM_LIB = Native.load("System", MacSystemLib.class);
    private static final MacWindowLib WINDOW_LIB = Native.load("Cocoa", MacWindowLib.class);
    
    // Patterns for process detection
    private static final Pattern ZOOM_PROCESS_PATTERN = Pattern.compile(
        ".*[Zz]oom.*|.*zoom\\.us.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZOOM_WINDOW_PATTERN = Pattern.compile(
        ".*[Zz]oom.*[Mm]eeting.*|.*Zoom.*", Pattern.CASE_INSENSITIVE);
    
    @Override
    protected Mono<List<ProcessInfo>> getCurrentProcesses() {
        return Mono.fromCallable(() -> {
            var processes = new ArrayList<ProcessInfo>();
            
            try {
                // Get all processes using ps command
                var process = Runtime.getRuntime().exec(
                    new String[]{"ps", "-eo", "pid,comm,lstart,%cpu,rss"});
                var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                var isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // Skip header
                        continue;
                    }
                    
                    var processInfo = parseProcessLine(line);
                    if (processInfo != null && isZoomProcess(processInfo.processName())) {
                        processes.add(processInfo);
                    }
                }
                
                reader.close();
                process.waitFor();
                
            } catch (Exception e) {
                logger.error("Error getting current processes", e);
            }
            
 return (List<ProcessInfo>) processes;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    @Override
    protected Mono<String> getWindowTitle(String processId) {
        return Mono.fromCallable(() -> {
            try {
                // Use AppleScript to get window title for the process
                var script = String.format(
                    "tell application \"System Events\"\n" +
                    "    set processList to every process whose unix id is %s\n" +
                    "    if (count of processList) > 0 then\n" +
                    "        set targetProcess to item 1 of processList\n" +
                    "        try\n" +
                    "            set windowTitle to title of front window of targetProcess\n" +
                    "            return windowTitle\n" +
                    "        on error\n" +
                    "            return name of targetProcess\n" +
                    "        end try\n" +
                    "    end if\n" +
                    "end tell", processId);
                
                var process = Runtime.getRuntime().exec(
                    new String[]{"osascript", "-e", script});
                var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                var title = reader.readLine();
                reader.close();
                process.waitFor();
                
                return title != null ? title.trim() : null;
                
            } catch (Exception e) {
                logger.debug("Error getting window title for process {}", processId, e);
                return null;
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Parses a line from ps command output into ProcessInfo.
     * 
     * @param line the ps output line
     * @return ProcessInfo object or null if parsing fails
     */
    private ProcessInfo parseProcessLine(String line) {
        try {
            // Expected format: PID COMMAND START_TIME %CPU RSS
            // Example: "1234 zoom.us Mon Jan 1 10:00:00 2024 2.5 123456"
            
            var parts = line.trim().split("\\s+", 6);
            if (parts.length < 6) {
                return null;
            }
            
            var processId = parts[0];
            var processName = parts[1];
            var cpuUsage = Double.parseDouble(parts[parts.length - 2]);
            var memoryUsage = Long.parseLong(parts[parts.length - 1]) * 1024; // Convert KB to bytes
            
            // Parse start time (simplified - in real implementation would need better parsing)
            var startTime = LocalDateTime.now().minusDays(1); // Placeholder
            
            return new ProcessInfo(
                processId,
                processName,
                "", // Command line not available from ps
                startTime,
                cpuUsage,
                memoryUsage
            );
            
        } catch (Exception e) {
            logger.debug("Error parsing process line: {}", line, e);
            return null;
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
     * Gets additional process information using native APIs.
     * 
     * @param processId the process ID
     * @return enhanced ProcessInfo with additional details
     */
    private ProcessInfo getEnhancedProcessInfo(String processId) {
        try {
            var pid = Integer.parseInt(processId);
            
            // Get process information using native APIs
            var procInfo = new MacProcessInfo();
            var size = new IntByReference(procInfo.size());
            
            var result = SYSTEM_LIB.proc_pidinfo(pid, 2, 0, procInfo.getPointer(), size.getValue());
            if (result <= 0) {
                return null;
            }
            
            // Get window information
            var windowTitle = getWindowTitle(processId).block();
            
            return new ProcessInfo(
                processId,
                new String(procInfo.processName).trim(),
                "",
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(procInfo.startTime), 
                    ZoneId.systemDefault()),
                procInfo.cpuUsage,
                procInfo.memoryUsage
            );
            
        } catch (Exception e) {
            logger.debug("Error getting enhanced process info for {}", processId, e);
            return null;
        }
    }
    
    /**
     * JNA interface for macOS system library.
     */
    public interface MacSystemLib extends com.sun.jna.Library {
        int proc_pidinfo(int pid, int flavor, long arg, Pointer buffer, int size);
        int proc_listpids(int type, int typeinfo, int[] buffer, int buffersize);
        int proc_name(int pid, byte[] buffer, int buffersize);
    }
    
    /**
     * JNA interface for macOS window library.
     */
    public interface MacWindowLib extends com.sun.jna.Library {
        Pointer CGWindowListCopyWindowInfo(int option, long relativeToWindow);
        void CFRelease(Pointer cf);
    }
    
    /**
     * Structure for macOS process information.
     */
    public static class MacProcessInfo extends Structure {
        public int size;
        public byte[] processName = new byte[16];
        public long startTime;
        public double cpuUsage;
        public long memoryUsage;
        
        public MacProcessInfo() {
            super();
        }
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("size", "processName", "startTime", "cpuUsage", "memoryUsage");
        }
    }
    
    /**
     * Structure for macOS window information.
     */
    public static class MacWindowInfo extends Structure {
        public int windowNumber;
        public int windowLayer;
        public Pointer windowName;
        public int ownerPid;
        public long windowBounds;
        
        public MacWindowInfo() {
            super();
        }
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("windowNumber", "windowLayer", "windowName", "ownerPid", "windowBounds");
        }
    }
}