package com.mgr.core.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.mgr.core.constant.Constant.*;

@Service
@RequiredArgsConstructor
@Getter
public class FileUploaderService {

    private final String DOCKERFILE_PATH = "src/main/resources/";

    final SimpMessagingTemplate simpMessagingTemplate;

    private boolean isDockerfileBuilt = true;

    @Async
    public void processDockerfileBuild() {
//        simpMessagingTemplate.convertAndSend("/topic/progress", "Process Start");
        isDockerfileBuilt = false;
        Optional.ofNullable(ClassLoader.getSystemClassLoader().getResource("Dockerfile")).ifPresent(url -> {
            try {
                ProcessBuilder process = new ProcessBuilder();

                process.command("docker", "build", "-t", TAG, "-f", url.toString(), ".")
                        .start()
                        .waitFor();

                process.command("docker", "tag", TAG, AWS_ECR_REPOSITORY_WITH_TAG)
                        .start()
                        .waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });

        isDockerfileBuilt = true;
//        if (isBuildSuccess) {
//            simpMessagingTemplate.convertAndSend("/topic/build", "Process Successfully End");
//        } else {
//            simpMessagingTemplate.convertAndSend("/topic/build", "Process failed");
//        }
    }

    public void processPushDockerfile() throws IOException, InterruptedException {
        ProcessBuilder process = new ProcessBuilder();
        process.command("aws", "ecr", "get-login-password", "--region", "us-east-1", "|", "docker", "login", "--username", "AWS", "--password-stdin", AWS_ECR_REPOSITORY)
                .start()
                .waitFor();

        process.command("docker", "push", AWS_ECR_REPOSITORY_WITH_TAG)
                .start()
                .waitFor();
    }

    public void saveDockerfile (MultipartFile dockerfile) throws IOException {
        Path path = Paths.get(DOCKERFILE_PATH + dockerfile.getOriginalFilename());
        Files.write(path, dockerfile.getBytes());
    }
}
