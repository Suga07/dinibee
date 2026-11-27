export function normalizeRestaurantDetail(detail) {
  if (!detail || typeof detail !== "object" || !detail.id) throw new Error("Restaurant detail response is missing");
  const loc = (detail.location && typeof detail.location === 'object') ? detail.location : detail;
  const biz = (detail.businessInfo && typeof detail.businessInfo === 'object') ? detail.businessInfo : detail;
  const opHours = Array.isArray(detail.operatingHours) ? detail.operatingHours : (detail.operatingHours?.hours || []);
  const reqServices = (detail.services || detail.requestedServices || [])
    .filter(s => typeof s === 'string' || s.requested === true || s.isRequested === true)
    .map(s => typeof s === 'string' ? s : (s.serviceType || s));
  
  return {
    ...detail,
    location: {
      addressLine1: loc.addressLine1, addressLine2: loc.addressLine2,
      city: loc.city || (typeof detail.location === 'string' ? detail.location : undefined),
      state: loc.state, postalCode: loc.postalCode, country: loc.country,
    },
    businessInfo: {
      businessName: detail.onboardingForm?.businessName ?? biz.businessName ?? biz.taxName ?? detail.restaurantName,
      businessRegistrationNumber: biz.businessRegistrationNumber,
      gstNumber: biz.gstNumber, fssaiNumber: biz.fssaiNumber,
    },
    operatingHours: { hours: opHours },
    requestedServices: reqServices,
    documents: (detail.documents || []).map(d => ({ ...d, name: d.originalFilename || d.name, status: d.verificationStatus || d.status })),
  };
}
