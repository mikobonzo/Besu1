package com.uf.banking.besu_iou.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;

public record IssueIouRequest(
        @NotBlank @Size(max = 200) String externalReference,
        @NotBlank @Pattern(regexp = "^0x[0-9a-fA-F]{40}$") String beneficiary,
        @Positive BigInteger amountMinorUnits,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency) {
}
