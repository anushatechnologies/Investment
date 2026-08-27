package com.anushabazaar.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class FirebaseLoginRequest {

    @JsonAlias({"id_token", "token", "firebaseToken"})
    private String idToken;

    public FirebaseLoginRequest() {
    }

    public FirebaseLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
