package com.github.zavier.domain.customer.gateway;

import com.github.zavier.domain.customer.Credit;

//Assume that the credit info is in another distributed Service
public interface CreditGateway {
    Credit getCredit(String customerId);
}
