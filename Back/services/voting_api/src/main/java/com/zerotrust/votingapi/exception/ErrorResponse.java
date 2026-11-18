package com.zerotrust.votingapi.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, List<String> fields, String message) {

    public ErrorResponse(String error, List<String> fields) {
        this(error, fields, null);
    }

    public static ErrorResponse de(String error) {
        return new ErrorResponse(error, null, null);
    }

    public static ErrorResponse con(String error, String message) {
        return new ErrorResponse(error, null, message);
    }
}
