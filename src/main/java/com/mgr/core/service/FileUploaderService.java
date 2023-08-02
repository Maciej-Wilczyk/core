package com.mgr.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileUploaderService {

    private final String tag = "testowy1:tag";

    private final String AWS_ECR_REPOSITORY = "445146526163.dkr.ecr.us-east-1.amazonaws.com";

    private final String DOCKERFILE_PATH = "src/main/resources/";

    final SimpMessagingTemplate simpMessagingTemplate;

    public void processDockerfileBuild() {
        simpMessagingTemplate.convertAndSend("/topic/progress", "Process Start");
        boolean isBuildSuccess = Optional.ofNullable(ClassLoader.getSystemClassLoader().getResource("Dockerfile")).map(url -> {
            try {
                ProcessBuilder process = new ProcessBuilder();

                process.command("docker", "build", "-t", tag, "-f", url.toString(), ".")
                        .start()
                        .waitFor();

                process.command("docker", "tag", tag, AWS_ECR_REPOSITORY + tag)
                        .start()
                        .waitFor();
                return true;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                return false;
            }
        }).orElse(false);

        if (isBuildSuccess) {
            simpMessagingTemplate.convertAndSend("/topic/build", "Process Successfully End");
        } else {
            simpMessagingTemplate.convertAndSend("/topic/build", "Process failed");
        }
    }

    public void processPushDockerfile() throws IOException, InterruptedException {
        ProcessBuilder process = new ProcessBuilder();
        process.command("aws", "ecr", "get-login-password", "--region", "us-east-1", "|", "docker", "login", "--username", "AWS", "--password-stdin", AWS_ECR_REPOSITORY)
                .start()
                .waitFor();

        process.command("docker", "push", AWS_ECR_REPOSITORY + tag)
                .start()
                .waitFor();
    }

    public void saveDockerfile (MultipartFile dockerfile) throws IOException {
        Path path = Paths.get(DOCKERFILE_PATH + dockerfile.getOriginalFilename());
        Files.write(path, dockerfile.getBytes());
    }
}
