package com.aqanetics.agent.core.dto;

public record OrganizationDto(String name) {

  @Override
  public String toString() {
    return "OrganizationDto(name=" + name + ")";
  }
}
