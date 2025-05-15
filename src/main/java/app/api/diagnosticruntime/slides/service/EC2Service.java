package app.api.diagnosticruntime.slides.service;

import app.api.diagnosticruntime.config.Ec2Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

@Slf4j
@Service
@Transactional
public class EC2Service {

    private final Ec2Client ec2Client;
    private final Ec2Properties ec2Properties;

    public EC2Service(Ec2Properties ec2Properties) {
        this.ec2Properties = ec2Properties;
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(ec2Properties.getAccessKeyId(), ec2Properties.getSecretKey());

        this.ec2Client = Ec2Client.builder()
                .region(Region.of(ec2Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    public void startInstanceIfStopped(String instanceId) {
        // Check the current state of the instance
        String instanceState = getInstanceState(instanceId);
        log.info("Instance state: " + instanceState);

        // Start the instance if it is stopped or hibernating
        if ("stopped".equalsIgnoreCase(instanceState) || "stopping".equalsIgnoreCase(instanceState)) {
            startInstance(instanceId);
        }

    }

    private String getInstanceState(String instanceId) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        DescribeInstancesResponse response = ec2Client.describeInstances(request);
        return response.reservations().get(0).instances().get(0).state().nameAsString();
    }

    private void startInstance(String instanceId) {
        System.out.println("Starting the instance...");
        StartInstancesRequest startRequest = StartInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2Client.startInstances(startRequest);
        waitForInstanceToBeRunning(instanceId);
    }

    private void waitForInstanceToBeRunning(String instanceId) {
        boolean running = false;
        while (!running) {
            String state = getInstanceState(instanceId);
            if ("running".equalsIgnoreCase(state)) {
                running = true;
                log.info("Instance is now running.");
            } else {
                log.info("Waiting for instance to be in the running state...");
                try {
                    Thread.sleep(5000); // Poll every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for instance to start.");
                }
            }
        }
    }

    public void hibernateInstance(String instanceId) {
        log.info("Hibernating the instance...");
        StopInstancesRequest stopRequest = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        ec2Client.stopInstances(stopRequest);
        waitForInstanceToBeStopped(instanceId);
    }

    private void waitForInstanceToBeStopped(String instanceId) {
        boolean stopped = false;
        while (!stopped) {
            String state = getInstanceState(instanceId);
            if ("stopped".equalsIgnoreCase(state)) {
                stopped = true;
                System.out.println("Instance is now in the stopped state.");
            } else {
                System.out.println("Waiting for instance to enter the stopped state...");
                try {
                    Thread.sleep(5000); // Poll every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for instance to stop.");
                }
            }
        }
    }

    public boolean isInstanceRunning(String instanceId) {
        try {
            String state = getInstanceState(instanceId);
            log.info("Checked instance state: {}", state);
            return "running".equalsIgnoreCase(state);
        } catch (Exception e) {
            log.error("Failed to check EC2 instance state for instanceId: {}", instanceId, e);
            return false;
        }
    }


}
