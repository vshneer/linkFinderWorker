package com.annalabs.linkFinderWorker;

import org.springframework.boot.SpringApplication;

public class TestLinkFinderWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.from(LinkFinderWorkerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
