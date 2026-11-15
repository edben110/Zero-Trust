package com.zerotrust.votingapi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, List<String> fields) {

    public static ErrorResponse de(String error) {
        return new ErrorResponse(error, null);
    }
}
