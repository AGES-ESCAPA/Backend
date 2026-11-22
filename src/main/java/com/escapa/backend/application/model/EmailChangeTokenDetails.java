package com.escapa.backend.application.model;

public record EmailChangeTokenDetails(String name, String currentEmail, String newEmail) {
}
