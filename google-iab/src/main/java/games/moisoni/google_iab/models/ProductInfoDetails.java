package games.moisoni.google_iab.models;

public class ProductInfoDetails {

    public static class OneTimePurchaseOfferDetails {


        private String formattedPrice;
        private long priceAmountMicros;
        private String priceCurrencyCode;

        public OneTimePurchaseOfferDetails(String formattedPrice, long priceAmountMicros, String priceCurrencyCode) {
            this.formattedPrice = formattedPrice;
            this.priceAmountMicros = priceAmountMicros;
            this.priceCurrencyCode = priceCurrencyCode;
        }

        public String getFormattedPrice() {
            return formattedPrice;
        }

        public void setFormattedPrice(String formattedPrice) {
            this.formattedPrice = formattedPrice;
        }

        public long getPriceAmountMicros() {
            return priceAmountMicros;
        }

        public void setPriceAmountMicros(long priceAmountMicros) {
            this.priceAmountMicros = priceAmountMicros;
        }

        public String getPriceCurrencyCode() {
            return priceCurrencyCode;
        }

        public void setPriceCurrencyCode(String priceCurrencyCode) {
            this.priceCurrencyCode = priceCurrencyCode;
        }

    }

    public class SubscriptionOfferDetails {

    }
}
