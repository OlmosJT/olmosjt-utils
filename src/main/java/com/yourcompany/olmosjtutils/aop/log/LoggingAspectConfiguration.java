package com.yourcompany.olmosjtutils.aop.log;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

  /**
   * Creates the LoggingAspect bean.
   *
   * @param env The Spring Environment, used for profile-specific logging.
   * @return The LoggingAspect instance.
   */
  @Bean
  public LoggingAspect loggingAspect(Environment env) {
    return new LoggingAspect(env);
  }

  /**
   * Aspect for logging method executions, including entry, exit, execution time,
   * success status, and detailed exception information.
   */
  @Aspect
  @RequiredArgsConstructor
  public static class LoggingAspect {
      private final Environment env;

    /**
    * ThreadLocal to track the depth of AOP-advised calls for the current thread.
    * Used to identify the "outermost" AOP call to clean up other ThreadLocals.
    */
    private static final ThreadLocal<Integer> AOP_CALL_DEPTH = ThreadLocal.withInitial(() -> 0);
    /**
    * ThreadLocal to store the set of exceptions for which a full stack trace
    * has already been logged in the "prod" profile during the current request.
     */
    private static final ThreadLocal<Set<Throwable>> ALREADY_LOGGED_FULL_IN_PROD = ThreadLocal.withInitial(HashSet::new);


    /**
     * Pointcut that matches all methods within classes annotated with
     * {@link org.springframework.web.bind.annotation.RestController},
     * {@link org.springframework.stereotype.Service},
     * or {@link org.springframework.stereotype.Component}.
     * Easily extensible by adding more annotations or package patterns.
     */
    @Pointcut(
            "within(uz.tengebank.wooppay..*) && " +
            "within(@org.springframework.web.bind.annotation.RestController *) || " +
            "within(@org.springframework.stereotype.Service *) || " +
            "within(@org.springframework.stereotype.Repository *) && " +
//            "within(@org.springframework.stereotype.Component *)) && " +
            "!within(uz.tengebank.wooppay.aop.LoggingAspectConfiguration.LoggingAspect) && " +
            "!within(org.springdoc..*)"
    )
    public void applicationLayerPointcut() {
      // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    /**
     * Retrieves the {@link Logger} associated with the class of the given {@link JoinPoint}.
     *
     * @param joinPoint The join point for which to get the logger.
     * @return The {@link Logger} instance.
     */
    private Logger logger(JoinPoint joinPoint) {
      return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    /**
     * Advice that logs method entry, exit, execution time, and success status.
     *
     * @param joinPoint The join point for the advised method.
     * @return The result of the method execution.
     * @throws Throwable if the advised method throws an exception.
     */
    @Around("applicationLayerPointcut()")
    public Object logAroundExecution(ProceedingJoinPoint joinPoint) throws Throwable {
      AOP_CALL_DEPTH.set(AOP_CALL_DEPTH.get() + 1);

      Logger log = logger(joinPoint);
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      String className = signature.getDeclaringType().getSimpleName();
      String methodName = signature.getName();


      // Log user context for Basic Authentication
      String userContextLog;
      try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
          Object principal = authentication.getPrincipal();
          String username;
          if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
          } else {
            username = principal.toString();
          }
          userContextLog = String.format("User: %s", username);
        } else {
          userContextLog = "User: anonymous";
        }
      } catch (Exception e) {
        userContextLog = "User: N/A (Error retrieving user context)";
        if (log.isTraceEnabled()) {
          log.trace("Could not retrieve user context for logging in {}.{}: {}", className, methodName, e.getMessage());
        }
      }


      log.info("➡ ENTER: {}.{}() | {}", className, methodName, userContextLog);

      if (log.isDebugEnabled()) {
        log.debug("\tArguments: {}", Arrays.toString(joinPoint.getArgs()));
      }

      final StopWatch stopWatch = new StopWatch();
      Object result = null;
      boolean success = false;

      try {
        stopWatch.start();
        result = joinPoint.proceed();
        success = true;
      } finally {
        int currentDepth = AOP_CALL_DEPTH.get() - 1;
        AOP_CALL_DEPTH.set(currentDepth);

        if (currentDepth == 0) {
          // This is the "outermost" AOP call for this thread returning, clean up request-scoped ThreadLocals
          ALREADY_LOGGED_FULL_IN_PROD.remove();
        }


        if(stopWatch.isRunning()) {
          stopWatch.stop();
        }
        String statusMessage = success ? "Success: true" : "Success: false (Check ERROR logs for details)";
        log.info("⬅ EXIT: {}.{}() | Execution Time: {} ms | {}",
                className, methodName, stopWatch.getTotalTimeMillis(), statusMessage);

        if (success && log.isDebugEnabled()) {
          log.debug("\tResult: {}", result);
        }
      }
      return result;
    }

    /**
     * Advice that logs exceptions thrown by methods matched by the pointcut.
     *
     * @param joinPoint The join point where the exception occurred.
     * @param ex        The thrown exception.
     */
    @AfterThrowing(pointcut = "applicationLayerPointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
      Logger log = logger(joinPoint);
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      String className = signature.getDeclaringType().getSimpleName();
      String methodName = signature.getName();

      boolean logFullStackTrace = false;
      Set<Throwable> loggedExceptionsSet = ALREADY_LOGGED_FULL_IN_PROD.get();
      if (!loggedExceptionsSet.contains(ex)) {
        logFullStackTrace = true;
        loggedExceptionsSet.add(ex);
      }

      String causeMessage = (ex.getCause() != null) ? ex.getCause().toString() : "N/A";
      if (logFullStackTrace) {
        log.error("❌ EXCEPTION in {}.{}() | Cause: '{}' | Message: '{}'",
                className, methodName, causeMessage, ex.getMessage(), ex);
      } else {
        log.error("❌ EXCEPTION in {}.{}() | Message: '{}' (propagated - full trace logged at origin)",
                className, methodName, ex.getMessage());
      }

      try {
        Span span = TraceUtil.current();
        if (span != null && span.isRecording()) {
          span.setStatus(StatusCode.ERROR, ex.getMessage() != null ? ex.getMessage() : "Unknown error");
          span.recordException(ex);
        }
      } catch (Exception e) {
        log.warn("Failed to update trace span for exception in {}.{}: {}", className, methodName, e.getMessage());
      }
    }

  }


}
