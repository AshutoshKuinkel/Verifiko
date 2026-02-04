package com.verifico.server.credit;

public enum TransactionType {
  COMMENT_MARKED_HELPFUL, // + 5 credits
  CREATE_POST, // - 20 credits
  BOOST_POST, // - 50 credits
  PURCHASE_CREDITS // + however many they purchase

  // MANUAL_ADJUSTMENTS (i won't include this manual
  // adjustments for now, but it could be needed in
  // the future
  // )
}
