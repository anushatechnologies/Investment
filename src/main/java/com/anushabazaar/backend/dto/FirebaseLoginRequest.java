package com.anushabazaar.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirebaseLoginRequest {

    @JsonAlias({"id_token", "token", "firebaseToken", "firebase_token", "idToken", "accessToken", "otp", "code"})
    private String idToken;

    @JsonAlias({"mobile", "mobileNumber", "phone", "phoneNumber", "recipient", "identifier"})
    private String mobileNumber;

    public FirebaseLoginRequest() {
    }

    public FirebaseLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public FirebaseLoginRequest(String idToken, String mobileNumber) {
        this.idToken = idToken;
        this.mobileNumber = mobileNumber;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
