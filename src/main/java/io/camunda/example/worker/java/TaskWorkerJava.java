package io.camunda.example.worker.java;

import io.camunda.zeebe.client.ZeebeClient;

import java.util.Map;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskWorkerJava {

  private static final Logger LOG = LogManager.getLogger(TaskWorkerJava.class);

  public static void main(String[] args) {

    LOG.info("ZEEBE_CLUSTER_ID {}", System.getenv("ZEEBE_CLUSTER_ID"));
    LOG.info("ZEEBE_CLIENT_ID {}", System.getenv("ZEEBE_CLIENT_ID"));
    LOG.info("ZEEBE_CLIENT_SECRET 4 chars {}", System.getenv("ZEEBE_CLIENT_SECRET").substring(4));
    LOG.info("jobType {}", System.getenv("jobType"));

    try (ZeebeClient client = 
         ZeebeClient.newCloudClientBuilder()
        .withClusterId(System.getenv("ZEEBE_CLUSTER_ID"))
        .withClientId(System.getenv("ZEEBE_CLIENT_ID"))
        .withClientSecret(System.getenv("ZEEBE_CLIENT_SECRET"))
        .withRegion("bru-2")
        .build();
    ) {
      client.newWorker().jobType(System.getenv("jobType")).handler((jobClient, job) -> {

        LOG.info("\n  Worker for task type {} processing job {} \n", job.getType(), job.toJson());

        LOG.info("--- Variables ---");
        for (var entry : job.getVariablesAsMap().entrySet()) LOG.info("{} : {}", entry.getKey(), entry.getValue());
    
        Map<String, Object> output = Map.of("worker-"+job.getType(), System.currentTimeMillis());
        LOG.info("return variables: {}", output);
    
        client.newCompleteCommand(job.getKey()).variables(output).send()
            .whenComplete(
                (response, exception) -> {
                  if (exception == null)
                    LOG.info("Successfully completed job {} with result {}", job.getElementInstanceKey(), response);
                  else
                    LOG.error("Failed to complete job", exception);
                }
            );
      }).open();
      // run until System.in receives exit command
      waitUntilSystemInput("noexit");
    } catch (Exception e) {
      LOG.error(e);
    }
  }

  private static void waitUntilSystemInput(final String exitCode) {
    try (final Scanner scanner = new Scanner(System.in)) {
      while (scanner.hasNextLine()) {
        final String nextLine = scanner.nextLine();
        if (nextLine.contains(exitCode)) {
          return;
        }
      }
    }
  }
}