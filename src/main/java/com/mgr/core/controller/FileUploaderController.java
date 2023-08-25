package com.mgr.core.controller;

import com.mgr.core.pojo.Data;
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

    @PostMapping(value= "/dockerfile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Data<Boolean>> setDockerFile(@RequestPart MultipartFile dockerfile) throws IOException {
        if (!dockerfile.isEmpty()) {
            fileUploaderService.saveDockerfile(dockerfile);
            return ResponseEntity.ok(new Data<>(Boolean.TRUE));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Data<>(Boolean.FALSE));
    }

    @PostMapping(path = "/buildDockerfile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Data<String>> buildDockerfile() throws IOException, InterruptedException {
        fileUploaderService.processDockerfileBuild();
        return ResponseEntity.ok(new Data<>("Dockerfile start building!"));
    }

    @GetMapping(path = "/buildStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Data<Boolean>> checkBuildStatus() {
        return fileUploaderService.isDockerfileBuilt()
                ? ResponseEntity.ok(new Data<>(Boolean.TRUE)) :
                ResponseEntity.status(HttpStatus.ACCEPTED).body(new Data<>(Boolean.FALSE));
    }

    @PostMapping(path = "/pushDockerfile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Data<String>> pushDockerfile() throws IOException, InterruptedException {
        fileUploaderService.processPushDockerfile();
        return ResponseEntity.ok(new Data<>("Dockerfile pushed!"));
    }

    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("health!");
    }
}
