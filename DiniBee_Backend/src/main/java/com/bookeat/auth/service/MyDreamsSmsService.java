package com.bookeat.auth.service;

import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service @Slf4j
public class MyDreamsSmsService {
  @Value("${mydreams.otp.enabled:false}") boolean enabled;
  @Value("${mydreams.otp.fail-silently:true}") boolean failSilently;
  @Value("${mydreams.otp.api-key:}") String apiKey;
  @Value("${mydreams.otp.template-id:1777179309773200049}") String templateId;
  @Value("${mydreams.otp.url-template:}") String urlTemplate;
  @Value("${mydreams.otp.message-template:Your OTP for login to DiniBee account is {otp} - DEALIT TECHNOLOGIES. https://vm.ltd/DEATEC/tk0Ksa}") String messageTemplate;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public void sendOtp(String mobile, String otp) {
    log.info("==================================================");
    log.info("DiniBee MOBILE OTP FOR [{}]: [{}]", mobile, otp);
    log.info("==================================================");
    if (!enabled) {
      log.info("MyDreams SMS is disabled. Use the console OTP [{}] for mobile [{}] to log in.", otp, mobile);
      return;
    }
    if (apiKey.isBlank()) {
      log.warn("MyDreams OTP is not fully configured. Set MYDREAMS_API_KEY.");
      if (!failSilently) {
        throw new ApiException(ErrorCode.OTP_PROVIDER_UNAVAILABLE);
      }
      return;
    }
    String message = messageTemplate
        .replace("{otp}", otp)
        .replace("{#num#}", otp)
        .replace("{#var#}", otp);
    String url = (urlTemplate != null ? urlTemplate.trim() : "");
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      String resolvedTid = (!url.isBlank() && url.matches("\\d+")) ? url : templateId;
      url = "http://app.mydreamstechnology.in/vb/apikey.php?apikey={apiKey}&senderid=DEATEC&number={mobile}&message={message}&templateid=" + resolvedTid;
    }
    url = url.replace("{apiKey}", enc(apiKey))
             .replace("{apikey}", enc(apiKey))
             .replace("{authkey}", enc(apiKey))
             .replace("{" + apiKey + "}", enc(apiKey))
             .replace("{senderId}", "DEATEC")
             .replace("{senderid}", "DEATEC")
             .replace("{mobile}", enc(mobile))
             .replace("{mobiles}", enc(mobile))
             .replace("{otp}", enc(otp))
             .replace("{templateId}", enc(templateId))
             .replace("{templateid}", enc(templateId))
             .replace("{message}", enc(message))
             .replace("{routeId}", "4");
    url = url.replace(" ", "+");
    try {
      log.info("Sending OTP via MyDreams URL: {}", url.replaceAll("(?i)(authkey|key)=[^&]+", "$1=REDACTED"));
      HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
      log.info("MyDreams SMS response: status={}, body={}", r.statusCode(), r.body());
      if (r.statusCode() < 200 || r.statusCode() >= 300) {
        log.error("MyDreams SMS rejected status={}, body={}", r.statusCode(), r.body());
        if (!failSilently) {
          throw new ApiException(ErrorCode.OTP_PROVIDER_UNAVAILABLE);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      if (!failSilently) {
        throw new ApiException(ErrorCode.OTP_PROVIDER_UNAVAILABLE);
      }
    } catch (Exception e) {
      log.error("MyDreams SMS delivery failed: {}. [FALLBACK ACTIVE]: Console OTP for {} is [{}]", e.getMessage(), mobile, otp);
      if (!failSilently) {
        throw new ApiException(ErrorCode.OTP_PROVIDER_UNAVAILABLE);
      }
    }
  }
  private String enc(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}
