package com.yp.selfheal.dc.exception;

public class QuorumNotMetException extends RuntimeException {
    public QuorumNotMetException(String message) {
        super(message);
    }
}
