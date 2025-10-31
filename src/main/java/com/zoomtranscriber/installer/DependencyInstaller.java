package com.zoomtranscriber.installer;

import com.zoomtranscriber.core.storage.InstallationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Component
public class DependencyInstaller {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyInstaller.class);
    private final Executor taskExecutor;
    
    public DependencyInstaller(Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }
    
    public CompletableFuture<InstallationResult> installJava(Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Checking system architecture...");
                var arch = System.getProperty("os.arch");
                var os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("mac")) {
                    return installJavaMac(progressCallback, arch);
                } else if (os.contains("linux")) {
                    return installJavaLinux(progressCallback, arch);
                } else {
                    return new InstallationResult(
                        InstallationState.Component.Java,
                        InstallationState.InstallStatus.ERROR,
                        null,
                        "Unsupported operating system: " + os
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to install Java", e);
                return new InstallationResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "Installation failed: " + e.getMessage()
                );
            }
        }, taskExecutor);
    }
    
    public CompletableFuture<InstallationResult> installOllama(Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Checking system architecture...");
                var os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("mac")) {
                    return installOllamaMac(progressCallback);
                } else if (os.contains("linux")) {
                    return installOllamaLinux(progressCallback);
                } else {
                    return new InstallationResult(
                        InstallationState.Component.Ollama,
                        InstallationState.InstallStatus.ERROR,
                        null,
                        "Unsupported operating system: " + os
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to install Ollama", e);
                return new InstallationResult(
                    InstallationState.Component.Ollama,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "Installation failed: " + e.getMessage()
                );
            }
        }, taskExecutor);
    }
    
    public CompletableFuture<InstallationResult> installModel(Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Downloading Qwen model...");
                var result = executeCommand(new String[]{"ollama", "pull", "qwen2:0.5b"}, progressCallback);
                
                if (result.exitCode() == 0) {
                    return new InstallationResult(
                        InstallationState.Component.Model,
                        InstallationState.InstallStatus.INSTALLED,
                        "qwen2:0.5b",
                        null
                    );
                } else {
                    return new InstallationResult(
                        InstallationState.Component.Model,
                        InstallationState.InstallStatus.ERROR,
                        null,
                        "Failed to download model: " + result.error()
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to install model", e);
                return new InstallationResult(
                    InstallationState.Component.Model,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "Installation failed: " + e.getMessage()
                );
            }
        }, taskExecutor);
    }
    
    private InstallationResult installJavaMac(Consumer<String> progressCallback, String arch) {
        try {
            progressCallback.accept("Installing Java using Homebrew...");
            var result = executeCommand(new String[]{"brew", "install", "openjdk@21"}, progressCallback);
            
            if (result.exitCode() == 0) {
                return new InstallationResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.INSTALLED,
                    "21",
                    "/opt/homebrew/opt/openjdk@21/bin/java"
                );
            } else {
                return new InstallationResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "Homebrew installation failed: " + result.error()
                );
            }
        } catch (Exception e) {
            return new InstallationResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.ERROR,
                null,
                "Mac Java installation failed: " + e.getMessage()
            );
        }
    }
    
    private InstallationResult installJavaLinux(Consumer<String> progressCallback, String arch) {
        try {
            progressCallback.accept("Updating package manager...");
            executeCommand(new String[]{"sudo", "apt", "update"}, progressCallback);
            
            progressCallback.accept("Installing OpenJDK 21...");
            var result = executeCommand(new String[]{"sudo", "apt", "install", "-y", "openjdk-21-jdk"}, progressCallback);
            
            if (result.exitCode() == 0) {
                return new InstallationResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.INSTALLED,
                    "21",
                    "/usr/bin/java"
                );
            } else {
                return new InstallationResult(
                    InstallationState.Component.Java,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "APT installation failed: " + result.error()
                );
            }
        } catch (Exception e) {
            return new InstallationResult(
                InstallationState.Component.Java,
                InstallationState.InstallStatus.ERROR,
                null,
                "Linux Java installation failed: " + e.getMessage()
            );
        }
    }
    
    private InstallationResult installOllamaMac(Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Installing Ollama using Homebrew...");
            var result = executeCommand(new String[]{"brew", "install", "ollama"}, progressCallback);
            
            if (result.exitCode() == 0) {
                return new InstallationResult(
                    InstallationState.Component.Ollama,
                    InstallationState.InstallStatus.INSTALLED,
                    "latest",
                    "/opt/homebrew/bin/ollama"
                );
            } else {
                return new InstallationResult(
                    InstallationState.Component.Ollama,
                    InstallationState.InstallStatus.ERROR,
                    null,
                    "Homebrew installation failed: " + result.error()
                );
            }
        } catch (Exception e) {
            return new InstallationResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.ERROR,
                null,
                "Mac Ollama installation failed: " + e.getMessage()
            );
        }
    }
    
    private InstallationResult installOllamaLinux(Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Downloading Ollama installation script...");
            var result = executeCommand(new String[]{"curl", "-fsSL", "https://ollama.com/install.sh", "-o", "/tmp/install.sh"}, progressCallback);
            
            if (result.exitCode() == 0) {
                progressCallback.accept("Running Ollama installation script...");
                result = executeCommand(new String[]{"sh", "/tmp/install.sh"}, progressCallback);
                
                if (result.exitCode() == 0) {
                    return new InstallationResult(
                        InstallationState.Component.Ollama,
                        InstallationState.InstallStatus.INSTALLED,
                        "latest",
                        "/usr/local/bin/ollama"
                    );
                }
            }
            
            return new InstallationResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.ERROR,
                null,
                "Linux Ollama installation failed: " + result.error()
            );
        } catch (Exception e) {
            return new InstallationResult(
                InstallationState.Component.Ollama,
                InstallationState.InstallStatus.ERROR,
                null,
                "Linux Ollama installation failed: " + e.getMessage()
            );
        }
    }
    
    private CommandResult executeCommand(String[] command, Consumer<String> progressCallback) {
        try {
            var process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (progressCallback != null) {
                        progressCallback.accept(line);
                    }
                }
            }
            
            var exitCode = process.waitFor();
            return new CommandResult(exitCode, output.toString(), null);
        } catch (Exception e) {
            return new CommandResult(-1, null, e.getMessage());
        }
    }
    
    public record InstallationResult(
            InstallationState.Component component,
            InstallationState.InstallStatus status,
            String version,
            String error
    ) {
        public boolean isSuccess() {
            return status == InstallationState.InstallStatus.INSTALLED;
        }
    }
    
    public record CommandResult(
            int exitCode,
            String output,
            String error
    ) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}