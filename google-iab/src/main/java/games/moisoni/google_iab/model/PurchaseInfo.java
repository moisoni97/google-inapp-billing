package games.moisoni.google_iab.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.Purchase;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import games.moisoni.google_iab.type.SkuProductType;

/**
 * A purchase of one product, passed to the BillingEventListener callbacks and returned by BillingConnector.getPurchasedProductsList()
 * <p>
 * A purchase of several products (getProducts()) is reported as one PurchaseInfo per product, all with the same purchase token
 * <p>
 * The values are a snapshot of the purchase when it was fetched or made, they are not updated afterward
 */
public class PurchaseInfo {

    private final SkuProductType skuProductType;
    private final ProductInfo productInfo;
    private final Purchase purchase;

    private final String product;

    private final AccountIdentifiers accountIdentifiers;
    private final List<String> products;

    private final String orderId;
    private final String purchaseToken;
    private final String originalJson;
    private final String developerPayload;
    private final String packageName;
    private final String signature;

    private final int quantity;
    private final int purchaseState;

    private final long purchaseTime;

    private final boolean isAcknowledged;
    private final boolean isAutoRenewing;

    /**
     * Creates a PurchaseInfo for a product whose details were fetched
     */
    public PurchaseInfo(@NonNull ProductInfo productInfo, @NonNull Purchase purchase) {
        this(productInfo.getSkuProductType(), productInfo.getProduct(), productInfo, purchase);
    }

    /**
     * Creates a PurchaseInfo for an owned product whose details are not available
     * (e.g. the product was deactivated in Play Console or its details query failed)
     */
    public PurchaseInfo(@NonNull SkuProductType skuProductType, @NonNull String product, @NonNull Purchase purchase) {
        this(skuProductType, product, null, purchase);
    }

    private PurchaseInfo(@NonNull SkuProductType skuProductType, @NonNull String product, @Nullable ProductInfo productInfo, @NonNull Purchase purchase) {
        this.productInfo = productInfo;
        this.purchase = purchase;
        this.product = product;
        this.skuProductType = skuProductType;
        this.accountIdentifiers = purchase.getAccountIdentifiers();
        this.products = purchase.getProducts();
        this.orderId = purchase.getOrderId();
        this.purchaseToken = purchase.getPurchaseToken();
        this.originalJson = purchase.getOriginalJson();
        this.developerPayload = purchase.getDeveloperPayload();
        this.packageName = purchase.getPackageName();
        this.signature = purchase.getSignature();
        this.quantity = purchase.getQuantity();
        this.purchaseState = purchase.getPurchaseState();
        this.purchaseTime = purchase.getPurchaseTime();
        this.isAcknowledged = purchase.isAcknowledged();
        this.isAutoRenewing = purchase.isAutoRenewing();
    }

    /**
     * Returns CONSUMABLE, NON_CONSUMABLE or SUBSCRIPTION, based on the product ID list the product was added to
     */
    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    /**
     * Returns the product details of this purchase
     * <p>
     * Can be null when Play Console no longer returns details for an owned product
     * (e.g. the product was deactivated) or when the product details query failed
     */
    @Nullable
    public ProductInfo getProductInfo() {
        return productInfo;
    }

    /**
     * Returns the original Play Billing Purchase
     */
    public Purchase getPurchase() {
        return purchase;
    }

    /**
     * Returns the product ID of this entry (one of getProducts())
     */
    public String getProduct() {
        return product;
    }

    /**
     * Returns the obfuscated account and profile IDs, or null when none were set in BillingFlowParams
     */
    @Nullable
    public AccountIdentifiers getAccountIdentifiers() {
        return accountIdentifiers;
    }

    /**
     * Returns all product IDs of the purchase (read-only), usually a single one
     */
    public List<String> getProducts() {
        return Collections.unmodifiableList(products);
    }

    /**
     * Returns the order ID, or null when the purchase is not completed yet (PENDING)
     */
    @Nullable
    public String getOrderId() {
        return orderId;
    }

    /**
     * Returns the token that identifies the purchase on Google Play, shared by all products of the purchase
     * <p>
     * Used to consume or acknowledge the purchase, and to verify it on a server
     */
    public String getPurchaseToken() {
        return purchaseToken;
    }

    /**
     * Returns the purchase data as JSON, as signed by Google Play
     * <p>
     * Send it with getSignature() to a server to verify the purchase there
     */
    public String getOriginalJson() {
        return originalJson;
    }

    /**
     * Returns the developer payload
     * <p>
     * Play Billing no longer supports setting one, so it is empty for new purchases
     */
    public String getDeveloperPayload() {
        return developerPayload;
    }

    /**
     * Returns the package name of the app the purchase belongs to
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Returns the signature of getOriginalJson()
     * <p>
     * It can be verified with the license key (public key) from Play Console. BillingConnector verifies it
     * when a license key is passed to its constructor
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns the purchased quantity
     * <p>
     * Greater than 1 only for products with multi-quantity purchases enabled in Play Console
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the purchase state: Purchase.PurchaseState.PURCHASED, PENDING or UNSPECIFIED_STATE
     * <p>
     * See isPurchased() and isPending()
     */
    public int getPurchaseState() {
        return purchaseState;
    }

    /**
     * Returns the time of the purchase, in milliseconds since the epoch (January 1, 1970, UTC)
     */
    public long getPurchaseTime() {
        return purchaseTime;
    }

    /**
     * Returns whether the purchase was acknowledged when it was fetched or made
     * <p>
     * Not updated afterward: it is still false inside onPurchaseAcknowledged(), and the next purchases refresh
     * reports the acknowledged purchase
     */
    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    /**
     * Returns whether the subscription renews automatically
     * <p>
     * False once the user canceled it (it stays active until the end of the paid period), and for in-app products
     */
    public boolean isAutoRenewing() {
        return isAutoRenewing;
    }

    /**
     * Returns true when the purchase state is PURCHASED (the payment is complete)
     */
    public boolean isPurchased() {
        return purchaseState == Purchase.PurchaseState.PURCHASED;
    }

    /**
     * Returns true when the purchase state is PENDING (e.g. a cash payment that is not completed yet)
     * <p>
     * A pending purchase can't be consumed or acknowledged yet, don't grant entitlement for it
     */
    public boolean isPending() {
        return purchaseState == Purchase.PurchaseState.PENDING;
    }

    /**
     * Two PurchaseInfo are equal when they have the same purchase token and product ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PurchaseInfo that = (PurchaseInfo) obj;
        return Objects.equals(purchaseToken, that.purchaseToken) && Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purchaseToken, product);
    }
}