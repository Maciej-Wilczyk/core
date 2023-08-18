package com.mgr.core.controller;

import com.mgr.core.service.FileUploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploaderController {

    private final FileUploaderService fileUploaderService;

    @PostMapping(path = "/dockerfile", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> setDockerFile(@RequestPart MultipartFile dockerFile) throws IOException {
        if (!dockerFile.isEmpty()) {
            fileUploaderService.saveDockerfile(dockerFile);
            return ResponseEntity.ok("Dockerfile saved!");
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/buildDockerfile")
    public ResponseEntity<String> buildDockerfile() {
        fileUploaderService.processDockerfileBuild();
        return ResponseEntity.ok("Dockerfile start building");
    }

    @GetMapping("/buildStatus")
    public ResponseEntity<Boolean> checkBuildStatus() {
        return ResponseEntity.ok(fileUploaderService.isDockerfileBuilt());
    }

    @PostMapping("/pushDockerfile")
    public ResponseEntity<String> pushDockerfile() throws IOException, InterruptedException {
        fileUploaderService.processPushDockerfile();
        return ResponseEntity.ok("Push succeed!");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("health!");
    }
}
