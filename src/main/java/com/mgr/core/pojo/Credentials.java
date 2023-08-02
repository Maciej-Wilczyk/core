package com.mgr.core.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Credentials {

    @JsonProperty("aws_access_key_id")
    private String awsAccessKeyId;

    @JsonProperty("aws_secret_access_key")
    private String awsSecretAccessKey;

    @JsonProperty("aws_session_token")
    private String awsSessionToken;
}
