package com.mgr.core.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CredentialsAndEc2Ip {
    private Credentials credentials;
    @JsonProperty("ec2_ip")
    private String ec2Ip;
}
