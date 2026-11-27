package com.bookeat;

import com.bookeat.payment.gateway.*;
import com.bookeat.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Provider contract fixtures only; no Zoho account or network is contacted. */
class ZohoGatewayTest {
    final ObjectMapper json=new ObjectMapper();
    final ZohoTransport http=mock(ZohoTransport.class);
    final ZohoAccessToken oauth=mock(ZohoAccessToken.class);
    ZohoPaymentsGateway gateway(){when(oauth.configured()).thenReturn(true);when(oauth.get()).thenReturn("test-oauth-token");return new ZohoPaymentsGateway(true,true,"23137556","widget-public","widget-signing","webhook-signing",oauth,http,json,Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"),ZoneOffset.UTC));}
    ZohoTransport.Response response(String body){return new ZohoTransport.Response(200,body.getBytes(StandardCharsets.UTF_8));}
    String payment(){return """
        {"code":0,"payment":{"payment_id":"2001","payments_session_id":"1001","amount":"10.50",
        "currency":"INR","status":"succeeded","amount_captured":"10.50","amount_refunded":"0.00",
        "fee_amount":"0.20","net_tax_amount":"0.04","total_fee_amount":"0.24","payment_method":{"type":"upi"}}}
        """;}
    @Test void protocolVectors(){ZohoProtocolChecks.main(new String[0]);}
    @Test void sessionCreationSendsMajorUnitsAndServerMetadata()throws Exception{
        var gateway=gateway();UUID id=UUID.randomUUID();
        when(http.send(eq("POST"),any(),anyMap(),any())).thenAnswer(call->{
            assertThat(call.getArgument(1).toString()).isEqualTo("https://paymentssandbox.zoho.in/api/v1/paymentsessions?account_id=23137556");
            Map<String,String> headers=call.getArgument(2);assertThat(headers.get("Authorization")).isEqualTo("Zoho-oauthtoken test-oauth-token");
            var body=json.readTree((byte[])call.getArgument(3));assertThat(body.path("amount").decimalValue()).isEqualByComparingTo("10.50");
            assertThat(body.path("meta_data").get(0).path("value").asText()).isEqualTo(id.toString());
            return response("{\"code\":0,\"payments_session\":{\"payments_session_id\":\"1001\",\"amount\":\"10.50\",\"currency\":\"INR\",\"reference_number\":\""+id+"\",\"meta_data\":[{\"key\":\"bookeat_payment_id\",\"value\":\""+id+"\"}]}}");
        });
        var result=gateway.create(id,new BigDecimal("10.50"));assertThat(result.amount()).isEqualTo(1050);assertThat(result.receipt()).isEqualTo(id.toString());
    }
    @Test void captureUsesActualZohoFieldsAndFeeTax(){var gateway=gateway();when(http.send(eq("GET"),any(),anyMap(),isNull())).thenReturn(response(payment()));var result=gateway.fetchPayment("2001");assertThat(result.status()).isEqualTo("captured");assertThat(result.amount()).isEqualTo(1050);assertThat(result.fee()).isEqualTo(24);assertThat(result.tax()).isEqualTo(4);assertThat(result.orderId()).isEqualTo("1001");}
    @Test void invalidCaptureAmountAndMissingFeesFailClosed(){var gateway=gateway();when(http.send(eq("GET"),any(),anyMap(),isNull())).thenReturn(response(payment().replace("\"amount_captured\":\"10.50\"","\"amount_captured\":\"1.00\"")));assertThatThrownBy(()->gateway.fetchPayment("2001")).isInstanceOf(ApiException.class);when(http.send(eq("GET"),any(),anyMap(),isNull())).thenReturn(response(payment().replace("\"fee_amount\":\"0.20\",","")));assertThatThrownBy(()->gateway.fetchPayment("2001")).isInstanceOf(ApiException.class);}
    @Test void paymentIdMismatchAndUnsafeIdAreRejected(){var gateway=gateway();when(http.send(eq("GET"),any(),anyMap(),isNull())).thenReturn(response(payment()));assertThatThrownBy(()->gateway.fetchPayment("2002")).isInstanceOf(ApiException.class);assertThatThrownBy(()->gateway.fetchPayment("2001/../../refunds")).isInstanceOf(ApiException.class);verify(http,times(1)).send(anyString(),any(),anyMap(),isNull());}
    @Test void refundUsesPluralRouteAndMetadataCorrelation(){var gateway=gateway();UUID refund=UUID.randomUUID();when(http.send(eq("POST"),any(),anyMap(),any())).thenAnswer(call->{assertThat(call.getArgument(1).toString()).contains("/payments/2001/refunds?account_id=");var body=json.readTree((byte[])call.getArgument(3));assertThat(body.path("amount").decimalValue()).isEqualByComparingTo("5.25");assertThat(body.path("meta_data").get(0).path("value").asText()).isEqualTo(refund.toString());return response("{\"code\":0,\"refund\":{\"refund_id\":\"3001\",\"payment_id\":\"2001\",\"amount\":\"5.25\",\"status\":\"succeeded\",\"reference_number\":\"provider-ref\",\"meta_data\":[{\"key\":\"bookeat_refund_id\",\"value\":\""+refund+"\"}]}}");});var result=gateway.refund("2001",refund,new BigDecimal("5.25"));assertThat(result.receipt()).isEqualTo(refund.toString());assertThat(result.status()).isEqualTo("processed");assertThat(result.amount()).isEqualTo(525);}
    @Test void uncertainPostNeverAutomaticallyRepeats(){var gateway=gateway();when(http.send(eq("POST"),any(),anyMap(),any())).thenReturn(new ZohoTransport.Response(401,"{}".getBytes(StandardCharsets.UTF_8)));assertThatThrownBy(()->gateway.create(UUID.randomUUID(),BigDecimal.TEN)).isInstanceOf(ApiException.class);verify(http,times(1)).send(eq("POST"),any(),anyMap(),any());verify(oauth).invalidate("test-oauth-token");}
    @Test void oauthRefreshIsCachedAndFormEncoded(){Clock clock=mock(Clock.class);var instant=new AtomicReference<>(Instant.parse("2026-09-13T10:00:00Z"));when(clock.instant()).thenAnswer(i->instant.get());when(http.send(eq("POST"),any(),anyMap(),any())).thenAnswer(call->{assertThat(call.getArgument(1).toString()).isEqualTo("https://accounts.zoho.in/oauth/v2/token");String form=new String((byte[])call.getArgument(3),StandardCharsets.UTF_8);assertThat(form).contains("grant_type=refresh_token","client_secret=secret%26value","refresh_token=private-refresh");return response("{\"access_token\":\"private-access\",\"expires_in\":3600}");});var tokens=new ZohoAccessToken(http,json,clock,"client","secret&value","private-refresh","");assertThat(tokens.get()).isEqualTo("private-access");tokens.get();verify(http,times(1)).send(anyString(),any(),anyMap(),any());instant.set(instant.get().plusSeconds(3600));tokens.get();verify(http,times(2)).send(anyString(),any(),anyMap(),any());}
    @Test void enabledGatewayRequiresCompleteConfiguration(){assertThatThrownBy(()->new ZohoPaymentsGateway(true,true,"23137556","key","signing","webhook",oauth,http,json,Clock.systemUTC())).isInstanceOf(IllegalStateException.class);}
}
