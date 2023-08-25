package com.mgr.core.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import static com.mgr.core.constant.Constant.*;

@Service
@RequiredArgsConstructor
@Getter
public class FileUploaderService {

    private final String DOCKERFILE_PATH = "src/main/resources/";

    private boolean isDockerfileBuilt = false;

    @Async
    public void processDockerfileBuild() throws IOException, InterruptedException {
        isDockerfileBuilt = false;

        ProcessBuilder process = new ProcessBuilder();

        process.command("docker", "build", "-t", TAG, "-f", System.getProperty("user.dir") + "/Dockerfile", ".");
        System.out.println("to jest url " + System.getProperty("user.dir"));

        process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process buildProcess = process.start();

        int buildExitCode = buildProcess.waitFor();
        System.out.println("EXITCODE: " + buildExitCode);

        // Read the output from the build process
        readProcessOutput(buildProcess.getInputStream());

        if (buildExitCode == 0) {
            process.command("docker", "tag", TAG, AWS_ECR_REPOSITORY_WITH_TAG);

            Process tagProcess = process.start();
            int tagExitCode = tagProcess.waitFor();

            // Read the output from the tag process
            readProcessOutput(tagProcess.getInputStream());

            if (tagExitCode == 0) {
                isDockerfileBuilt = true;
            }
        }

    }


    private void readProcessOutput(InputStream inputStream) throws IOException {
        System.out.println("jestem w readerze");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            StringBuilder lineBuiler = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineBuiler.append(line);
                System.out.println(line);
            }
            System.out.println("odp string builder" + lineBuiler.toString());
        }
    }

    public void processPushDockerfile() throws IOException, InterruptedException {
        ProcessBuilder process = new ProcessBuilder();
        String command = "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin " + AWS_ECR_REPOSITORY;
        //process.command("aws", "ecr", "get-login-password", "--region", "us-east-1", "|", "docker", "login", "--username", "AWS", "--password-stdin", AWS_ECR_REPOSITORY);
        process.command("bash", "-c", command);
        process.redirectErrorStream(true); // Redirect error stream to output stream
        Process p = process.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = p.waitFor();
        System.out.println("Exit code: " + exitCode);

        process.command("docker", "push", AWS_ECR_REPOSITORY_WITH_TAG)
                .start()
                .waitFor();
    }

    public void saveDockerfile(MultipartFile dockerfile) throws IOException {
        String currentDirectory = System.getProperty("user.dir");
        String fileName = dockerfile.getOriginalFilename();
        File destinationFile = new File(currentDirectory, fileName);
        dockerfile.transferTo(destinationFile);
    }
}
