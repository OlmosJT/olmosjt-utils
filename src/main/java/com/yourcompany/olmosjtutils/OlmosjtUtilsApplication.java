package com.yourcompany.olmosjtutils;

import com.yourcompany.olmosjtutils.appstartuplog.ApplicationUtils;
import com.yourcompany.olmosjtutils.masking.MaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.util.Currency;

@Slf4j
@SpringBootApplication
public class OlmosjtUtilsApplication {

  public static void main(String[] args) {
    var application = new SpringApplication(OlmosjtUtilsApplication.class);
    var env = application.run(args).getEnvironment();
    ApplicationUtils.logApplicationStartup(env);
  }


}
