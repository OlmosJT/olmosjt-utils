package com.yourcompany.olmosjtutils;

import com.yourcompany.olmosjtutils.appstartuplog.ApplicationUtils;
import com.yourcompany.olmosjtutils.i18.LocalizedMessageUtils;
import com.yourcompany.olmosjtutils.masking.MaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@RestController
public class OlmosjtUtilsApplication {

  private final LocalizedMessageUtils localizedMessageUtils;

  public static void main(String[] args) {
    var application = new SpringApplication(OlmosjtUtilsApplication.class);
    var env = application.run(args).getEnvironment();
    ApplicationUtils.logApplicationStartup(env);
  }

  @GetMapping()
  public String getMessage() {
    return localizedMessageUtils.getMessageForLocale("welcome.user", Locale.of("ru"), "olmos.jt", "face", "bitch");
  }

}
