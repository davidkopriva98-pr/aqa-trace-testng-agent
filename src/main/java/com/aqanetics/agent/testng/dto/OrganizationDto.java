package com.aqanetics.agent.testng.dto;

public record OrganizationDto(String name) {

  @Override
  public String toString() {
    return "OrganizationDto(name=" + name + ")";
  }
}
