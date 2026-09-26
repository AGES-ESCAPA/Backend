/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.escapa.backend.adapters.dto.certificate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StudentSummaryDTO {
    private String name;
    private String avatarUrl;
    private boolean verified;

    public StudentSummaryDTO(String name, String avatarUrl, boolean verified) {
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.verified = verified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

@JsonProperty ("isVerified")
    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
