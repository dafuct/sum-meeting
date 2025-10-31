package com.zoomtranscriber.installer;

import com.zoomtranscriber.core.storage.InstallationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SystemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemChecker.class);
    
    public SystemCheckResult checkSystem() {
        var results = new ArrayList<ComponentCheckResult>();
        
        results.add(checkJava());
        results.add(checkOllama());
        results.add(checkModel());
        
        return new SystemCheckResult(results);
    }
    
    public ComponentCheckResult checkJava() {
        try {
            var process = Runtime.getRuntime().exec(new String[]{"java", "-version"});
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var version = extractJavaVersion(process);
                var path = findJavaPath();
                return new ComponentCheckResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.INSTALLED,
                    version,
                    path,
                    null
                );
            } else {
                return new ComponentCheckResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.NOT_INSTALLED,
                    null,
                    null,
                    "Java is not installed or not in PATH"
                );
            }
        } catch (Exception e) {
            logger.error("Error checking Java installation", e);
            return new ComponentCheckResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.ERROR,
                null,
                null,
                "Error checking Java: " + e.getMessage()
            );
        }
    }
    
    public ComponentCheckResult checkOllama() {
        try {
            var process = Runtime.getRuntime().exec(new String[]{"ollama", "--version"});
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var version = extractOllamaVersion(process);
                var path = findOllamaPath();
                return new ComponentCheckResult(
                    InstallationState.Component.Ollama,
                    InstallationState.InstallStatus.INSTALLED,
                    version,
                    path,
                    null
                );
            } else {
                return new ComponentCheckResult(
                    InstallationState.Component.Ollama,
                    InstallationState.InstallStatus.NOT_INSTALLED,
                    null,
                    null,
                    "Ollama is not installed or not in PATH"
                );
            }
        } catch (Exception e) {
            logger.error("Error checking Ollama installation", e);
            return new ComponentCheckResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.ERROR,
                null,
                null,
                "Error checking Ollama: " + e.getMessage()
            );
        }
    }
    
    public ComponentCheckResult checkModel() {
        try {
            var process = Runtime.getRuntime().exec(new String[]{"ollama", "list"});
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var models = extractInstalledModels(process);
                var hasQwenModel = models.stream().anyMatch(model -> model.toLowerCase().contains("qwen"));
                
                if (hasQwenModel) {
                    return new ComponentCheckResult(
                        InstallationState.Component.Model,
                        InstallationState.InstallStatus.INSTALLED,
                        "qwen model available",
                        null,
                        null
                    );
                } else {
                    return new ComponentCheckResult(
                        InstallationState.Component.Model,
                        InstallationState.InstallStatus.NOT_INSTALLED,
                        null,
                        null,
                        "Qwen model is not installed. Run: ollama pull qwen2:0.5b"
                    );
                }
            } else {
                return new ComponentCheckResult(
                    InstallationState.Component.Model,
                    InstallationState.InstallStatus.NOT_INSTALLED,
                    null,
                    null,
                    "Cannot check models - Ollama not installed"
                );
            }
        } catch (Exception e) {
            logger.error("Error checking model installation", e);
            return new ComponentCheckResult(
                InstallationState.Component.Model,
                InstallationState.InstallStatus.ERROR,
                null,
                null,
                "Error checking model: " + e.getMessage()
            );
        }
    }
    
    private String extractJavaVersion(Process process) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            var line = reader.lines()
                    .filter(l -> l.contains("version"))
                    .findFirst()
                    .orElse("Unknown");
            
            if (line.contains("\"")) {
                var start = line.indexOf("\"") + 1;
                var end = line.lastIndexOf("\"");
                return line.substring(start, end);
            }
            return line;
        }
    }
    
    private String extractOllamaVersion(Process process) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .findFirst()
                    .orElse("Unknown");
        }
    }
    
    private List<String> extractInstalledModels(Process process) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.contains("NAME"))
                    .map(String::trim)
                    .toList();
        }
    }
    
    private String findJavaPath() {
        try {
            var paths = System.getenv("PATH").split(":");
            for (var path : paths) {
                var javaPath = Paths.get(path, "java");
                if (Files.isExecutable(javaPath)) {
                    return javaPath.toString();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String findOllamaPath() {
        try {
            var paths = System.getenv("PATH").split(":");
            for (var path : paths) {
                var ollamaPath = Paths.get(path, "ollama");
                if (Files.isExecutable(ollamaPath)) {
                    return ollamaPath.toString();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean isSystemReady() {
        var checkResult = checkSystem();
        return checkResult.results().stream()
                .allMatch(result -> result.status() == InstallationState.InstallStatus.INSTALLED);
    }
    
    public List<String> getMissingComponents() {
        var checkResult = checkSystem();
        return checkResult.results().stream()
                .filter(result -> result.status() == InstallationState.InstallStatus.NOT_INSTALLED)
                .map(result -> result.component().toString())
                .toList();
    }
    
    public record SystemCheckResult(List<ComponentCheckResult> results) {
        public boolean isAllInstalled() {
            return results.stream().allMatch(r -> r.status() == InstallationState.InstallStatus.INSTALLED);
        }
        
        public List<ComponentCheckResult> getFailedChecks() {
            return results.stream()
                    .filter(r -> r.status() != InstallationState.InstallStatus.INSTALLED)
                    .toList();
        }
    }
    
    public record ComponentCheckResult(
            InstallationState.Component component,
            InstallationState.InstallStatus status,
            String version,
            String installPath,
            String error
    ) {
        public boolean isInstalled() {
            return status == InstallationState.InstallStatus.INSTALLED;
        }
        
        public Optional<String> getError() {
            return Optional.ofNullable(error);
        }
    }
}