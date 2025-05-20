package com.yourcompany.olmosjtutils.aop.log;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TraceUtil {
  public static String getTraceIdSafely() {
    Span span = Span.fromContext(Context.current());
    // Check if the span is valid
    if (span.getSpanContext().isValid()) {
      return span.getSpanContext().getTraceId();
    } else {
      return "No active trace ID";
    }
  }

  public static Span current(){
    return Span.fromContext(Context.current());
  }

}
