package com.bookeat;
import com.bookeat.payment.gateway.ZohoSignatures;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
public final class ZohoProtocolChecks {
    private static int checks;
    private static void check(boolean b){checks++;if(!b)throw new AssertionError("Zoho protocol assertion "+checks);}
    public static void main(String[] args){
        String key="bookeat-test-signing-key",session="173000002315107",payment="173000002895221",signature="415d295261db9b924310a2713416a5ee50d02973cf2131f521b232eb9625859f";
        check(ZohoSignatures.checkout(key,session,payment,signature));
        check(!ZohoSignatures.checkout(key,session,payment,"bc1d102103c7bbec80741ef2f18b650ffc7dd24d932565b118d4f6220d13fd4f"));
        check(!ZohoSignatures.checkout(key,session,"173000002895222",signature));
        check(!ZohoSignatures.checkout("other",session,payment,signature));
        check(!ZohoSignatures.checkout(key,"order_old",payment,signature));
        check(!ZohoSignatures.checkout(key,session,"pay_old",signature));
        byte[] body="{\"event_id\":1987000003853347,\"event_type\":\"payment.succeeded\"}".getBytes(StandardCharsets.UTF_8);
        String header="t=1789293600000,v=8bb827fe2159f7e7c66ac0147e98398519b4f6ead003f96d1074b956dc4d09b5";
        Instant now=Instant.ofEpochMilli(Long.parseLong("1789293600000"));
        check(ZohoSignatures.webhook(key,body,header,now,300));
        check(ZohoSignatures.webhook(key,body,header,now.plusSeconds(300),300));
        check(!ZohoSignatures.webhook(key,body,header,now.plusSeconds(301),300));
        check(!ZohoSignatures.webhook(key,body,header,now.minusSeconds(301),300));
        check(!ZohoSignatures.webhook(key,body,header+",v=8bb827fe2159f7e7c66ac0147e98398519b4f6ead003f96d1074b956dc4d09b5",now,300));
        check(!ZohoSignatures.webhook(key,body,"v=8bb827fe2159f7e7c66ac0147e98398519b4f6ead003f96d1074b956dc4d09b5",now,300));
        check(!ZohoSignatures.webhook(key,body,"t=not-a-timestamp,v=8bb827fe2159f7e7c66ac0147e98398519b4f6ead003f96d1074b956dc4d09b5",now,300));
        check(!ZohoSignatures.webhook(key,body,header+",x=1",now,300));
        check(!ZohoSignatures.webhook(key,new byte[262145],header,now,300));
        check(!ZohoSignatures.webhook("",body,header,now,300));
        for(int i=0;i<body.length;i++){byte[] changed=body.clone();changed[i]^=1;check(!ZohoSignatures.webhook(key,changed,header,now,300));}
        for(int i=0;i<64;i++){String changed=signature.substring(0,i)+(signature.charAt(i)=='a'?'b':'a')+signature.substring(i+1);check(!ZohoSignatures.checkout(key,session,payment,changed));}
        for(String bad:new String[]{"","-1","12/3"," 123","１２３","123?account_id=4","1".repeat(33)})check(!ZohoSignatures.validId(bad));
        check(ZohoSignatures.validId("1".repeat(32)));
        System.out.println("PASS: "+checks+" executed Zoho signature/ID/replay protocol assertions");
    }
}
