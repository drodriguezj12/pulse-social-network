package com.pulse.auth.common;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        // Same message for unknown user and wrong password: avoids user enumeration.
        super("Invalid username or password");
    }
}
