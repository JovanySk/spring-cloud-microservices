package com.jovany.bakery.order;

/** A service we depend on (catalog or inventory) is down, timing out or answering 5xx. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is not available right now, please try again later", cause);
    }
}
