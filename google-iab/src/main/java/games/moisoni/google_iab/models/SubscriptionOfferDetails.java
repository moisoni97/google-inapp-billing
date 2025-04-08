package games.moisoni.google_iab.models;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionOfferDetails {

    private final String offerId;
    private final List<String> offerTags;
    private final String offerToken;
    private final String basePlanId;
    private final List<PricingPhases> pricingPhases;

    public SubscriptionOfferDetails(String offerId, List<ProductDetails.PricingPhase> pricingPhases, List<String> offerTags, String offerToken, String basePlanId) {
        this.offerId = offerId;
        this.offerTags = offerTags;
        this.offerToken = offerToken;
        this.basePlanId = basePlanId;

        this.pricingPhases = new ArrayList<>();

        if (pricingPhases != null) {
            for (ProductDetails.PricingPhase pricingPhase : pricingPhases) {
                PricingPhases newPricingPhase = createPricingPhase(pricingPhase);
                this.pricingPhases.add(newPricingPhase);
            }
        }
    }

    public String getOfferId() {
        return offerId;
    }

    public List<String> getOfferTags() {
        return offerTags;
    }

    public String getOfferToken() {
        return offerToken;
    }

    public String getBasePlanId() {
        return basePlanId;
    }

    public List<PricingPhases> getPricingPhases() {
        return pricingPhases;
    }

    private PricingPhases createPricingPhase(ProductDetails.PricingPhase pricingPhase) {
        return new PricingPhases(pricingPhase.getFormattedPrice(), pricingPhase.getPriceAmountMicros(), pricingPhase.getPriceCurrencyCode(),
                pricingPhase.getBillingPeriod(), pricingPhase.getBillingCycleCount(), pricingPhase.getRecurrenceMode());
    }

    public record PricingPhases(String formattedPrice, long priceAmountMicros,
                                String priceCurrencyCode, String billingPeriod,
                                int billingCycleCount, int recurrenceMode) {

    }
}
