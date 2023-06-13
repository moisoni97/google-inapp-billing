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

        List<ProductDetails.PricingPhase> pricingPhaseList = pricingPhases;
        this.pricingPhases = new ArrayList<>();

        if (pricingPhaseList != null) {
            for (ProductDetails.PricingPhase pricingPhase : pricingPhaseList) {
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

    public class PricingPhases {

        private final String formattedPrice;
        private final long priceAmountMicros;
        private final String priceCurrencyCode;
        private final String billingPeriod;
        private final int billingCycleCount;
        private final int recurrenceMode;

        public PricingPhases(String formattedPrice, long priceAmountMicros, String priceCurrencyCode, String billingPeriod, int billingCycleCount, int recurrenceMode) {
            this.formattedPrice = formattedPrice;
            this.priceAmountMicros = priceAmountMicros;
            this.priceCurrencyCode = priceCurrencyCode;
            this.billingPeriod = billingPeriod;
            this.billingCycleCount = billingCycleCount;
            this.recurrenceMode = recurrenceMode;
        }

        public String getFormattedPrice() {
            return formattedPrice;
        }

        public long getPriceAmountMicros() {
            return priceAmountMicros;
        }

        public String getPriceCurrencyCode() {
            return priceCurrencyCode;
        }

        public String getBillingPeriod() {
            return billingPeriod;
        }

        public int getBillingCycleCount() {
            return billingCycleCount;
        }

        public int getRecurrenceMode() {
            return recurrenceMode;
        }
    }
}
