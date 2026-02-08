package com.verifico.server.payment.dto;


import com.verifico.server.credit.TransactionType;
import com.verifico.server.payment.CreditsPurchasedAmount;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class PurchaseCreditsRequest {

  @NotNull
  private CreditsPurchasedAmount amount;

  private TransactionType productName = TransactionType.PURCHASE_CREDITS;

  @Min(1)
  private int quantity = 1;

  private String currency = "usd";
}
