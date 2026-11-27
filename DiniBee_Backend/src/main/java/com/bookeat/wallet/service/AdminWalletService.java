package com.bookeat.wallet.service;
import com.bookeat.wallet.repository.WalletRepository;import com.bookeat.wallet.mapper.WalletMapper;import com.bookeat.wallet.dto.WalletResponses;import com.bookeat.common.response.PageResponse;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.data.domain.*;import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional public class AdminWalletService {
 private final WalletServiceImpl service;private final WalletRepository wallets;private final WalletMapper mapper;
 public record View(UUID customerId,WalletResponses.Balance wallet){}
 public View get(UUID customer){return new View(customer,mapper.balance(service.lock(customer)));}
 public PageResponse<View> list(int page,int size){return PageResponse.from(wallets.findAll(PageRequest.of(page,size,Sort.by("id"))).map(w->new View(w.getCustomerId(),mapper.balance(w))));}
}
