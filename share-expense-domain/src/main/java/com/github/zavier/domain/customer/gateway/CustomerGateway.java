package com.github.zavier.domain.customer.gateway;

import com.github.zavier.domain.customer.Customer;

public interface CustomerGateway {
    Customer getByById(String customerId);
}
