package com.jovany.bakery.order.client;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the declarative HTTP clients in this package.
 * Kept out of the main class so @WebMvcTest slices do not try to create the clients.
 */
@Configuration
@EnableFeignClients
public class FeignConfig {
}
