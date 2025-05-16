package com.yourcompany.olmosjtutils;

import com.yourcompany.olmosjtutils.appstartuplog.ApplicationUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OlmosjtUtilsApplication {

  public static void main(String[] args) {
    var application = new SpringApplication(OlmosjtUtilsApplication.class);
    var env = application.run(args).getEnvironment();
    ApplicationUtils.logApplicationStartup(env);
  }

}
