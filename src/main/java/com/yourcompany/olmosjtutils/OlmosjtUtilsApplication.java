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
public class OlmosjtUtilsApplication implements ApplicationRunner {

  public static void main(String[] args) {
    var application = new SpringApplication(OlmosjtUtilsApplication.class);
    var env = application.run(args).getEnvironment();
    ApplicationUtils.logApplicationStartup(env);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
      log.info(new DTO(
              "51702026bnmbnm670039",
              "olmos@jt20@gmail.com",
              "+998 (93) 008-24-17",
              "AB",
              "9319901",
              new BigDecimal("3900000"),
              "Internal Note"
      ).toString());
  }

  record DTO(
          String userPinfl,
          String userEmail,
          String UserPhone,
          String UsrPassportSeries,
          String UsrPassportNumber,
          BigDecimal amount,
          String internalNote
  ) {
    @Override
    public String toString() {
      return """
          DTO[
           userPinfl: %s,
           userEmail: %s,
           UserPhone: %s,
           UsrPassportSeries: %s,
           UsrPassportNumber: %s,
           Amount: %s,
           internalNote: %s
          ]
          """.formatted(
              MaskingUtils.maskPinfl(userPinfl),
              MaskingUtils.maskEmail(userEmail),
              MaskingUtils.maskPhoneNumber(UserPhone),
              MaskingUtils.maskPassportSeries(UsrPassportSeries),
              MaskingUtils.maskPassportNumber(UsrPassportNumber),
              MaskingUtils.maskAmountWithCurrency(amount, "UZS"),
              this.internalNote
      );

    }
  }
}
