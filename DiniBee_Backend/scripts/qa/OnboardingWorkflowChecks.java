import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.service.WorkflowRules;
import com.bookeat.restaurant.security.SettlementCipher;
import com.bookeat.restaurant.storage.DocumentContentRules;
import java.util.*;
import java.time.*;
public class OnboardingWorkflowChecks {
 static int count;
 static void check(boolean b){count++;if(!b)throw new AssertionError("Assertion "+count);}
 static void rejects(Runnable r){boolean rejected=false;try{r.run();}catch(RuntimeException ex){rejected=true;}check(rejected);}
 public static void main(String[] args) {
  var expected=new EnumMap<ReviewAction,Set<ApprovalStatus>>(ReviewAction.class);
  expected.put(ReviewAction.SUBMIT,Set.of(ApprovalStatus.DRAFT,ApprovalStatus.REJECTED,ApprovalStatus.CHANGES_REQUESTED));
  expected.put(ReviewAction.UNDER_REVIEW,Set.of(ApprovalStatus.SUBMITTED));
  expected.put(ReviewAction.APPROVE,Set.of(ApprovalStatus.UNDER_REVIEW));
  expected.put(ReviewAction.REJECT,Set.of(ApprovalStatus.SUBMITTED,ApprovalStatus.UNDER_REVIEW));
  expected.put(ReviewAction.REQUEST_CHANGES,Set.of(ApprovalStatus.SUBMITTED,ApprovalStatus.UNDER_REVIEW,ApprovalStatus.APPROVED));
  expected.put(ReviewAction.SUSPEND,Set.of(ApprovalStatus.APPROVED));expected.put(ReviewAction.REACTIVATE,Set.of(ApprovalStatus.SUSPENDED));
  expected.put(ReviewAction.SERVICE_CHANGE,Set.of(ApprovalStatus.APPROVED));expected.put(ReviewAction.DOCUMENT_VERIFY,Set.of(ApprovalStatus.UNDER_REVIEW));expected.put(ReviewAction.SETTLEMENT_VERIFY,Set.of(ApprovalStatus.UNDER_REVIEW));
  for(var action:ReviewAction.values())for(var from:ApprovalStatus.values())check(WorkflowRules.permits(from,action)==expected.get(action).contains(from));
  for(var approval:ApprovalStatus.values())for(var status:RestaurantStatus.values())for(int flags=0;flags<8;flags++)
    check(WorkflowRules.effectiveService(approval,status,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0)==(approval==ApprovalStatus.APPROVED && status==RestaurantStatus.ACTIVE && flags==7));
  check(WorkflowRules.validHours(true,null,null,false));check(!WorkflowRules.validHours(true,LocalTime.NOON,null,false));
  check(WorkflowRules.validHours(false,LocalTime.of(20,0),LocalTime.of(2,0),true));check(!WorkflowRules.validHours(false,LocalTime.of(20,0),LocalTime.of(2,0),false));
  check(!WorkflowRules.validHours(false,LocalTime.NOON,LocalTime.NOON,false));check(WorkflowRules.validHours(false,LocalTime.NOON,LocalTime.NOON,true));
  var c=new SettlementCipher(Base64.getEncoder().encodeToString(new byte[32]));var id=UUID.randomUUID();String bank="123456789012";
  String a=c.encrypt(id,bank),b=c.encrypt(id,bank);check(!a.equals(b));check(!a.contains(bank));check(c.decrypt(id,a).equals(bank));
  rejects(()->c.decrypt(UUID.randomUUID(),a));rejects(()->new SettlementCipher("short"));rejects(()->c.decrypt(id,"v2:"+a));
  byte[] tampered=Base64.getDecoder().decode(a.substring(3));tampered[15]^=1;rejects(()->c.decrypt(id,"v1:"+Base64.getEncoder().encodeToString(tampered)));
  check(DocumentContentRules.detect("%PDF-1.7 test".getBytes()).equals("application/pdf"));
  check(DocumentContentRules.detect(new byte[]{(byte)137,80,78,71,13,10,26,10}).equals("image/png"));
  check(DocumentContentRules.detect(new byte[]{(byte)255,(byte)216,(byte)255}).equals("image/jpeg"));
  rejects(()->DocumentContentRules.detect("<script/>".getBytes()));rejects(()->DocumentContentRules.detect(new byte[DocumentContentRules.MAX_BYTES+1]));
  rejects(()->DocumentContentRules.validateKey("../../credentials"));rejects(()->DocumentContentRules.validateKey("/etc/passwd"));
  DocumentContentRules.validateKey("restaurants/"+UUID.randomUUID()+"/documents/"+UUID.randomUUID());count++;
  check(DocumentContentRules.safeFilename("../../unsafe file\r\n.pdf").equals("unsafe_file__.pdf"));
  System.out.println("PASS: "+count+" executable assertions against actual Phase 3 helper classes; not Spring/JUnit/integration tests.");
 }
}
