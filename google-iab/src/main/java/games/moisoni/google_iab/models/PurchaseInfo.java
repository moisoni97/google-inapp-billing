package games.moisoni.google_iab.models;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.Purchase;

import java.util.List;

import games.moisoni.google_iab.enums.SkuProductType;

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

    public PurchaseInfo(ProductInfo productInfo, Purchase purchase) {
        this.productInfo = productInfo;
        this.purchase = purchase;
        this.product = productInfo.getProduct();
        this.skuProductType = productInfo.getSkuProductType();
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

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public String getProduct() {
        return product;
    }

    public AccountIdentifiers getAccountIdentifiers() {
        return accountIdentifiers;
    }

    public List<String> getProducts() {
        return products;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public String getOriginalJson() {
        return originalJson;
    }

    public String getDeveloperPayload() {
        return developerPayload;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSignature() {
        return signature;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPurchaseState() {
        return purchaseState;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    public boolean isAutoRenewing() {
        return isAutoRenewing;
    }
}