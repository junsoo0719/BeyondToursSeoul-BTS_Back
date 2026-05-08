package com.beyondtoursseoul.bts.exception;

public class TourApiRateLimitException extends RuntimeException {
    public TourApiRateLimitException() {
        super("TourAPI 일일 호출 한도 초과");
    }
}
