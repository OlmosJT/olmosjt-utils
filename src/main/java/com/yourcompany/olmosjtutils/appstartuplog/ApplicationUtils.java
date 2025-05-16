package com.yourcompany.olmosjtutils.appstartuplog;

import io.micrometer.common.util.StringUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * <h3>How to Use:</h3>
 * </br>
 * <strong>Usage 1:</strong>
 * </br>
 * <pre><code class="language-java">
 * &#64;Component
 * public class ApplicationStartupLogger {
 *
 *     private final Environment environment;
 *
 *     public ApplicationStartupLogger(Environment environment) {
 *         this.environment = environment;
 *     }
 *
 *     &#64;EventListener(ApplicationReadyEvent.class)
 *     public void logStartup() {
 *         ApplicationUtils.logApplicationStartup(environment);
 *     }
 * }
 * </code></pre>
 *
 * <strong>Usage 2:</strong>
 * <pre><code class="language-java">
 *
 * &#64;SpringBootApplication
 * public class SpringBootApplication {
 *
 *   public static void main(String[] args) {
 *     var application = new SpringApplication(SpringBootApplication.class);
 *     var env = application.run(args).getEnvironment();
 *     ApplicationUtils.logApplicationStartup(env);
 *   }
 *
 * }
 * </code></pre>
 *
 * <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.23.0/themes/prism.min.css" rel="stylesheet" />
 * <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.23.0/prism.min.js"></script>
 */

@Slf4j
@UtilityClass
public class ApplicationUtils {

    public static void logApplicationStartup(Environment env) {
        String appName = env.getProperty("spring.application.name", "UndefinedApplicationName");

        // Attempt to get app version, common if BuildProperties or similar is used
        String appVersion = env.getProperty("info.app.version");
        if (StringUtils.isBlank(appVersion)) {
            appVersion = env.getProperty("application.version", "N/A");
        }

        String protocol = getProtocol(env);
        String serverPort = env.getProperty("server.port", "8080");
        String displayableContextPath = getDisplayContextPath(env);
        String hostAddress = getHostAddress();

        // Prepare log message
        StringBuilder sb = new StringBuilder("\n\n------------------------------------------------------------------------\n");
        sb.append(String.format(" Application: '%s' (Version: %s)\n", appName, appVersion));
        sb.append(" Is Running!\n");
        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Access URLs:\n");
        sb.append(String.format("   Local:      %s://localhost:%s%s\n", protocol, serverPort, displayableContextPath));
        sb.append(String.format("   External:   %s://%s:%s%s\n", protocol, hostAddress, serverPort, displayableContextPath));

        // Swagger UI
        // springdoc.swagger-ui.enabled defaults to true
        String swaggerUiEnabledProperty = env.getProperty("springdoc.swagger-ui.enabled", "true");
        boolean isSwaggerUiEnabled = Boolean.parseBoolean(swaggerUiEnabledProperty);

        if (isSwaggerUiEnabled) {
            String swaggerUiPathSegment = env.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
            if (!swaggerUiPathSegment.startsWith("/")) {
                swaggerUiPathSegment = "/" + swaggerUiPathSegment;
            }

            String swaggerFullUrl;
            String baseUrl = String.format("%s://localhost:%s", protocol, serverPort);
            if ("/".equals(displayableContextPath)) {
                swaggerFullUrl = baseUrl + swaggerUiPathSegment;
            } else {
                swaggerFullUrl = baseUrl + displayableContextPath + swaggerUiPathSegment;
            }
            sb.append(String.format("   Swagger UI: %s\n", swaggerFullUrl));
        }

        sb.append("------------------------------------------------------------------------\n");
        sb.append(" Environment:\n");
        String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length == 0) {
            activeProfiles = env.getDefaultProfiles();
        }
        sb.append(String.format("   Active Profiles: %s\n", String.join(", ", activeProfiles)));
        sb.append(String.format("   Default Charset: %s\n", Charset.defaultCharset()));
        sb.append(String.format("   File Encoding:   %s\n", Charset.defaultCharset().displayName()));

        String springAppJson = env.getProperty("SPRING_APPLICATION_JSON");
        if (StringUtils.isNotBlank(springAppJson)) {
            sb.append(String.format("   SPRING_APPLICATION_JSON: %s\n", springAppJson));
        }

        // Command-Line Arguments (interpreted as Spring properties)
        boolean commandLineArgsHeaderPrinted = false;
        if (env instanceof ConfigurableEnvironment configurableEnv) {
          for (PropertySource<?> ps : configurableEnv.getPropertySources()) { // Iterate over property sources
                if (ps instanceof CommandLinePropertySource && ps.getName().contains("commandLineArgs")) {
                    Object source = ps.getSource();
                    if (source instanceof String[] sourceArgs) {
                      if (sourceArgs.length > 0) {
                            if (!commandLineArgsHeaderPrinted) {
                                sb.append("   Command-Line Arguments:\n");
                                commandLineArgsHeaderPrinted = true;
                            }
                            sb.append(String.format("     Raw: %s\n", Arrays.toString(sourceArgs)));
                        }
                    }
                }
            }
        } else {
            log.debug("Cannot access property sources for command line arguments as Environment is not a ConfigurableEnvironment.");
        }

        sb.append("------------------------------------------------------------------------\n");
        sb.append(" JVM:\n");
        sb.append(String.format("   Version:  %s (Vendor: %s)\n",
                System.getProperty("java.version"),
                System.getProperty("java.vm.vendor")));
        sb.append(String.format("   Runtime:  %s (Version: %s)\n",
                System.getProperty("java.runtime.name"),
                System.getProperty("java.runtime.version")));

        try {
            long pid = ProcessHandle.current().pid();
            sb.append(String.format("   PID:      %d\n", pid));
        } catch (UnsupportedOperationException | SecurityException e) {
            log.debug("Cannot retrieve PID: {}", e.getMessage());
            sb.append("   PID:      N/A (Requires Java 9+ and permissions)\n");
        }

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        sb.append(String.format("   VM Args:  %s\n", String.join(" ", runtimeMXBean.getInputArguments())));

        NumberFormat numberFormat = NumberFormat.getInstance();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long allocatedMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        sb.append(String.format("   Max Memory:      %s MB\n", numberFormat.format(maxMemory == Long.MAX_VALUE ? -1 : maxMemory / (1024 * 1024))));
        sb.append(String.format("   Allocated Memory: %s MB\n", numberFormat.format(allocatedMemory / (1024 * 1024))));
        sb.append(String.format("   Free Memory:     %s MB\n", numberFormat.format(freeMemory / (1024 * 1024))));

        sb.append("------------------------------------------------------------------------\n");
        sb.append(" OS:\n");
        sb.append(String.format("   Name:    %s (%s)\n", System.getProperty("os.name"), System.getProperty("os.version")));
        sb.append(String.format("   Arch:    %s\n", System.getProperty("os.arch")));
        sb.append("------------------------------------------------------------------------\n\n");

        log.info(sb.toString());
    }

    private static String getProtocol(Environment env) {
        if (StringUtils.isNotBlank(env.getProperty("server.ssl.key-store")) ||
                Boolean.parseBoolean(env.getProperty("server.ssl.enabled", "false"))) {
            return "https";
        }
        return "http";
    }

    private static String getDisplayContextPath(Environment env) {
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath) || "/".equals(contextPath)) {
            return "/";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    private static String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback.");
            return "localhost";
        }
    }
}
