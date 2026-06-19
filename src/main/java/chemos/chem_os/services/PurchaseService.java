package chemos.chem_os.services;

import chemos.chem_os.dto.CreatePurchaseRequest;
import chemos.chem_os.dto.UpdatePurchaseRequest;
import chemos.chem_os.mapper.PurchaseMapper;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.repository.PurchaseRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
//@Transactional can be used for more safety

public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;

    public Purchase createPurchase(CreatePurchaseRequest createPurchaseRequest){
        Purchase purchase = purchaseMapper.toEntity(createPurchaseRequest);
        return purchaseRepository.save(purchase);
    }

    public List<Purchase> getAllPurchase() {
        return purchaseRepository.findAll();
    }

    public Purchase getPurchaseById(String id){
        return purchaseRepository.findById(id)
                .orElseThrow(() -> (
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Purchase not found with id: " + id
                        )
                        ));
    }

    public Purchase updatePurchase(String id, UpdatePurchaseRequest updateRequest) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + id
                ));

        purchaseMapper.updateEntity(purchase, updateRequest);
        return purchaseRepository.save(purchase);
    }
}
