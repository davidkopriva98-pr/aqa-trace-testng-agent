package com.aqanetics.agent.core.exception;

import java.io.IOException;

public class AqaAgentException extends IOException {

  private final boolean ignoreException;

  public AqaAgentException(String message, boolean ignoreException) {
    super(message);
    this.ignoreException = ignoreException;
  }

  public boolean shouldThrowException() {
    return !ignoreException;
  }
}
