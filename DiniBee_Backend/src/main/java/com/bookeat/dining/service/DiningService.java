package com.bookeat.dining.service;
import com.bookeat.dining.dto.*;import com.bookeat.common.response.PageResponse;import java.util.*;
public interface DiningService {
 DiningResponse gps(UUID actor,UUID reservation,DiningRequests.Gps request);
 DiningResponse qr(UUID actor,UUID reservation,DiningRequests.Qr request);
 DiningResponse manual(UUID actor,UUID restaurant,UUID reservation,DiningRequests.Action request);
 CheckinToken issue(UUID actor,UUID restaurant,UUID reservation);
 List<DiningResponse> current(UUID actor);DiningResponse get(UUID actor,UUID id);
 PageResponse<DiningResponse> ownerList(UUID actor,UUID restaurant,int page,int size);
 DiningResponse action(UUID actor,UUID restaurant,UUID id,DiningRequests.Action request,String action);
 record CheckinToken(String token,java.time.Instant expiresAt){}
}
