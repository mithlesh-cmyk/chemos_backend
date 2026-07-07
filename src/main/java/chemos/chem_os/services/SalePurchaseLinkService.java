package chemos.chem_os.services;

import chemos.chem_os.dto.CreateSalePurchaseLinkRequest;
import chemos.chem_os.dto.PurchaseLinkSummaryResponse;
import chemos.chem_os.dto.SaleLinkSummaryResponse;
import chemos.chem_os.dto.SalePurchaseLinkResponse;
import chemos.chem_os.dto.UpdateSalePurchaseLinkRequest;
import chemos.chem_os.model.Purchase;
import chemos.chem_os.model.Sales;
import chemos.chem_os.model.SalePurchaseLink;
import chemos.chem_os.repository.PurchaseRepository;
import chemos.chem_os.repository.SalePurchaseLinkRepository;
import chemos.chem_os.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalePurchaseLinkService {

    private final SalePurchaseLinkRepository linkRepository;
    private final SalesRepository salesRepository;
    private final PurchaseRepository purchaseRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SalePurchaseLinkResponse createLink(CreateSalePurchaseLinkRequest request) {
        Sales sale = salesRepository.findById(request.saleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + request.saleId()));

        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + request.purchaseId()));

        if (request.linkedQuantity() == null || request.linkedQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Linked quantity must be a positive number");
        }

        if (linkRepository.existsBySaleIdAndPurchaseId(request.saleId(), request.purchaseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A link already exists between this sale and purchase. Use PUT to update it.");
        }

        if (sale.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sale " + request.saleId() + " has no quantity set.");
        }
        if (purchase.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase " + request.purchaseId() + " has no quantity set.");
        }

        validateQuantities(request.saleId(), request.purchaseId(), request.linkedQuantity(),
                sale.getQuantity(), purchase.getQuantity(), null);

        SalePurchaseLink link = SalePurchaseLink.builder()
                .saleId(request.saleId())
                .purchaseId(request.purchaseId())
                .createdByUsername(resolveCurrentUsername())
                .linkedQuantity(request.linkedQuantity())
                .build();

        SalePurchaseLink saved = linkRepository.save(link);
        auditLogService.log("CREATE", "SALE_PURCHASE_LINK", saved.getId(), null, saved);
        return buildResponse(saved, purchase, sale);
    }

    @Transactional
    public SalePurchaseLinkResponse updateLink(String id, UpdateSalePurchaseLinkRequest request) {
        SalePurchaseLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Link not found with id: " + id));

        if (request.linkedQuantity() == null || request.linkedQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Linked quantity must be a positive number");
        }

        Sales sale = salesRepository.findById(link.getSaleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + link.getSaleId()));

        Purchase purchase = purchaseRepository.findById(link.getPurchaseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + link.getPurchaseId()));

        if (sale.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sale " + link.getSaleId() + " has no quantity set.");
        }
        if (purchase.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase " + link.getPurchaseId() + " has no quantity set.");
        }

        // Pass the current link so its existing quantity is excluded from the validation sums
        validateQuantities(link.getSaleId(), link.getPurchaseId(), request.linkedQuantity(),
                sale.getQuantity(), purchase.getQuantity(), link);

        SalePurchaseLink snapshot = link.toBuilder().build();
        link.setLinkedQuantity(request.linkedQuantity());
        SalePurchaseLink saved = linkRepository.save(link);
        auditLogService.log("UPDATE", "SALE_PURCHASE_LINK", saved.getId(), snapshot, saved);
        return buildResponse(saved, purchase, sale);
    }

    @Transactional
    public void deleteLink(String id) {
        SalePurchaseLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Link not found with id: " + id));
        linkRepository.delete(link);
        auditLogService.log("DELETE", "SALE_PURCHASE_LINK", id, link, null);
    }

    @Transactional(readOnly = true)
    public List<SalePurchaseLinkResponse> getLinksByUser(String username) {
        String resolvedUsername = (username == null || username.isBlank())
                ? resolveCurrentUsername()
                : username.trim();

        List<SalePurchaseLink> links = linkRepository.findByCreatedByUsernameOrderByCreatedAtDesc(resolvedUsername);
        return links.stream()
                .map(link -> {
                    Purchase purchase = purchaseRepository.findById(link.getPurchaseId()).orElse(null);
                    Sales sale = salesRepository.findById(link.getSaleId()).orElse(null);
                    if (purchase == null || sale == null) {
                        return null;
                    }
                    return buildResponse(link, purchase, sale);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleLinkSummaryResponse getSaleLinkSummary(String saleId) {
        Sales sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + saleId));

        List<SalePurchaseLink> links = linkRepository.findBySaleId(saleId);
        double totalLinked = links.stream().mapToDouble(SalePurchaseLink::getLinkedQuantity).sum();
        double remaining = sale.getQuantity() - totalLinked;

        List<SaleLinkSummaryResponse.LinkedPurchaseItem> items = links.stream().map(link -> {
            Purchase purchase = purchaseRepository.findById(link.getPurchaseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Purchase not found with id: " + link.getPurchaseId()));
            double purchaseUsed = linkRepository.sumLinkedQuantityByPurchaseId(link.getPurchaseId());
            double purchaseAvailable = purchase.getQuantity() - purchaseUsed;
            return new SaleLinkSummaryResponse.LinkedPurchaseItem(
                    link.getId(),
                    link.getPurchaseId(),
                    link.getLinkedQuantity(),
                    purchase.getQuantity(),
                    purchaseAvailable
            );
        }).toList();

        return new SaleLinkSummaryResponse(saleId, sale.getQuantity(), totalLinked, remaining, items);
    }

    @Transactional(readOnly = true)
    public PurchaseLinkSummaryResponse getPurchaseLinkSummary(String purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Purchase not found with id: " + purchaseId));

        List<SalePurchaseLink> links = linkRepository.findByPurchaseId(purchaseId);
        double totalLinked = links.stream().mapToDouble(SalePurchaseLink::getLinkedQuantity).sum();
        double availableQuantity = purchase.getQuantity() - totalLinked;

        List<PurchaseLinkSummaryResponse.LinkedSaleItem> items = links.stream().map(link -> {
            Sales sale = salesRepository.findById(link.getSaleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Sale not found with id: " + link.getSaleId()));
            double saleUsed = linkRepository.sumLinkedQuantityBySaleId(link.getSaleId());
            double saleRemaining = sale.getQuantity() - saleUsed;
            return new PurchaseLinkSummaryResponse.LinkedSaleItem(
                    link.getId(),
                    link.getSaleId(),
                    link.getLinkedQuantity(),
                    sale.getQuantity(),
                    saleRemaining
            );
        }).toList();

        return new PurchaseLinkSummaryResponse(purchaseId, purchase.getQuantity(), totalLinked, availableQuantity, items);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that {@code proposedQty} can be committed for a given (saleId, purchaseId) pair.
     *
     * @param existingLink  the current link being updated, or {@code null} for a new link.
     *                      When not null, its existing linkedQuantity is subtracted from the
     *                      running sums before checking, so it doesn't count against itself.
     */
    private void validateQuantities(String saleId, String purchaseId, double proposedQty,
                                    double saleTotal, double purchaseOriginal,
                                    SalePurchaseLink existingLink) {

        double purchaseAlreadyUsed = linkRepository.sumLinkedQuantityByPurchaseId(purchaseId);
        double saleAlreadyLinked = linkRepository.sumLinkedQuantityBySaleId(saleId);

        if (existingLink != null) {
            // Exclude this link's own current value from the running sums
            purchaseAlreadyUsed -= existingLink.getLinkedQuantity();
            saleAlreadyLinked -= existingLink.getLinkedQuantity();
        }

        double purchaseAvailable = purchaseOriginal - purchaseAlreadyUsed;
        double saleRemaining = saleTotal - saleAlreadyLinked;

        if (proposedQty > purchaseAvailable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Linked quantity (%.2f) exceeds available PO quantity (%.2f). "
                            + "The PO has %.2f already committed to other sales.",
                            proposedQty, purchaseAvailable, purchaseAlreadyUsed));
        }

        if (proposedQty > saleRemaining) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Linked quantity (%.2f) exceeds remaining sale requirement (%.2f). "
                            + "The sale already has %.2f linked from other POs.",
                            proposedQty, saleRemaining, saleAlreadyLinked));
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                return name;
            }
        }
        return "system";
    }

    /**
     * Builds a {@link SalePurchaseLinkResponse} with up-to-date derived quantities
     * computed after the link has been persisted.
     */
    private SalePurchaseLinkResponse buildResponse(SalePurchaseLink link, Purchase purchase, Sales sale) {
        double purchaseUsed = linkRepository.sumLinkedQuantityByPurchaseId(link.getPurchaseId());
        double purchaseAvailable = purchase.getQuantity() - purchaseUsed;
        double saleUsed = linkRepository.sumLinkedQuantityBySaleId(link.getSaleId());
        double saleRemaining = sale.getQuantity() - saleUsed;

        return new SalePurchaseLinkResponse(
                link.getId(),
                link.getSaleId(),
                link.getPurchaseId(),
                link.getCreatedByUsername(),
                link.getLinkedQuantity(),
                purchase.getQuantity(),
                purchaseAvailable,
                sale.getQuantity(),
                saleRemaining,
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
