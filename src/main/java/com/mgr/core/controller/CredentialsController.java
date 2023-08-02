package com.mgr.core.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mgr.core.pojo.Credentials;

import java.io.IOException;

@RestController
@RequestMapping("/credentials")
public class CredentialsController {


    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> setCredentials(@RequestBody Credentials credentials) {
        try {
            ProcessBuilder process = new ProcessBuilder();

            process.command("aws", "configure", "set", "aws_access_key_id", credentials.getAwsAccessKeyId())
                    .start()
                    .waitFor();

            process.command("aws", "configure", "set", "aws_secret_access_key", credentials.getAwsSecretAccessKey())
                    .start()
                    .waitFor();

            process.command("aws", "configure", "set", "aws_session_token", credentials.getAwsSessionToken())
                    .start()
                    .waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok("credentials set");
    }
}
