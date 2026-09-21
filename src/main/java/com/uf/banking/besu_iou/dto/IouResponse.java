package com.uf.banking.besu_iou.dto;

import java.math.BigInteger;

public record IouResponse(String id, String beneficiary, BigInteger amountMinorUnits,
        String currency, String status) {
}
