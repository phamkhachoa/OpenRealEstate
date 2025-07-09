package com.proptech.realestate.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing Python scripts from Java
 * Handles communication between Java application and Python NLP scripts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PythonExecutorService {

    private final Gson gson;

    @Value("${app.python.executable:python3}")
    private String pythonExecutable;

    @Value("${app.python.script.path:src/main/resources/nlp/underthesea_processor.py}")
    private String pythonScriptPath;

    @Value("${app.python.timeout.seconds:30}")
    private long timeoutSeconds;

    /**
     * Execute Python script with given text input
     * Results are cached to improve performance for repeated queries
     */
    @Cacheable(value = "nlpResults", key = "#input.hashCode()")
    public PythonExecutionResult executePythonScript(String input) {
        log.debug("Executing Python NLP script for input: {}", input.substring(0, Math.min(input.length(), 50)));
        
        try {
            // Prepare command
            CommandLine commandLine = new CommandLine(pythonExecutable);
            commandLine.addArgument(pythonScriptPath);
            commandLine.addArgument(input, false); // false = don't handle quotes
            
            // Configure executor
            DefaultExecutor executor = new DefaultExecutor();
            executor.setExitValue(0);
            
            // Set timeout
            ExecuteWatchdog watchdog = new ExecuteWatchdog(TimeUnit.SECONDS.toMillis(timeoutSeconds));
            executor.setWatchdog(watchdog);
            
            // Capture output
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            executor.setStreamHandler(streamHandler);
            
            // Execute
            int exitCode = executor.execute(commandLine);
            
            String output = outputStream.toString(StandardCharsets.UTF_8);
            String error = errorStream.toString(StandardCharsets.UTF_8);
            
            log.debug("Python script executed with exit code: {}", exitCode);
            
            if (exitCode == 0 && !output.trim().isEmpty()) {
                return PythonExecutionResult.success(output.trim());
            } else {
                log.error("Python script execution failed. Exit code: {}, Error: {}", exitCode, error);
                return PythonExecutionResult.error("Python execution failed: " + error);
            }
            
        } catch (IOException e) {
            log.error("Failed to execute Python script", e);
            return PythonExecutionResult.error("IO Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during Python script execution", e);
            return PythonExecutionResult.error("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Parse JSON output from Python script
     */
    public <T> T parseJsonResult(String jsonOutput, Class<T> resultType) {
        try {
            return gson.fromJson(jsonOutput, resultType);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON output: {}", jsonOutput, e);
            throw new IllegalArgumentException("Invalid JSON output from Python script", e);
        }
    }

    /**
     * Check if Python and required dependencies are available
     */
    public boolean checkPythonEnvironment() {
        try {
            // Test basic Python execution
            CommandLine testCommand = new CommandLine(pythonExecutable);
            testCommand.addArgument("--version");
            
            DefaultExecutor executor = new DefaultExecutor();
            executor.setExitValue(0);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);
            
            int exitCode = executor.execute(testCommand);
            String output = outputStream.toString(StandardCharsets.UTF_8);
            
            log.info("Python version check: {}", output.trim());
            
            // Test underthesea import
            CommandLine importTest = new CommandLine(pythonExecutable);
            importTest.addArgument("-c");
            importTest.addArgument("import underthesea; print('underthesea available')");
            
            exitCode = executor.execute(importTest);
            
            if (exitCode == 0) {
                log.info("UndertheSea library is available");
                return true;
            } else {
                log.warn("UndertheSea library not available");
                return false;
            }
            
        } catch (Exception e) {
            log.error("Python environment check failed", e);
            return false;
        }
    }

    /**
     * Result wrapper for Python script execution
     */
    public static class PythonExecutionResult {
        private final boolean success;
        private final String output;
        private final String error;

        private PythonExecutionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static PythonExecutionResult success(String output) {
            return new PythonExecutionResult(true, output, null);
        }

        public static PythonExecutionResult error(String error) {
            return new PythonExecutionResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }
    }
} 