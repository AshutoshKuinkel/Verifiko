package com.verifico.server.payment.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Data
@NoArgsConstructor
public class CheckoutRequest {
  private int amount;
  private Long quantitiy;
  private String name;
  private String currency;
}
