package com.mgr.core.controller;

import com.mgr.core.pojo.CredentialsAndEc2Ip;
import com.mgr.core.service.EC2InstanceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PreSettingsController {

    private final EC2InstanceManagementService ec2InstanceManagementService;

    @PostMapping(path = "/credentialsAndEc2Ip", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> setCredentialsAndEc2Ip(@RequestBody CredentialsAndEc2Ip credentialsAndEc2Ip) {
        try {
            ProcessBuilder process = new ProcessBuilder();
            process.command("aws", "configure", "set", "aws_access_key_id", credentialsAndEc2Ip
                    .getCredentials()
                    .getAwsAccessKeyId())
                    .start()
                    .waitFor();

            process.command("aws", "configure", "set", "aws_secret_access_key", credentialsAndEc2Ip
                    .getCredentials()
                    .getAwsSecretAccessKey())
                    .start()
                    .waitFor();

            process.command("aws", "configure", "set", "aws_session_token", credentialsAndEc2Ip
                    .getCredentials()
                    .getAwsSessionToken())
                    .start()
                    .waitFor();
            ec2InstanceManagementService.setEc2PublicIp(credentialsAndEc2Ip.getEc2Ip());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok("credentials and ec2 ip set");
    }
    @PostMapping("/dockerStart")
    public ResponseEntity<String> startDockerServer() {
        try {
            ProcessBuilder process = new ProcessBuilder();
            process.command("sudo", "service", "docker", "start")
                    .start()
                    .waitFor();
            process.command("sudo", "usermod", "-a", "-G", "docker", "ec2-user")
                    .start()
                    .waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok("docker starting");
    }

}
