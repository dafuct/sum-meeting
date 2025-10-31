package com.zoomtranscriber.platform.macos;

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
public class MacOSInstaller {
    
    private static final Logger logger = LoggerFactory.getLogger(MacOSInstaller.class);
    
    private static final List<String> REQUIRED_TOOLS = Arrays.asList("brew", "curl", "sh");
    
    public PlatformCheckResult checkPlatform() {
        var osName = System.getProperty("os.name");
        var osVersion = System.getProperty("os.version");
        
        if (!osName.toLowerCase().contains("mac")) {
            return new PlatformCheckResult(
                    false,
                    "This installer is for macOS only. Current OS: " + osName,
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
                "macOS platform ready",
                new PlatformInfo(osName, osVersion, getArchitecture())
        );
    }
    
    public InstallationResult installHomebrew() {
        try {
            if (isHomebrewInstalled()) {
                return new InstallationResult(
                        InstallationState.InstallStatus.INSTALLED,
                        "Homebrew already installed",
                        "/opt/homebrew/bin/brew"
                );
            }
            
            logger.info("Installing Homebrew...");
            var command = "/bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"";
            var process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            
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
                        "Homebrew installed successfully",
                        "/opt/homebrew/bin/brew"
                );
            } else {
                return new InstallationResult(
                        InstallationState.InstallStatus.ERROR,
                        "Homebrew installation failed",
                        null
                );
            }
        } catch (Exception e) {
            logger.error("Failed to install Homebrew", e);
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Homebrew installation error: " + e.getMessage(),
                    null
            );
        }
    }
    
    public InstallationResult installJava() {
        try {
            logger.info("Installing Java via Homebrew...");
            var process = Runtime.getRuntime().exec(new String[]{"brew", "install", "openjdk@21"});
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var javaPath = "/opt/homebrew/opt/openjdk@21/bin/java";
                if (Files.exists(Paths.get(javaPath))) {
                    return new InstallationResult(
                            InstallationState.InstallStatus.INSTALLED,
                            "Java 21 installed via Homebrew",
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
            logger.info("Installing Ollama via Homebrew...");
            var process = Runtime.getRuntime().exec(new String[]{"brew", "install", "ollama"});
            
            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            var exitCode = process.waitFor();
            
            if (exitCode == 0) {
                var ollamaPath = "/opt/homebrew/bin/ollama";
                if (Files.exists(Paths.get(ollamaPath))) {
                    return new InstallationResult(
                            InstallationState.InstallStatus.INSTALLED,
                            "Ollama installed via Homebrew",
                            ollamaPath
                    );
                }
            }
            
            return new InstallationResult(
                    InstallationState.InstallStatus.ERROR,
                    "Ollama installation failed: " + output.toString(),
                    null
            );
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
            var homebrewPath = "/opt/homebrew/bin";
            var shellConfig = getShellConfigPath();
            
            if (shellConfig.isPresent()) {
                var configPath = shellConfig.get();
                var configContent = Files.readString(Paths.get(configPath));
                
                if (!configContent.contains(homebrewPath)) {
                    var envLine = "\n# Homebrew\nexport PATH=\"" + homebrewPath + ":$PATH\"\n";
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
    
    private boolean isHomebrewInstalled() {
        return isToolAvailable("brew");
    }
    
    private Optional<String> getShellConfigPath() {
        var home = System.getProperty("user.home");
        var shell = System.getenv("SHELL");
        
        if (shell != null) {
            if (shell.contains("zsh")) {
                return Optional.of(Paths.get(home, ".zshrc").toString());
            } else if (shell.contains("bash")) {
                return Optional.of(Paths.get(home, ".bash_profile").toString());
            }
        }
        
        var zshrc = Paths.get(home, ".zshrc");
        if (Files.exists(zshrc)) {
            return Optional.of(zshrc.toString());
        }
        
        var bashProfile = Paths.get(home, ".bash_profile");
        if (Files.exists(bashProfile)) {
            return Optional.of(bashProfile.toString());
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