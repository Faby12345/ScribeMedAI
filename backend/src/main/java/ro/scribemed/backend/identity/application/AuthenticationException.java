package ro.scribemed.backend.identity.application;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException() {
        super("Invalid email or password");
    }
}
