package com.verifico.server.post;

public enum Category {

  AI("Artificial Intelligence"),
  SAAS("Software as a Service"),
  FINTECH("Financial Technology"),
  HEALTHTECH("Healthcare Technology"),
  EDTECH("Education Technology"),
  ECOMMERCE("E-commerce"),
  PRODUCTIVITY("Productivity & Workflow"),
  SOCIAL("Social & Community"),
  DEVELOPER_TOOLS("Developer Tools"),
  MARTECH("Marketing Technology"),
  CYBERSECURITY("Cybersecurity"),
  DATA_ANALYTICS("Data & Analytics"),
  MARKETPLACE("Online Marketplace"),
  CREATOR_ECONOMY("Creator Economy"),
  AI_INFRASTRUCTURE("AI Infrastructure"),
  AI_AUTOMATION("AI Automation"),
  OTHER("Other");

  private final String displayname;

  Category(String displayName) {
    this.displayname = displayName;
  }

  public String getDisplayName() {
    return displayname;
  }

  public static Category findByCategoryName(String name) {
    Category result = null;
    for (Category category : values()) {
      if (category.name().equalsIgnoreCase(name)) {
        result = category;
        break;
      }
    }
    return result;
  }
}