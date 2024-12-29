package com.annalabs.linkFinderWorker.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class LinkFinderWorker {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.api}")
    private String topic;

    public void processMessage(String message) {
        String fileNameInput = message + "_in.txt";
        String fileNameOutput = message + "_out.txt";
        String fieParamsOutput = message + "_params_out.txt";
        File tempDir = new File(System.getProperty("java.io.tmpdir")); // System temp directory
        File pathInputFile = new File(tempDir, fileNameInput); // Combine tempDir and fileName
        File pathOutputFile = new File(tempDir, fileNameOutput); // Combine tempDir and fileName
        File pathParamsOutputFile = new File(tempDir, fieParamsOutput);
        if (!tempDir.exists()) {
            System.err.println("Temp directory does not exist: " + tempDir);
            return;
        }
        try {
            // Write message to file
            // Write the message to the input file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathInputFile))) {
                writer.write(message);
                writer.newLine(); // Ensure a newline for proper parsing by the tool
            } catch (IOException e) {
                System.err.println("Error writing to input file: " + e.getMessage());
                return;
            }
            String command = "xnLinkFinder -i " + pathInputFile.getAbsolutePath() + " -o " + pathOutputFile.getAbsolutePath() + " -sp " + message + " -sf " + message + " -op " + pathParamsOutputFile.getAbsolutePath();
            // Execute the CLI command
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectErrorStream(true); // Redirect stderr to stdout
            Process process = processBuilder.start();

            // Read process output asynchronously
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new BufferedReader(new FileReader(pathOutputFile)))) {
                    reader.lines().forEach(line -> {
                        kafkaTemplate.send(topic, line);
                    }); // Process output
                } catch (IOException e) {
                    System.err.println("Error reading from output file: " + e.getMessage());
                }
            });

            // Wait for the process to complete
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                System.err.println("Process destroyed forcibly");
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing command: " + e.getMessage());
        }
    }

}
