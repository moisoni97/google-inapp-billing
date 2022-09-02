package games.moisoni.google_iab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import games.moisoni.google_iab.enums.ErrorType;
import games.moisoni.google_iab.enums.PurchasedResult;
import games.moisoni.google_iab.enums.SkuProductType;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.enums.SupportState;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.PurchaseInfo;
import games.moisoni.google_iab.models.ProductInfo;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.android.billingclient.api.BillingClient.FeatureType.SUBSCRIPTIONS;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.BillingClient.ProductType.SUBS;

public class BillingConnector {

    private static final String TAG = "BillingConnector";
    private static final int defaultResponseCode = 99;

    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L;
    private long reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS;

    private final String base64Key;

    private BillingClient billingClient;
    private BillingEventListener billingEventListener;

    private List<String> consumableIds;
    private List<String> nonConsumableIds;
    private List<String> subscriptionIds;

    List<QueryProductDetailsParams.Product> allProductList = new ArrayList<>();

    private final List<ProductInfo> fetchedSkuInfoList = new ArrayList<>();
    private final List<PurchaseInfo> purchasedProductsList = new ArrayList<>();

    private boolean shouldAutoAcknowledge = false;
    private boolean shouldAutoConsume = false;
    private boolean shouldEnableLogging = false;

    private boolean isConnected = false;
    private boolean fetchedPurchasedProducts = false;

    /**
     * BillingConnector public constructor
     *
     * @param context   - is the application context
     * @param base64Key - is the public developer key from Play Console
     */
    public BillingConnector(Context context, String base64Key) {
        this.init(context);
        this.base64Key = base64Key;
    }

    /**
     * To initialize BillingConnector
     */
    private void init(Context context) {
        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener((billingResult, purchases) -> {
                    switch (billingResult.getResponseCode()) {
                        case OK:
                            if (purchases != null) {
                                processPurchases(ProductType.NONE, purchases, false);
                            }
                            break;
                        case USER_CANCELED:
                            Log("User pressed back or canceled a dialog." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.USER_CANCELED, billingResult)));
                            break;
                        case SERVICE_UNAVAILABLE:
                            Log("Network connection is down." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.SERVICE_UNAVAILABLE, billingResult)));
                            break;
                        case BILLING_UNAVAILABLE:
                            Log("Billing API version is not supported for the type requested." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.BILLING_UNAVAILABLE, billingResult)));
                            break;
                        case ITEM_UNAVAILABLE:
                            Log("Requested product is not available for purchase." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_UNAVAILABLE, billingResult)));
                            break;
                        case DEVELOPER_ERROR:
                            Log("Invalid arguments provided to the API." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.DEVELOPER_ERROR, billingResult)));
                            break;
                        case ERROR:
                            Log("Fatal error during the API action." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ERROR, billingResult)));
                            break;
                        case ITEM_ALREADY_OWNED:
                            Log("Failure to purchase since item is already owned." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_ALREADY_OWNED, billingResult)));
                            break;
                        case ITEM_NOT_OWNED:
                            Log("Failure to consume since item is not owned." + " Response code: " + billingResult.getResponseCode());
                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.ITEM_NOT_OWNED, billingResult)));
                            break;
                        case SERVICE_DISCONNECTED:
                        case SERVICE_TIMEOUT:
                            Log("Initialization error: service disconnected/timeout. Trying to reconnect...");
                            break;
                        default:
                            Log("Initialization error: " + new BillingResponse(ErrorType.BILLING_ERROR, billingResult));
                            break;
                    }
                })
                .build();
    }

    /**
     * To attach an event listener to establish a bridge with the caller
     */
    public final void setBillingEventListener(BillingEventListener billingEventListener) {
        this.billingEventListener = billingEventListener;
    }

    /**
     * To set consumable products ids
     */
    public final BillingConnector setConsumableIds(List<String> consumableIds) {
        this.consumableIds = consumableIds;
        return this;
    }

    /**
     * To set non-consumable products ids
     */
    public final BillingConnector setNonConsumableIds(List<String> nonConsumableIds) {
        this.nonConsumableIds = nonConsumableIds;
        return this;
    }

    /**
     * To set subscription products ids
     */
    public final BillingConnector setSubscriptionIds(List<String> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
        return this;
    }

    /**
     * To auto acknowledge the purchase
     */
    public final BillingConnector autoAcknowledge() {
        shouldAutoAcknowledge = true;
        return this;
    }

    /**
     * To auto consume the purchase
     */
    public final BillingConnector autoConsume() {
        shouldAutoConsume = true;
        return this;
    }

    /**
     * To enable logging for debugging
     */
    public final BillingConnector enableLogging() {
        shouldEnableLogging = true;
        return this;
    }

    /**
     * Returns the state of the billing client
     */
    public final boolean isReady() {
        if (!isConnected) {
            Log("Billing client is not ready because no connection is established yet");
        }

        if (!billingClient.isReady()) {
            Log("Billing client is not ready yet");
        }

        return isConnected && billingClient.isReady() && !fetchedSkuInfoList.isEmpty();
    }

    /**
     * Returns a boolean state of the SKU
     *
     * @param productId - is the SKU id that has to be checked
     */
    private boolean checkSkuBeforeInteraction(String productId) {
        if (!isReady()) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_NOT_READY,
                    "Client is not ready yet", defaultResponseCode)));
        } else if (productId != null && fetchedSkuInfoList.stream().noneMatch(it -> it.getSku().equals(productId))) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.SKU_NOT_EXIST,
                    "The SKU id: " + productId + " doesn't seem to exist on Play Console", defaultResponseCode)));
        } else return isReady();

        return false;
    }

    /**
     * To connect the billing client with Play Console
     */
    public final BillingConnector connect() {

        List<QueryProductDetailsParams.Product> productInAppList = new ArrayList<>();
        List<QueryProductDetailsParams.Product> productSubsList = new ArrayList<>();

        //set empty list to null so we only have to deal with lists that are null or not empty
        if (consumableIds == null || consumableIds.isEmpty()) {
            consumableIds = null;
        } else {
            for (String item : consumableIds) {
                productInAppList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(item).setProductType(INAPP).build());
            }
        }

        if (nonConsumableIds == null || nonConsumableIds.isEmpty()) {
            nonConsumableIds = null;
        } else {
            for (String item : nonConsumableIds) {
                productInAppList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(item).setProductType(INAPP).build());
            }
        }

        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            subscriptionIds = null;
        } else {
            for (String item : subscriptionIds) {
                productSubsList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(item).setProductType(SUBS).build());
            }
        }

        allProductList.addAll(productInAppList);
        allProductList.addAll(productSubsList);

        //check if any list is provided
        if (allProductList.isEmpty()) {
            throw new IllegalArgumentException("At least one list of consumables, non-consumables or subscriptions is needed");
        }

        //check for duplicates SKU ids
        int allIdsSize = allProductList.size();
        int allIdsSizeDistinct = (int) allProductList.stream().distinct().count();
        if (allIdsSize != allIdsSizeDistinct) {
            throw new IllegalArgumentException("The SKU id must appear only once in a list. Also, it must not be in different lists");
        }

        Log("Billing service: connecting...");
        if (!billingClient.isReady()) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    isConnected = false;

                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_DISCONNECTED,
                            "Billing service: disconnected", defaultResponseCode)));

                    Log("Billing service: Trying to reconnect...");
                    retryBillingClientConnection();
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    isConnected = false;

                    switch (billingResult.getResponseCode()) {
                        case OK:
                            isConnected = true;
                            Log("Billing service: connected");

                            //query consumable and non-consumable SKU details
                            if (!productInAppList.isEmpty()) {
                                querySkuDetails(INAPP, productInAppList);
                            }

                            //query subscription SKU details
                            if (subscriptionIds != null) {
                                querySkuDetails(SUBS, productSubsList);
                            }
                            break;
                        case BILLING_UNAVAILABLE:
                            Log("Billing service: unavailable");
                            retryBillingClientConnection();
                            break;
                        default:
                            Log("Billing service: error");
                            retryBillingClientConnection();
                            break;
                    }
                }
            });
        }

        return this;
    }

    /**
     * Retries the billing client connection with exponential backoff
     * Max out at the time specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS (15 minutes)
     */
    private void retryBillingClientConnection() {
        findUiHandler().postDelayed(this::connect, reconnectMilliseconds);
        reconnectMilliseconds = Math.min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
    }

    /**
     * Fires a query in Play Console to show products available to purchase
     */
    private void querySkuDetails(String skuType, List<QueryProductDetailsParams.Product> productList) {
        QueryProductDetailsParams skuDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(productList).build();

        billingClient.queryProductDetailsAsync(skuDetailsParams, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == OK) {
                if (skuDetailsList != null && skuDetailsList.isEmpty()) {
                    Log("Query SKU Details: data not found. Make sure SKU ids are configured on Play Console");

                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                            "No SKU found", defaultResponseCode)));
                } else {
                    Log("Query SKU Details: data found");

                    if (skuDetailsList != null) {
                        List<ProductInfo> fetchedSkuInfo = skuDetailsList.stream().map(this::generateSkuInfo).collect(Collectors.toList());
                        fetchedSkuInfoList.addAll(fetchedSkuInfo);

                        switch (skuType) {
                            case INAPP:
                            case SUBS:
                                findUiHandler().post(() -> billingEventListener.onProductsFetched(fetchedSkuInfo));
                                break;
                            default:
                                throw new IllegalStateException("SKU type is not implemented");
                        }

                        List<String> fetchedSkuIds = fetchedSkuInfo.stream().map(ProductInfo::getSku).collect(Collectors.toList());
                        boolean isFetched = fetchedSkuIds.stream().anyMatch(productList::contains);

                        if (isFetched) {
                            fetchPurchasedProducts();
                        }

                    } else {
                        Log("Query SKU Details: SKU details list is null");
                    }
                }
            } else {
                Log("Query SKU Details: failed");
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR, billingResult)));
            }
        });
    }

    /**
     * Returns a new SkuInfo object containing the SKU type and SKU details
     *
     * @param productDetails - is the object provided by the billing client API
     */
    private ProductInfo generateSkuInfo(ProductDetails productDetails) {
        SkuProductType skuProductType;

        switch (productDetails.getProductType()) {
            case INAPP:
                boolean consumable = isSkuIdConsumable(productDetails.getProductId());
                if (consumable) {
                    skuProductType = SkuProductType.CONSUMABLE;
                } else {
                    skuProductType = SkuProductType.NON_CONSUMABLE;
                }
                break;
            case SUBS:
                skuProductType = SkuProductType.SUBSCRIPTION;
                break;
            default:
                throw new IllegalStateException("SKU type is not implemented correctly");
        }

        return new ProductInfo(skuProductType, productDetails);
    }

    private boolean isSkuIdConsumable(String productId) {
        if (consumableIds == null) {
            return false;
        }

        return consumableIds.contains(productId);
    }

    /**
     * Returns purchases details for currently owned items without a network request
     */
    private void fetchPurchasedProducts() {
        if (billingClient.isReady()) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(INAPP).build(),
                    (billingResult, purchases) -> {
                        if (billingResult.getResponseCode() == OK) {
                            if (purchases.isEmpty()) {
                                Log("Query IN-APP Purchases: the list is empty");
                            } else {
                                Log("Query IN-APP Purchases: data found and progress");
                            }

                            processPurchases(ProductType.INAPP, purchases, true);
                        } else {
                            Log("Query IN-APP Purchases: failed");
                        }
                    }
            );

            //query subscription purchases for supported devices
            if (isSubscriptionSupported() == SupportState.SUPPORTED) {
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(SUBS).build(),
                        (billingResult, purchases) -> {
                            if (billingResult.getResponseCode() == OK) {
                                if (purchases.isEmpty()) {
                                    Log("Query IN-APP Purchases: the list is empty");
                                } else {
                                    Log("Query IN-APP Purchases: data found and progress");
                                }

                                processPurchases(ProductType.SUBS, purchases, true);
                            } else {
                                Log("Query IN-APP Purchases: failed");
                            }
                        }
                );
            }

        } else {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR,
                    "Billing client is not ready yet", defaultResponseCode)));
        }
    }

    /**
     * Before using subscriptions, device-support must be checked
     * Not all devices support subscriptions
     */
    public SupportState isSubscriptionSupported() {
        BillingResult response = billingClient.isFeatureSupported(SUBSCRIPTIONS);

        switch (response.getResponseCode()) {
            case OK:
                Log("Subscriptions support check: success");
                return SupportState.SUPPORTED;
            case SERVICE_DISCONNECTED:
                Log("Subscriptions support check: disconnected. Trying to reconnect...");
                return SupportState.DISCONNECTED;
            default:
                Log("Subscriptions support check: error -> " + response.getResponseCode() + " " + response.getDebugMessage());
                return SupportState.NOT_SUPPORTED;
        }
    }

    /**
     * Checks purchases signature for more security
     */
    private void processPurchases(ProductType skuType, List<Purchase> allPurchases, boolean purchasedProductsFetched) {
        List<PurchaseInfo> signatureValidPurchases = new ArrayList<>();

        //create a list with signature valid purchases
        List<Purchase> validPurchases = allPurchases.stream().filter(this::isPurchaseSignatureValid).collect(Collectors.toList());
        for (Purchase purchase : validPurchases) {

            //query all SKUs as a list
            List<String> purchasesSkus = purchase.getSkus();

            //loop through all SKUs and progress for each SKU individually
            for (int i = 0; i < purchasesSkus.size(); i++) {
                String purchaseSku = purchasesSkus.get(i);

                Optional<ProductInfo> skuInfo = fetchedSkuInfoList.stream().filter(it -> it.getSku().equals(purchaseSku)).findFirst();
                if (skuInfo.isPresent()) {
                    ProductDetails skuDetails = skuInfo.get().getSkuDetails();

                    PurchaseInfo purchaseInfo = new PurchaseInfo(generateSkuInfo(skuDetails), purchase);
                    signatureValidPurchases.add(purchaseInfo);

                }
            }
        }

        if (purchasedProductsFetched) {
            fetchedPurchasedProducts = true;
            findUiHandler().post(() -> billingEventListener.onPurchasedProductsFetched(skuType, signatureValidPurchases));
        } else {
            findUiHandler().post(() -> billingEventListener.onProductsPurchased(signatureValidPurchases));
        }

        purchasedProductsList.addAll(signatureValidPurchases);

        for (PurchaseInfo purchaseInfo : signatureValidPurchases) {
            if (shouldAutoConsume) {
                consumePurchase(purchaseInfo);
            }

            if (shouldAutoAcknowledge) {
                boolean isSkuConsumable = purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE;
                if (!isSkuConsumable) {
                    acknowledgePurchase(purchaseInfo);
                }
            }
        }
    }

    /**
     * Consume consumable products so that the user can buy the item again
     * <p>
     * Consumable products might be bought/consumed by users multiple times (for eg. diamonds, coins etc)
     * They have to be consumed within 3 days otherwise Google will refund the products
     */
    public void consumePurchase(PurchaseInfo purchaseInfo) {
        if (checkSkuBeforeInteraction(purchaseInfo.getSku())) {
            if (purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE) {
                if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken()).build();

                    billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                        if (billingResult.getResponseCode() == OK) {
                            purchasedProductsList.remove(purchaseInfo);
                            findUiHandler().post(() -> billingEventListener.onPurchaseConsumed(purchaseInfo));
                        } else {
                            Log("Handling consumables: error during consumption attempt: " + billingResult.getDebugMessage());

                            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.CONSUME_ERROR, billingResult)));
                        }
                    });
                } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    Log("Handling consumables: purchase can not be consumed because the state is PENDING. " +
                            "A purchase can be consumed only when the state is PURCHASED");

                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CONSUME_WARNING,
                            "Warning: purchase can not be consumed because the state is PENDING. Please consume the purchase later", defaultResponseCode)));
                }
            }
        }
    }

    /**
     * Acknowledge non-consumable products & subscriptions
     * <p>
     * This will avoid refunding for these products to users by Google
     */
    public void acknowledgePurchase(PurchaseInfo purchaseInfo) {
        if (checkSkuBeforeInteraction(purchaseInfo.getSku())) {
            switch (purchaseInfo.getSkuProductType()) {
                case NON_CONSUMABLE:
                case SUBSCRIPTION:
                    if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        if (!purchaseInfo.getPurchase().isAcknowledged()) {
                            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken()).build();

                            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                                if (billingResult.getResponseCode() == OK) {
                                    findUiHandler().post(() -> billingEventListener.onPurchaseAcknowledged(purchaseInfo));
                                } else {
                                    Log("Handling acknowledges: error during acknowledgment attempt: " + billingResult.getDebugMessage());

                                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                                            new BillingResponse(ErrorType.ACKNOWLEDGE_ERROR, billingResult)));
                                }
                            });
                        }
                    } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        Log("Handling acknowledges: purchase can not be acknowledged because the state is PENDING. " +
                                "A purchase can be acknowledged only when the state is PURCHASED");

                        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.ACKNOWLEDGE_WARNING,
                                "Warning: purchase can not be acknowledged because the state is PENDING. Please acknowledge the purchase later", defaultResponseCode)));
                    }
                    break;
            }
        }
    }

    /**
     * Called to purchase a non-consumable/consumable product
     */
    public final void purchase(Activity activity, String productId) {
        purchase(activity, productId, 0);
    }

    /**
     * Called to purchase a non-consumable/consumable product
     * <p>
     * The offset Index represents the different offers in the subscription.
     */
    private void purchase(Activity activity, String productId, int offerIndex) {
        if (checkSkuBeforeInteraction(productId)) {
            Optional<ProductInfo> skuInfo = fetchedSkuInfoList.stream().filter(it -> it.getSku().equals(productId)).findFirst();
            if (skuInfo.isPresent()) {

                ProductDetails productDetails = skuInfo.get().getSkuDetails();

                //The offset Index represents the different offers in the subscription. (after Google Billing v5+)
                String offerToken = productDetails
                        .getSubscriptionOfferDetails()
                        .get(offerIndex)
                        .getOfferToken();

                List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                        List.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(offerToken).build());

                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();
                billingClient.launchBillingFlow(activity, billingFlowParams);
            } else {
                Log("Billing client can not launch billing flow because SKU details are missing");
            }
        }
    }

    /**
     * Called to purchase a subscription
     * <p>
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     *
     * If there is only one base package, offerIndex = 0
     */
    public final void subscribe(Activity activity, String productId, int offerIndex) {
        purchase(activity, productId, offerIndex);
    }

    /**
     * Called to cancel a subscription
     */
    public final void unsubscribe(Activity activity, String productId) {
        try {
            String subscriptionUrl = "http://play.google.com/store/account/subscriptions?package=" + activity.getPackageName() + "&sku=" + productId;

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(subscriptionUrl));

            activity.startActivity(intent);
            activity.finish();
        } catch (Exception e) {
            Log("Handling subscription cancellation: error while trying to unsubscribe");
            e.printStackTrace();
        }

    }

    /**
     * Checks purchase state synchronously
     */
    public final PurchasedResult isPurchased(ProductInfo skuInfo) {
        return checkPurchased(skuInfo.getSku());
    }

    private PurchasedResult checkPurchased(String productId) {
        if (!isReady()) {
            return PurchasedResult.CLIENT_NOT_READY;
        } else if (!fetchedPurchasedProducts) {
            return PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET;
        } else {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getSku().equals(productId)) {
                    return PurchasedResult.YES;
                }
            }
            return PurchasedResult.NO;
        }
    }

    /**
     * Checks purchase signature validity
     */
    private boolean isPurchaseSignatureValid(Purchase purchase) {
        return Security.verifyPurchase(base64Key, purchase.getOriginalJson(), purchase.getSignature());
    }

    /**
     * Returns the main thread for operations that need to be executed on the UI thread
     * <p>
     * BillingEventListener runs on it
     */
    private Handler findUiHandler() {
        return new Handler(Looper.getMainLooper());
    }

    /**
     * To print a log while debugging BillingConnector
     */
    private void Log(String debugMessage) {
        if (shouldEnableLogging) {
            Log.d(TAG, debugMessage);
        }
    }

    /**
     * Called to release the BillingClient instance
     * <p>
     * To avoid leaks this method should be called when BillingConnector is no longer needed
     */
    public void release() {
        if (billingClient != null && billingClient.isReady()) {
            Log("BillingConnector instance release: ending connection...");
            billingClient.endConnection();
        }
    }
}