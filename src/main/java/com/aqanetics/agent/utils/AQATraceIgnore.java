package com.aqanetics.agent.utils;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a specific test or configuration method should be ignored or skipped from being
 * registered in the AQA Trace reporting software.
 *
 * <p>When applied to a method (either a test method annotated with {@code @Test} or a configuration
 * method like {@code @BeforeMethod}, {@code @AfterMethod}, etc.), it specifically indicates that
 * this method should be ignored for AQA Trace reporting.
 *
 * <p>The presence of this annotation, with its default value of {@code true}, is sufficient to
 * signal that the method should be ignored.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({METHOD})
@Documented
public @interface AQATraceIgnore {

  /**
   * Indicates whether the annotated method should be ignored for AQA Trace reporting. The default
   * value is {@code true}.
   *
   * @return {@code true} if the method should be ignored, {@code false} otherwise.
   */
  boolean value() default true;
}
