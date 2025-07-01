package games.moisoni.google_iab.models;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @NonNull
    private PricingPhases createPricingPhase(@NonNull ProductDetails.PricingPhase pricingPhase) {
        return new PricingPhases(pricingPhase.getFormattedPrice(), pricingPhase.getPriceAmountMicros(), pricingPhase.getPriceCurrencyCode(),
                pricingPhase.getBillingPeriod(), pricingPhase.getBillingCycleCount(), pricingPhase.getRecurrenceMode());
    }

    public static final class PricingPhases {

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

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            PricingPhases that = (PricingPhases) object;
            return priceAmountMicros == that.priceAmountMicros &&
                    billingCycleCount == that.billingCycleCount &&
                    recurrenceMode == that.recurrenceMode &&
                    Objects.equals(formattedPrice, that.formattedPrice) &&
                    Objects.equals(priceCurrencyCode, that.priceCurrencyCode) &&
                    Objects.equals(billingPeriod, that.billingPeriod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(formattedPrice, priceAmountMicros, priceCurrencyCode, billingPeriod, billingCycleCount, recurrenceMode);
        }

        @NonNull
        @Override
        public String toString() {
            return "PricingPhases[" +
                    "formattedPrice='" + formattedPrice + '\'' +
                    ", priceAmountMicros=" + priceAmountMicros +
                    ", priceCurrencyCode='" + priceCurrencyCode + '\'' +
                    ", billingPeriod='" + billingPeriod + '\'' +
                    ", billingCycleCount=" + billingCycleCount +
                    ", recurrenceMode=" + recurrenceMode +
                    ']';
        }
    }
}