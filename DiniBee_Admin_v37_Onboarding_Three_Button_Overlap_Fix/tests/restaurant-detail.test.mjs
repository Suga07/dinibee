import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeRestaurantDetail } from '../lib/bookeat/services/restaurant-detail.mjs';

test('flat backend DTO displays saved location, legal info, hours and requested services', () => {
  const result = normalizeRestaurantDetail({id:'r1', approvalStatus:'DRAFT', addressLine1:'10 Test Street', city:'Chennai',
    state:'TN', postalCode:'600001', businessRegistrationNumber:'REG-123', gstNumber:'GST-value', fssaiNumber:'12345678901234',
    onboardingForm:{businessName:'Test Hospitality'}, operatingHours:[{dayOfWeek:'MONDAY',closed:false}],
    services:[{serviceType:'RESTAURANT',requested:true,enabled:false},{serviceType:'TAKEAWAY',requested:false}],
    settlementProfile:{bankName:'Test Bank',maskedAccountNumber:'****9012'},
    documents:[{id:'d1',originalFilename:'license.pdf',verificationStatus:'PENDING'}]});
  assert.equal(result.location.city,'Chennai'); assert.equal(result.businessInfo.businessName,'Test Hospitality');
  assert.equal(result.businessInfo.businessRegistrationNumber,'REG-123');
  assert.equal(result.operatingHours.hours.length,1); assert.equal(result.approvalStatus,'DRAFT');
  assert.deepEqual(result.requestedServices,['RESTAURANT']);
  assert.equal(result.documents[0].name,'license.pdf'); assert.equal(result.documents[0].status,'PENDING');
  assert.equal(result.settlementProfile.maskedAccountNumber,'****9012');
});
test('missing optional data stays absent rather than being invented', () => {
  const result = normalizeRestaurantDetail({id:'r1',approvalStatus:'DRAFT'});
  assert.equal(result.location.city,undefined); assert.deepEqual(result.requestedServices,[]);
});
test('empty responses produce an error rather than a blank detail drawer', () => {
  for (const value of [null,undefined,{}]) assert.throws(() => normalizeRestaurantDetail(value));
});
