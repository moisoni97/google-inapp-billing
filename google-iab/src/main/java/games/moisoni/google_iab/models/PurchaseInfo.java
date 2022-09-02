package games.moisoni.google_iab.models;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.Purchase;

import java.util.List;

import games.moisoni.google_iab.enums.SkuProductType;

public class PurchaseInfo {

    private final SkuProductType skuProductType;
    private final ProductInfo skuInfo;
    private final Purchase purchase;

    private final String sku;

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

    public PurchaseInfo(ProductInfo skuInfo, Purchase purchase) {
        this.skuInfo = skuInfo;
        this.purchase = purchase;
        this.sku = skuInfo.getSku();
        this.skuProductType = skuInfo.getSkuProductType();
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

    public ProductInfo getSkuInfo() {
        return skuInfo;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public String getSku() {
        return sku;
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