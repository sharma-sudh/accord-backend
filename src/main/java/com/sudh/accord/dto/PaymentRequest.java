package com.sudh.accord.dto;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount, String merchantName) {}