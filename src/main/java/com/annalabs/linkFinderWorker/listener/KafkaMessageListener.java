package com.annalabs.linkFinderWorker.listener;

import com.annalabs.linkFinderWorker.worker.LinkFinderWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class KafkaMessageListener {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Thread pool for async processing
    @Autowired
    private LinkFinderWorker worker;

    @KafkaListener(topics = {"domains", "subdomains", "js"}, groupId = "link-finder-group")
    public void listen(String message) {
        executorService.submit(() -> worker.processMessage(message));
    }
}
