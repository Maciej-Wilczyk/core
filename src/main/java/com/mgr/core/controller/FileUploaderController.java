package com.mgr.core.controller;

import com.mgr.core.service.FileUploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploaderController {

    private final FileUploaderService fileUploaderService;

    @PostMapping(path = "/dockerfile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> setDockerFile(@RequestPart MultipartFile dockerFile) throws IOException {
        if (!dockerFile.isEmpty()) {
            fileUploaderService.saveDockerfile(dockerFile);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping(path = "/buildDockerfile")
    public ResponseEntity<?> buildDockerfile() {
        fileUploaderService.processDockerfileBuild();
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/buildStatus")
    public ResponseEntity<?> checkBuildStatus() {
        return fileUploaderService.isDockerfileBuilt()
                ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping(path = "/pushDockerfile")
    public ResponseEntity<?> pushDockerfile() throws IOException, InterruptedException {
        fileUploaderService.processPushDockerfile();
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("health!");
    }
}
