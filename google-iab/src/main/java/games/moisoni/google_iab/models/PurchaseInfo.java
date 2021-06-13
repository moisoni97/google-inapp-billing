package games.moisoni.google_iab.models;

import com.android.billingclient.api.Purchase;

import games.moisoni.google_iab.enums.SkuProductType;

public class PurchaseInfo {

    private final String skuId;
    private final SkuProductType skuProductType;
    private final SkuInfo skuInfo;
    private final Purchase purchase;

    public PurchaseInfo(SkuInfo skuInfo, Purchase purchase) {
        this.skuInfo = skuInfo;
        this.purchase = purchase;
        this.skuId = skuInfo.getSkuId();
        this.skuProductType = skuInfo.getSkuProductType();
    }

    public String getSkuId() {
        return skuId;
    }

    public SkuProductType getSkuProductType() {
        return skuProductType;
    }

    public SkuInfo getSkuInfo() {
        return skuInfo;
    }

    public Purchase getPurchase() {
        return purchase;
    }
}