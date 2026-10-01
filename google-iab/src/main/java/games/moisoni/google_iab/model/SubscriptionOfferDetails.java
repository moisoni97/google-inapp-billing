package games.moisoni.google_iab.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A base plan or an offer of a subscription, from ProductInfo.getSubscriptionOfferDetails()
 * <p>
 * Purchase it with BillingConnector.subscribe(activity, productId, basePlanId, offerId)
 */
public class SubscriptionOfferDetails {

    private final String offerId;
    private final List<String> offerTags;
    private final String offerToken;
    private final String basePlanId;
    private final List<PricingPhases> pricingPhases;

    /**
     * Creates the details of a base plan or offer from the values returned by Play Billing
     */
    public SubscriptionOfferDetails(@Nullable String offerId, List<ProductDetails.PricingPhase> pricingPhases, List<String> offerTags, String offerToken, String basePlanId) {
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

    /**
     * Returns the offer ID from Play Console, or null for the base plan itself (no offer)
     */
    @Nullable
    public String getOfferId() {
        return offerId;
    }

    /**
     * Returns the tags set in Play Console for the base plan and the offer
     */
    public List<String> getOfferTags() {
        return offerTags;
    }

    /**
     * Returns the token that selects this base plan or offer in the purchase flow (used by subscribe())
     */
    public String getOfferToken() {
        return offerToken;
    }

    /**
     * Returns the base plan ID from Play Console
     */
    public String getBasePlanId() {
        return basePlanId;
    }

    /**
     * Returns the pricing phases in the order they are charged (e.g. a free trial, then the regular price)
     * <p>
     * The last phase is the regular price
     */
    public List<PricingPhases> getPricingPhases() {
        return pricingPhases;
    }

    @NonNull
    private PricingPhases createPricingPhase(@NonNull ProductDetails.PricingPhase pricingPhase) {
        return new PricingPhases(pricingPhase.getFormattedPrice(), pricingPhase.getPriceAmountMicros(), pricingPhase.getPriceCurrencyCode(),
                pricingPhase.getBillingPeriod(), pricingPhase.getBillingCycleCount(), pricingPhase.getRecurrenceMode());
    }

    /**
     * A pricing phase of a base plan or offer: a price charged every billing period, for a number of periods
     */
    public static final class PricingPhases {

        private final String formattedPrice;
        private final long priceAmountMicros;
        private final String priceCurrencyCode;
        private final String billingPeriod;
        private final int billingCycleCount;
        private final int recurrenceMode;

        /**
         * Creates a pricing phase from the values returned by Play Billing
         */
        public PricingPhases(String formattedPrice, long priceAmountMicros, String priceCurrencyCode, String billingPeriod, int billingCycleCount, int recurrenceMode) {
            this.formattedPrice = formattedPrice;
            this.priceAmountMicros = priceAmountMicros;
            this.priceCurrencyCode = priceCurrencyCode;
            this.billingPeriod = billingPeriod;
            this.billingCycleCount = billingCycleCount;
            this.recurrenceMode = recurrenceMode;
        }

        /**
         * Returns the formatted price, including the currency symbol (e.g. "$4.99")
         */
        public String getFormattedPrice() {
            return formattedPrice;
        }

        /**
         * Returns the price in micro-units (1,000,000 micro-units = 1 unit of the currency), 0 for a free trial
         */
        public long getPriceAmountMicros() {
            return priceAmountMicros;
        }

        /**
         * Returns the ISO 4217 currency code (e.g. "USD")
         */
        public String getPriceCurrencyCode() {
            return priceCurrencyCode;
        }

        /**
         * Returns the billing period in ISO 8601 format (e.g. "P1W" for one week, "P1M" for one month, "P1Y" for one year)
         */
        public String getBillingPeriod() {
            return billingPeriod;
        }

        /**
         * Returns the number of billing periods the phase lasts, 0 when it recurs until the subscription is canceled
         */
        public int getBillingCycleCount() {
            return billingCycleCount;
        }

        /**
         * Returns how the phase recurs (ProductDetails.RecurrenceMode): INFINITE_RECURRING (1) until the subscription
         * is canceled, FINITE_RECURRING (2) for getBillingCycleCount() periods, or NON_RECURRING (3) charged once
         */
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