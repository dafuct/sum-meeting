package com.zoomtranscriber.platform.linux;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class LinuxInstaller {
    
    private static final Logger logger = LoggerFactory.getLogger(LinuxInstaller.class);
    
    private static final List<String> REQUIRED_TOOLS = Arrays.asList("apt", "curl", "sh", "wget");
    private static final List<String> SUPPORTED_DISTROS = Arrays.asList("ubuntu", "debian", "centos", "rhel", "fedora");
    
    public PlatformCheckResult checkPlatform() {
        var osName = System.getProperty("os.name").toLowerCase();
        var osVersion = System.getProperty("os.version");
        
        if (!osName.contains("linux")) {
            return new PlatformCheckResult(
                    false,
                    "This installer is for Linux only. Current OS: " + osName,
                    null
            );
        }
        
        var distro = getLinuxDistribution();
        if (distro.isEmpty() || !SUPPORTED_DISTROS.contains(distro.get().toLowerCase())) {
            return new PlatformCheckResult(
                    false,
                    "Unsupported Linux distribution: " + distro.orElse("Unknown"),
                    null
            );
        }
        
        var toolsCheck = checkRequiredTools();
        if (!toolsCheck.allToolsAvailable()) {
            return new PlatformCheckResult(
                    false,
                    "Missing required tools: " + toolsCheck.missingTools(),
                    null
            );
        }
        
        return new PlatformCheckResult(
                true,
                "Linux platform ready",
                new PlatformInfo(osName, osVersion, distro.get(), getArchitecture())
        );
    }
    
    public InstallationResult updatePackageIndex() {
        try {
            logger.info("Updating package index...");
            var process = Runtime.getRuntime().exec(new String[]{"sudo", "apt", "update"});
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return new InstallationResult(
                        InstallationState.InstallStatus.INSTALLED,
                        "Package index updated successfully",
                        null
                );
            } else {
                return new InstallationResult(
                        InstallationState.InstallStatus.ERROR,
                        "Package index update failed: " + output.toString(),
                        null
                );
            }
        } catch (Exception e) {
            logger.error("Failed to update package index", e);
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Package index update error: " + e.getMessage(),
                    null
            );
        }
    }
    
    public InstallationResult installJava() {
        try {
            logger.info("Installing OpenJDK 21 via APT...");
            var process = Runtime.getRuntime().exec(new String[]{"sudo", "apt", "install", "-y", "openjdk-21-jdk"});
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var javaPath = "/usr/bin/java";
                if (Files.exists(Paths.get(javaPath))) {
                    return new InstallationResult(
                            InstallationState.InstallStatus.INSTALLED,
                            "OpenJDK 21 installed via APT",
                            javaPath
                    );
                }
            }
            
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Java installation failed: " + output.toString(),
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to install Java", e);
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Java installation error: " + e.getMessage(),
                    null
            );
        }
    }
    
    public InstallationResult installOllama() {
        try {
            logger.info("Installing Ollama...");
            
            var downloadScript = downloadOllamaScript();
            if (downloadScript.status() != InstallationState.InstallStatus.INSTALLED) {
                return downloadScript;
            }
            
            var installResult = runOllamaInstallScript();
            if (installResult.status() != InstallationState.InstallStatus.INSTALLED) {
                return installResult;
            }
            
            var ollamaPath = "/usr/local/bin/ollama";
            if (Files.exists(Paths.get(ollamaPath))) {
                return new InstallationResult(
                        InstallationState.InstallStatus.INSTALLED,
                        "Ollama installed successfully",
                        ollamaPath
                );
            } else {
                return new InstallationResult(
                        InstallationState.InstallStatus.ERROR,
                        "Ollama installation completed but binary not found at expected location",
                        null
                );
            }
        } catch (Exception e) {
            logger.error("Failed to install Ollama", e);
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Ollama installation error: " + e.getMessage(),
                    null
            );
        }
    }
    
    public InstallationResult setupEnvironment() {
        try {
            var shellConfig = getShellConfigPath();
            
            if (shellConfig.isPresent()) {
                var configPath = shellConfig.get();
                var configContent = Files.readString(Paths.get(configPath));
                
                var ollamaPath = "/usr/local/bin";
                if (!configContent.contains(ollamaPath)) {
                    var envLine = "\n# Ollama\nexport PATH=\"" + ollamaPath + ":$PATH\"\n";
                    Files.writeString(Paths.get(configPath), configContent + envLine);
                    
                    return new InstallationResult(
                            InstallationState.InstallStatus.INSTALLED,
                            "Environment variables updated",
                            configPath
                    );
                } else {
                    return new InstallationResult(
                            InstallationState.InstallStatus.INSTALLED,
                            "Environment already configured",
                            configPath
                    );
                }
            }
            
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Could not find shell configuration file",
                    null
            );
        } catch (Exception e) {
            logger.error("Failed to setup environment", e);
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Environment setup error: " + e.getMessage(),
                    null
            );
        }
    }
    
    private Optional<String> getLinuxDistribution() {
        try {
            if (Files.exists(Paths.get("/etc/os-release"))) {
                var content = Files.readString(Paths.get("/etc/os-release"));
                return content.lines()
                        .filter(line -> line.startsWith("ID="))
                        .map(line -> line.substring(3).replace("\"", ""))
                        .findFirst();
            }
            
            if (Files.exists(Paths.get("/etc/lsb-release"))) {
                var content = Files.readString(Paths.get("/etc/lsb-release"));
                return content.lines()
                        .filter(line -> line.startsWith("DISTRIB_ID="))
                        .map(line -> line.substring(11).replace("\"", ""))
                        .findFirst();
            }
            
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to detect Linux distribution", e);
            return Optional.empty();
        }
    }
    
    private ToolsCheckResult checkRequiredTools() {
        var missingTools = REQUIRED_TOOLS.stream()
                .filter(tool -> !isToolAvailable(tool))
                .toList();
        
        return new ToolsCheckResult(missingTools.isEmpty(), missingTools);
    }
    
    private boolean isToolAvailable(String tool) {
        try {
            var process = Runtime.getRuntime().exec(new String[]{"which", tool});
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private InstallationResult downloadOllamaScript() {
        try {
            logger.info("Downloading Ollama installation script...");
            var process = Runtime.getRuntime().exec(new String[]{
                    "curl", "-fsSL", "https://ollama.com/install.sh", "-o", "/tmp/install.sh"
            });
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0 && Files.exists(Paths.get("/tmp/install.sh"))) {
                return new InstallationResult(
                        InstallationState.InstallStatus.INSTALLED,
                        "Ollama script downloaded",
                        "/tmp/install.sh"
                );
            } else {
                return new InstallationResult(
                        InstallationState.InstallStatus.ERROR,
                        "Failed to download Ollama installation script",
                        null
                );
            }
        } catch (Exception e) {
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Download error: " + e.getMessage(),
                    null
            );
        }
    }
    
    private InstallationResult runOllamaInstallScript() {
        try {
            logger.info("Running Ollama installation script...");
            var process = Runtime.getRuntime().exec(new String[]{"sh", "/tmp/install.sh"});
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return new InstallationResult(
                        InstallationState.InstallStatus.INSTALLED,
                        "Ollama installation script completed",
                        null
                );
            } else {
                return new InstallationResult(
                        InstallationState.InstallStatus.ERROR,
                        "Ollama installation script failed: " + output.toString(),
                        null
                );
            }
        } catch (Exception e) {
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Script execution error: " + e.getMessage(),
                    null
            );
        }
    }
    
    private Optional<String> getShellConfigPath() {
        var home = System.getProperty("user.home");
        var shell = System.getenv("SHELL");
        
        if (shell != null) {
            if (shell.contains("zsh")) {
                return Optional.of(Paths.get(home, ".zshrc").toString());
            } else if (shell.contains("bash")) {
                return Optional.of(Paths.get(home, ".bashrc").toString());
            }
        }
        
        var bashrc = Paths.get(home, ".bashrc");
        if (Files.exists(bashrc)) {
            return Optional.of(bashrc.toString());
        }
        
        var profile = Paths.get(home, ".profile");
        if (Files.exists(profile)) {
            return Optional.of(profile.toString());
        }
        
        return Optional.empty();
    }
    
    private String getArchitecture() {
        var arch = System.getProperty("os.arch").toLowerCase();
        return arch.contains("aarch64") ? "arm64" : "x86_64";
    }
    
    public record PlatformCheckResult(
            boolean isSupported,
            String message,
            PlatformInfo platformInfo
    ) {
        public boolean isReady() {
            return isSupported && platformInfo != null;
        }
    }
    
    public record PlatformInfo(
            String osName,
            String osVersion,
            String distribution,
            String architecture
    ) {}
    
    public record ToolsCheckResult(
            boolean allToolsAvailable,
            List<String> missingTools
    ) {}
    
    public record InstallationResult(
            InstallationState.InstallStatus status,
            String message,
            String installPath
    ) {
        public boolean isSuccess() {
            return status == InstallationState.InstallStatus.INSTALLED;
        }
    }
}