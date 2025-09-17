package games.moisoni.google_iab;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.NETWORK_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.android.billingclient.api.BillingClient.FeatureType.SUBSCRIPTIONS;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.BillingClient.ProductType.SUBS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import games.moisoni.google_iab.enums.ErrorType;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.enums.PurchasedResult;
import games.moisoni.google_iab.enums.SkuProductType;
import games.moisoni.google_iab.enums.SupportState;
import games.moisoni.google_iab.listeners.AcknowledgeEventListener;
import games.moisoni.google_iab.listeners.BillingEventListener;
import games.moisoni.google_iab.listeners.ConsumeEventListener;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.ProductInfo;
import games.moisoni.google_iab.models.PurchaseInfo;

public class BillingConnector implements DefaultLifecycleObserver {

    private final Handler uiHandler;

    private static final String TAG = "BillingConnector";
    private static final int defaultResponseCode = 99; //custom response code not used by the official BillingClient API

    private static final int notAnOffer = -1;

    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L;
    private final AtomicLong reconnectMilliseconds = new AtomicLong(RECONNECT_TIMER_START_MILLISECONDS);

    private static final int MAX_PENDING_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000L;
    private static final long MAX_RETRY_DELAY_MS = 10000L;
    private static final long MAX_PENDING_DURATION_MS = 1000 * 60 * 5;

    private final String base64Key;

    private final Context context;
    private Lifecycle lifecycle;

    private BillingClient billingClient;
    private BillingEventListener billingEventListener;

    private List<String> consumableIds;
    private List<String> nonConsumableIds;
    private List<String> subscriptionIds;

    private final List<QueryProductDetailsParams.Product> allProductList = new ArrayList<>();

    private final List<ProductInfo> fetchedProductInfoList = new ArrayList<>();
    private final List<PurchaseInfo> purchasedProductsList = new ArrayList<>();

    private final Object purchasedProductsSync = new Object(); //object for thread safety

    private int productDetailsQueriesPending;
    private int purchaseQueriesPending;

    private boolean shouldAutoAcknowledge = false;
    private boolean shouldAutoConsume = false;
    private boolean shouldEnableLogging = false;

    private volatile boolean isConnected = false;
    private volatile boolean fetchedPurchasedProducts = false;

    /**
     * BillingConnector public constructor
     *
     * @param context   - is the application context
     * @param base64Key - is the public developer key from Play Console
     * @param lifecycle - (optional) the lifecycle object to automatically manage the BillingConnector's
     *                  lifecycle. If provided, the connector will automatically handle connection
     *                  cleanup when the lifecycle owner is destroyed. Can be null if manual lifecycle
     *                  management is preferred.
     */
    public BillingConnector(@NonNull Context context, String base64Key, @Nullable Lifecycle lifecycle) {
        this.context = context.getApplicationContext();
        this.base64Key = base64Key;
        if (lifecycle != null) {
            this.lifecycle = lifecycle;
            lifecycle.addObserver(this);
        }
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.init();
    }

    /**
     * To initialize BillingConnector
     */
    private void init() {
        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enablePrepaidPlans().enableOneTimeProducts().build())
                .setListener(this::onPurchasesUpdated)
                .build();
    }

    private void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        switch (billingResult.getResponseCode()) {
            case OK:
                if (purchases != null) {
                    processPurchases(ProductType.COMBINED, purchases, false);
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
                Log("Initialization error: service disconnected/timeout. Trying to reconnect...");
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                        new BillingResponse(ErrorType.CLIENT_DISCONNECTED, billingResult)));
                break;
            case NETWORK_ERROR:
                Log("Initialization error: service network error. Trying to reconnect...");
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                        new BillingResponse(ErrorType.NETWORK_ERROR, billingResult)));
                break;
            default:
                Log("Initialization error: " + new BillingResponse(ErrorType.BILLING_ERROR, billingResult));
                break;
        }
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

        return isConnected && billingClient.isReady() && !fetchedProductInfoList.isEmpty();
    }

    /**
     * Returns a boolean state of the product
     *
     * @param productId - is the product id that has to be checked
     */
    private boolean checkProductBeforeInteraction(String productId) {
        if (!isReady()) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_NOT_READY,
                    "Client is not ready yet", defaultResponseCode)));
            return false;
        }

        boolean productExists = false;
        if (productId != null) {
            for (ProductInfo productInfo : fetchedProductInfoList) {
                if (productInfo.getProduct().equals(productId)) {
                    productExists = true;
                    break;
                }
            }
        }

        if (productId != null && !productExists) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PRODUCT_NOT_EXIST,
                    "The product id: " + productId + " doesn't seem to exist on Play Console", defaultResponseCode)));
            return false;
        }
        return true;
    }

    /**
     * To connect the billing client with Play Console
     */
    public final BillingConnector connect() {
        if (!isPlayStoreInstalled(context)) {
            findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PLAY_STORE_NOT_INSTALLED,
                    "Google Play Store is not installed", BILLING_UNAVAILABLE)));
            return this;
        }

        List<QueryProductDetailsParams.Product> productInAppList = new ArrayList<>();
        List<QueryProductDetailsParams.Product> productSubsList = new ArrayList<>();

        //set empty list to null so we only have to deal with lists that are null or not empty
        if (consumableIds == null || consumableIds.isEmpty()) {
            consumableIds = null;
        } else {
            for (String id : consumableIds) {
                productInAppList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(INAPP).build());
            }
        }

        if (nonConsumableIds == null || nonConsumableIds.isEmpty()) {
            nonConsumableIds = null;
        } else {
            for (String id : nonConsumableIds) {
                productInAppList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(INAPP).build());
            }
        }

        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            subscriptionIds = null;
        } else {
            for (String id : subscriptionIds) {
                productSubsList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(SUBS).build());
            }
        }

        allProductList.addAll(productInAppList);
        allProductList.addAll(productSubsList);

        int queryCount = 0;
        if (!productInAppList.isEmpty()) queryCount++;
        if (!productSubsList.isEmpty()) queryCount++;
        productDetailsQueriesPending = queryCount;

        //check if any list is provided
        if (allProductList.isEmpty()) {
            throw new IllegalArgumentException("At least one list of consumables, non-consumables or subscriptions is needed");
        }

        //check for duplicates product ids
        int allIdsSize = allProductList.size();
        int allIdsSizeDistinct = new HashSet<>(allProductList).size();
        if (allIdsSize != allIdsSizeDistinct) {
            throw new IllegalArgumentException("The product id must appear only once in a list. Also, it must not be in different lists");
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
                    switch (billingResult.getResponseCode()) {
                        case OK:
                            isConnected = true;
                            Log("Billing service: connected");

                            //query consumable and non-consumable product details
                            if (!productInAppList.isEmpty()) {
                                queryProductDetails(INAPP, productInAppList);
                            }

                            //query subscription product details
                            if (subscriptionIds != null) {
                                queryProductDetails(SUBS, productSubsList);
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
        long currentDelay = reconnectMilliseconds.get();
        findUiHandler().postDelayed(this::connect, currentDelay);

        long currentVal, newVal;
        do {
            currentVal = reconnectMilliseconds.get();
            newVal = Math.min(currentVal * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
        } while (!reconnectMilliseconds.compareAndSet(currentVal, newVal));
    }

    /**
     * Fires a query in Play Console to show products available to purchase
     */
    private void queryProductDetails(String productType, List<QueryProductDetailsParams.Product> productList) {
        QueryProductDetailsParams productDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(productList).build();

        billingClient.queryProductDetailsAsync(productDetailsParams, (billingResult, productDetailsResult) -> {
            if (billingResult.getResponseCode() == OK) {
                List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();

                HashSet<String> foundProductIds = new HashSet<>();
                for (ProductDetails details : productDetailsList) {
                    foundProductIds.add(details.getProductId());
                }

                for (QueryProductDetailsParams.Product requestedProduct : productList) {
                    String productId = requestedProduct.zza(); // .zza() gets the product ID string
                    if (!foundProductIds.contains(productId)) {
                        Log("Error: Product ID '" + productId + "' not found. " +
                                "Make sure it is configured correctly in the Play Console");
                        findUiHandler().post(() -> billingEventListener.onProductQueryError(productId, new BillingResponse(ErrorType.PRODUCT_ID_QUERY_FAILED,
                                "Product ID '" + productId + "' not found", defaultResponseCode)
                        ));
                    }
                }

                if (productDetailsList.isEmpty()) {
                    Log("Query Product Details: No valid products found. Make sure product ids are configured on Play Console");
                    findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                            "No products found", defaultResponseCode)));
                } else {
                    Log("Query Product Details: data found for " + productDetailsList.size() + " products");

                    List<ProductInfo> fetchedProductInfo = new ArrayList<>();
                    for (ProductDetails productDetails : productDetailsList) {
                        fetchedProductInfo.add(generateProductInfo(productDetails));
                    }
                    fetchedProductInfoList.addAll(fetchedProductInfo);

                    switch (productType) {
                        case INAPP:
                        case SUBS:
                            findUiHandler().post(() -> billingEventListener.onProductsFetched(fetchedProductInfo));
                            break;
                        default:
                            throw new IllegalStateException("Product type is not implemented");
                    }

                    if (--productDetailsQueriesPending == 0) {
                        fetchPurchasedProducts();
                    }
                }
            } else {
                Log("Query Product Details: failed with response code: " + billingResult.getResponseCode());
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this,
                        new BillingResponse(ErrorType.BILLING_ERROR, billingResult)));
            }
        });
    }

    /**
     * Returns a new ProductInfo object containing the product type and product details
     *
     * @param productDetails - is the object provided by the billing client API
     */
    @NonNull
    private ProductInfo generateProductInfo(@NonNull ProductDetails productDetails) {
        SkuProductType skuProductType;

        switch (productDetails.getProductType()) {
            case INAPP:
                boolean consumable = isProductIdConsumable(productDetails.getProductId());
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
                throw new IllegalStateException("Product type is not implemented correctly");
        }

        return new ProductInfo(skuProductType, productDetails);
    }

    private boolean isProductIdConsumable(String productId) {
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
            purchaseQueriesPending = 1;
            if (isSubscriptionSupported() == SupportState.SUPPORTED) {
                purchaseQueriesPending++;
            }

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
                                    Log("Query SUBS Purchases: the list is empty");
                                } else {
                                    Log("Query SUBS Purchases: data found and progress");
                                }

                                processPurchases(ProductType.SUBS, purchases, true);
                            } else {
                                Log("Query SUBS Purchases: failed");
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
        SupportState state;

        switch (response.getResponseCode()) {
            case OK:
                Log("Subscriptions support check: success");
                state = SupportState.SUPPORTED;
                break;
            case SERVICE_DISCONNECTED:
                Log("Subscriptions support check: disconnected. Trying to reconnect...");
                state = SupportState.DISCONNECTED;
                break;
            default:
                Log("Subscriptions support check: error -> " + response.getResponseCode() + " " + response.getDebugMessage());
                state = SupportState.NOT_SUPPORTED;
                break;
        }
        return state;
    }

    /**
     * Checks purchases signature for more security
     */
    private void processPurchases(ProductType productType, @NonNull List<Purchase> allPurchases, boolean purchasedProductsFetched) {
        List<PurchaseInfo> signatureValidPurchases = new ArrayList<>();

        List<Purchase> validPurchases = new ArrayList<>();
        for (Purchase purchase : allPurchases) {
            if (isPurchaseSignatureValid(purchase)) {
                validPurchases.add(purchase);
            }
        }

        Map<String, ProductInfo> productInfoMap = new HashMap<>();
        for (ProductInfo productInfo : fetchedProductInfoList) {
            productInfoMap.put(productInfo.getProduct(), productInfo);
        }

        for (Purchase purchase : validPurchases) {
            for (String productId : purchase.getProducts()) {
                ProductInfo foundProductInfo = productInfoMap.get(productId);
                if (foundProductInfo != null) {
                    PurchaseInfo purchaseInfo = new PurchaseInfo(foundProductInfo, purchase);
                    signatureValidPurchases.add(purchaseInfo);
                }
            }
        }

        //synchronize access to purchasedProductsList
        synchronized (purchasedProductsSync) {
            //clear existing purchases of this type when fetching (to avoid duplicates)
            if (purchasedProductsFetched) {
                Iterator<PurchaseInfo> iterator = purchasedProductsList.iterator();
                while (iterator.hasNext()) {
                    PurchaseInfo purchaseInfo = iterator.next();
                    boolean isSubscription = purchaseInfo.getSkuProductType() == SkuProductType.SUBSCRIPTION;

                    if (productType == ProductType.SUBS && isSubscription) {
                        iterator.remove();
                    } else if (productType == ProductType.INAPP && !isSubscription) {
                        iterator.remove();
                    } else if (productType == ProductType.COMBINED) {
                        iterator.remove();
                    }
                }
            }

            //add new purchases
            purchasedProductsList.addAll(signatureValidPurchases);
        }

        if (purchasedProductsFetched) {
            findUiHandler().post(() -> billingEventListener.onPurchasedProductsFetched(productType, signatureValidPurchases));
            if (--purchaseQueriesPending == 0) {
                fetchedPurchasedProducts = true;
            }
        } else {
            findUiHandler().post(() -> billingEventListener.onProductsPurchased(signatureValidPurchases));
        }

        for (PurchaseInfo purchaseInfo : signatureValidPurchases) {
            if (shouldAutoConsume) {
                consumePurchase(purchaseInfo);
            }
            if (shouldAutoAcknowledge) {
                boolean isProductConsumable = purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE;
                if (!isProductConsumable) {
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
    public void consumePurchase(@NonNull PurchaseInfo purchaseInfo) {
        if (checkProductBeforeInteraction(purchaseInfo.getProduct())) {
            if (purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE) {
                if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken()).build();

                    billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                        if (billingResult.getResponseCode() == OK) {
                            synchronized (purchasedProductsSync) {
                                purchasedProductsList.remove(purchaseInfo);
                            }
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
    public void acknowledgePurchase(@NonNull PurchaseInfo purchaseInfo) {
        if (checkProductBeforeInteraction(purchaseInfo.getProduct())) {
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
        purchase(activity, productId, notAnOffer);
    }

    /**
     * Called to purchase a non-consumable/consumable product
     * <p>
     * The offer index represents the different offers in the subscription
     */
    private void purchase(Activity activity, String productId, int selectedOfferIndex) {
        if (checkProductBeforeInteraction(productId)) {
            ProductInfo foundProductInfo = null;
            for (ProductInfo productInfo : fetchedProductInfoList) {
                if (productInfo.getProduct().equals(productId)) {
                    foundProductInfo = productInfo;
                    break;
                }
            }

            if (foundProductInfo != null) {
                ProductDetails productDetails = foundProductInfo.getProductDetails();
                ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList;

                if (productDetails.getProductType().equals(SUBS)) {
                    List<ProductDetails.SubscriptionOfferDetails> offerDetails = productDetails.getSubscriptionOfferDetails();
                    if (offerDetails != null && selectedOfferIndex >= 0 && selectedOfferIndex < offerDetails.size()) {
                        //the offer index represents the different offers in the subscription
                        //offer index is only available for subscriptions starting with Google Billing v5+
                        productDetailsParamsList = ImmutableList.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(offerDetails.get(selectedOfferIndex).getOfferToken())
                                        .build()
                        );
                    }
                    //handle invalid selectedOfferIndex for subscriptions
                    else {
                        Log("Invalid selectedOfferIndex: " + selectedOfferIndex + " for product: " + productId +
                                ". Offer details size: " + (offerDetails != null ? offerDetails.size() : "null"));
                        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                                "Invalid subscription offer index provided", defaultResponseCode)));
                        return; //prevent proceeding with an invalid index
                    }
                }
                //handle IN-APP products (consumable or non-consumable)
                else {
                    productDetailsParamsList = ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                    );
                }

                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build();

                billingClient.launchBillingFlow(activity, billingFlowParams);
            } else {
                Log("Billing client can not launch billing flow because product details are missing for product: " + productId);
                findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PRODUCT_NOT_EXIST,
                        "Product details not found for " + productId, defaultResponseCode)));
            }
        }
    }

    /**
     * Verifies if a purchase still exists in the purchased products list
     *
     * @param purchaseInfo - the purchase to verify
     * @return true if purchase exists and is still pending, false otherwise
     */
    private boolean verifyPurchaseState(PurchaseInfo purchaseInfo) {
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo info : purchasedProductsList) {
                if (info.getPurchase().getPurchaseToken().equals(purchaseInfo.getPurchase().getPurchaseToken())) {
                    return true;
                }
            }
        }

        Log("Pending purchase no longer exists: " + purchaseInfo.getProduct());
        notifyBillingError(ErrorType.PENDING_PURCHASE_CANCELED,
                "Pending purchase was removed");
        return false;
    }

    /**
     * Retries a pending purchase for the given product ID
     * <p>
     * Checks if the product is in a pending state
     * <p>
     * Retries with exponential backoff (max 3 retries)
     * <p>
     * Notifies listener of success/failure
     *
     * @param productId - the product ID to retry
     */
    public void retryPendingPurchase(String productId) {
        if (!isReady()) {
            Log("Cannot retry pending purchase: Billing client is not ready");
            notifyBillingError(ErrorType.CLIENT_NOT_READY, "Billing client is not ready");
            return;
        }

        //synchronize the entire check to prevent races
        PurchaseInfo pendingPurchase = null;
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId) && purchaseInfo.isPending()) {
                    pendingPurchase = purchaseInfo;
                    break;
                }
            }
        }

        if (pendingPurchase == null || !pendingPurchase.isPending()) {
            Log("No pending purchase found for product: " + productId);
            notifyBillingError(ErrorType.NOT_PENDING, "No pending purchase for: " + productId);
            return;
        }

        retryPurchaseWithBackoff(pendingPurchase, 0, System.currentTimeMillis());
    }

    /**
     * Retries a pending purchase with exponential backoff
     * Includes acknowledgment and consume retry logic for completed purchases
     *
     * @param purchaseInfo - the pending purchase to retry
     * @param retryCount   - current retry attempt (starts at 0)
     */
    private void retryPurchaseWithBackoff(PurchaseInfo purchaseInfo, int retryCount, long startTime) {
        if (shouldStopRetrying(purchaseInfo, retryCount, startTime)) {
            handleRetryFailure(purchaseInfo);
            return;
        }

        long delayMs = calculateRetryDelay(retryCount);
        Log("Retrying pending purchase (" + (retryCount + 1) +
                "/" + MAX_PENDING_RETRIES + ") for: " + purchaseInfo.getProduct());

        findUiHandler().postDelayed(() -> {
            boolean shouldContinue = verifyPurchaseState(purchaseInfo);
            if (!shouldContinue) return;

            queryPurchasesForRetry(purchaseInfo, retryCount, startTime);
        }, delayMs);
    }

    /**
     * Queries purchases from Google Play for retry attempt
     *
     * @param purchaseInfo - the pending purchase being retried
     * @param retryCount   - current number of retry attempts
     * @param startTime    - timestamp when retries began (in milliseconds)
     */
    private void queryPurchasesForRetry(@NonNull PurchaseInfo purchaseInfo, int retryCount, long startTime) {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(purchaseInfo.getSkuProductType() ==
                                SkuProductType.SUBSCRIPTION ? SUBS : INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() != OK) {
                        Log("Failed to query purchases during retry: " +
                                billingResult.getDebugMessage());
                        retryPurchaseWithBackoff(purchaseInfo,
                                retryCount + 1,
                                startTime);
                        return;
                    }

                    handlePurchaseQueryResult(purchaseInfo, purchases, retryCount, startTime);
                });
    }

    /**
     * Handles the result of a purchase query during retry attempt
     *
     * @param originalInfo - the original pending purchase info
     * @param purchases    - list of purchases returned from query
     * @param retryCount   - current number of retry attempts
     * @param startTime    - timestamp when retries began (in milliseconds)
     */
    private void handlePurchaseQueryResult(PurchaseInfo originalInfo, @NonNull List<Purchase> purchases, int retryCount, long startTime) {
        Purchase completedPurchase = null;
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseToken().equals(originalInfo.getPurchase().getPurchaseToken())) {
                completedPurchase = purchase;
                break;
            }
        }

        if (completedPurchase == null) {
            Log("Pending purchase not found, may have been canceled: " +
                    originalInfo.getProduct());
            notifyBillingError(ErrorType.PENDING_PURCHASE_CANCELED,
                    "Pending purchase may have been canceled");
            return;
        }

        if (completedPurchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Log("Pending purchase completed: " + originalInfo.getProduct());
            handleCompletedPurchase(originalInfo, completedPurchase);
        } else {
            retryPurchaseWithBackoff(originalInfo, retryCount + 1, startTime);
        }
    }

    /**
     * Acknowledges a purchase with retry logic
     *
     * @param purchaseInfo - the purchase to acknowledge
     * @param retryCount   - current retry attempt
     * @param maxRetries   - maximum number of retries
     * @param listener     - to handle success/failure
     */
    private void acknowledgePurchaseWithRetry(@NonNull PurchaseInfo purchaseInfo, int retryCount, int maxRetries, AcknowledgeEventListener listener) {
        if (retryCount >= maxRetries) {
            Log("Max retries reached for acknowledgment: " + purchaseInfo.getProduct());
            listener.onFailure();
            return;
        }

        long delayMs = Math.min(INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount), MAX_RETRY_DELAY_MS);

        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == OK) {
                Log("Acknowledgment successful for: " + purchaseInfo.getProduct());
                listener.onSuccess();
            } else {
                Log("Acknowledgment failed (attempt " + (retryCount + 1) +
                        "/" + maxRetries + ") for: " + purchaseInfo.getProduct() +
                        " - " + billingResult.getDebugMessage());

                findUiHandler().postDelayed(() -> acknowledgePurchaseWithRetry(purchaseInfo, retryCount + 1, maxRetries, listener), delayMs);
            }
        });
    }

    /**
     * Consumes a purchase with retry logic
     *
     * @param purchaseInfo - the purchase to consume
     * @param retryCount   - current retry attempt
     * @param maxRetries   - maximum number of retries
     * @param listener     - to handle success/failure
     */
    private void consumeWithRetry(@NonNull PurchaseInfo purchaseInfo, int retryCount, int maxRetries, @NonNull ConsumeEventListener listener) {
        if (retryCount >= maxRetries) {
            Log("Max consume retries reached for: " + purchaseInfo.getProduct());
            listener.onFailure();
            return;
        }

        long delayMs = Math.min(INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount), MAX_RETRY_DELAY_MS);

        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseInfo.getPurchase().getPurchaseToken())
                .build();

        billingClient.consumeAsync(params, (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == OK) {
                Log("Consume success for: " + purchaseInfo.getProduct());
                listener.onSuccess();
            } else {
                Log("Consume failed (attempt " + (retryCount + 1) +
                        "/" + maxRetries + "): " + billingResult.getDebugMessage());

                findUiHandler().postDelayed(() -> consumeWithRetry(purchaseInfo, retryCount + 1, maxRetries, listener), delayMs);
            }
        });
    }

    /**
     * Handles a completed purchase (state changed from PENDING to PURCHASED)
     * Includes consume & acknowledgment retry logic with strict state validation
     */
    private void handleCompletedPurchase(@NonNull PurchaseInfo originalInfo, @NonNull Purchase completedPurchase) {
        //initial state verification
        if (completedPurchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            Log("Attempted to handle NON-PURCHASED item: " + completedPurchase.getPurchaseState() +
                    " for product: " + originalInfo.getProduct());
            return;
        }

        //verify purchase token matches
        if (!completedPurchase.getPurchaseToken().equals(originalInfo.getPurchase().getPurchaseToken())) {
            Log("Purchase token mismatch for product: " + originalInfo.getProduct());
            notifyBillingError(ErrorType.DEVELOPER_ERROR, "Purchase verification failed");
            return;
        }

        //synchronized block for thread-safe processing
        synchronized (purchasedProductsSync) {
            //ensure original pending entry is removed when a pending purchase completes
            Iterator<PurchaseInfo> iterator = purchasedProductsList.iterator();
            while (iterator.hasNext()) {
                PurchaseInfo purchaseInfo = iterator.next();
                if (purchaseInfo.getPurchase().getPurchaseToken().equals(originalInfo.getPurchase().getPurchaseToken()) && purchaseInfo.isPending()) {
                    iterator.remove();
                    break;
                }
            }

            //re-verify state after synchronization
            if (completedPurchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                Log("Purchase state changed during processing: " +
                        completedPurchase.getPurchaseState() +
                        " for product: " + originalInfo.getProduct());
                return;
            }

            PurchaseInfo completedPurchaseInfo = new PurchaseInfo(originalInfo.getProductInfo(), completedPurchase);

            //process the completed purchase
            processPurchases(
                    originalInfo.getSkuProductType() == SkuProductType.SUBSCRIPTION ?
                            ProductType.SUBS : ProductType.INAPP,
                    Collections.singletonList(completedPurchase),
                    false
            );

            //handle auto-consume for consumables
            if (shouldAutoConsume && originalInfo.getSkuProductType() == SkuProductType.CONSUMABLE) {
                consumeWithRetry(completedPurchaseInfo, 0, 3, new ConsumeEventListener() {
                    @Override
                    public void onSuccess() {
                        synchronized (purchasedProductsSync) {
                            purchasedProductsList.remove(completedPurchaseInfo);
                        }
                        findUiHandler().post(() ->
                                billingEventListener.onPurchaseConsumed(completedPurchaseInfo));
                    }

                    @Override
                    public void onFailure() {
                        handleConsumeFailure(completedPurchaseInfo);
                    }
                });
            }
            //handle auto-acknowledge for non-consumables and subscriptions
            else if (shouldAutoAcknowledge && !completedPurchase.isAcknowledged()) {
                acknowledgePurchaseWithRetry(completedPurchaseInfo, 0, 3, new AcknowledgeEventListener() {
                    @Override
                    public void onSuccess() {
                        findUiHandler().post(() ->
                                billingEventListener.onPurchaseAcknowledged(completedPurchaseInfo));
                    }

                    @Override
                    public void onFailure() {
                        handleAcknowledgeFailure(completedPurchaseInfo);
                    }
                });
            }
        }
    }

    /**
     * Calculates the next retry delay using exponential backoff
     *
     * @param retryCount - current number of retry attempts
     * @return delay in milliseconds before next retry attempt
     */
    private long calculateRetryDelay(int retryCount) {
        return Math.min(INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount), MAX_RETRY_DELAY_MS);
    }

    /**
     * Determines if pending purchase retries should stop based on retry count and duration
     *
     * @param purchaseInfo - the purchase being retried
     * @param retryCount   - current number of retry attempts
     * @param startTime    - timestamp when retries began (in milliseconds)
     * @return true if retries should stop, false otherwise
     */
    private boolean shouldStopRetrying(PurchaseInfo purchaseInfo, int retryCount, long startTime) {
        if (retryCount >= MAX_PENDING_RETRIES) {
            Log("Max retry attempts reached for: " + purchaseInfo.getProduct());
            return true;
        }

        if (System.currentTimeMillis() - startTime > MAX_PENDING_DURATION_MS) {
            Log("Max retry duration exceeded for: " + purchaseInfo.getProduct());
            return true;
        }

        return false;
    }

    /**
     * Handles consumption failure events after all retry attempts are exhausted
     *
     * @param purchaseInfo - contains details about the purchase that failed consumption
     */
    private void handleConsumeFailure(@NonNull PurchaseInfo purchaseInfo) {
        Log("Consume failed for: " + purchaseInfo.getProduct());
        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CONSUME_ERROR,
                "Failed to consume  purchase", defaultResponseCode)));
    }

    /**
     * Handles acknowledgment failure events after all retry attempts are exhausted
     *
     * @param purchaseInfo - contains details about the purchase that failed acknowledgment
     */
    private void handleAcknowledgeFailure(@NonNull PurchaseInfo purchaseInfo) {
        Log("Acknowledge failed for: " + purchaseInfo.getProduct());
        findUiHandler().post(() -> billingEventListener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.ACKNOWLEDGE_ERROR,
                "Failed to acknowledge purchase", defaultResponseCode)));
    }

    /**
     * Handles failure case when max retries for a pending purchase are reached
     * <p>
     * Removes the failed purchase from the purchased products list and notifies listener
     *
     * @param purchaseInfo - the purchase that failed to complete
     */
    private void handleRetryFailure(@NonNull PurchaseInfo purchaseInfo) {
        Log("Max retries reached for pending purchase: " + purchaseInfo.getProduct());

        //synchronize access when removing failed purchase
        synchronized (purchasedProductsSync) {
            Iterator<PurchaseInfo> iterator = purchasedProductsList.iterator();
            while (iterator.hasNext()) {
                PurchaseInfo p = iterator.next();
                if (p.getPurchase().getPurchaseToken().equals(purchaseInfo.getPurchase().getPurchaseToken())) {
                    iterator.remove();
                    break;
                }
            }
        }

        notifyBillingError(ErrorType.PENDING_PURCHASE_RETRY_ERROR,
                "Pending purchase still not completed after " + MAX_PENDING_RETRIES + " retries");
    }

    /**
     * Notifies billing event listener about an error on the UI thread
     *
     * @param errorType - type of error that occurred
     * @param message   - descriptive error message
     */
    private void notifyBillingError(ErrorType errorType, String message) {
        findUiHandler().post(() -> {
            if (billingEventListener != null) {
                billingEventListener.onBillingError(BillingConnector.this,
                        new BillingResponse(errorType, message, defaultResponseCode));
            }
        });
    }


    /**
     * Called to purchase a subscription with offers
     * <p>
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     * <p>
     * For subscription with only one base package, use subscribe(activity, productId) method or selectedOfferIndex = 0
     */
    public final void subscribe(Activity activity, String productId, int selectedOfferIndex) {
        purchase(activity, productId, selectedOfferIndex);
    }

    /**
     * Called to purchase a simple subscription.
     * <p>
     * This method assumes the desired offer is the first one available (index 0).
     * For subscriptions with multiple offers, use subscribe(activity, productId, selectedOfferIndex).
     */
    public final void subscribe(Activity activity, String productId) {
        purchase(activity, productId, 0);
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
        } catch (Exception e) {
            Log("Handling subscription cancellation: error while trying to unsubscribe"
                    + "\nError: " + e.getMessage());
        }

    }

    /**
     * Checks if a subscription is currently active and auto-renewing
     *
     * @param productId - is the subscription product ID to check
     */
    public boolean isSubscriptionActive(String productId) {
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId))
                    return purchaseInfo.getPurchase().isAutoRenewing();
            }
        }
        return false;
    }

    /**
     * Checks if a purchase is in pending state
     * <p>
     * Pending purchases require completion through the Google Play Store
     * and will eventually transition to PURCHASED or canceled state
     *
     * @param productId - is the product ID to check
     */
    public boolean isPurchasePending(String productId) {
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId))
                    return purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING;
            }
        }
        return false;
    }

    /**
     * Checks if Google Play Store is installed on the device using a two-step verification:
     * 1. Checks for the Play Store package ("com.android.vending")
     * 2. Verifies if any app can handle Play Store URLs (fallback)
     * <p>
     * Will trigger both PLAY_STORE_NOT_INSTALLED and BILLING_UNAVAILABLE
     *
     * @param context - the application context
     * @return true if Play Store is installed, false otherwise
     */
    public boolean isPlayStoreInstalled(@NonNull Context context) {
        if (isPlayStoreInstalledByPackage(context)) {
            return true;
        }
        return canHandlePlayStoreUrl(context);
    }

    /**
     * Checks if Google Play Store is installed by verifying the existence of its package
     *
     * @param context - the application context
     * @return true if Play Store package exists, false otherwise
     */
    private boolean isPlayStoreInstalledByPackage(@NonNull Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log("Google Play Store is not installed");
            return false;
        }
    }

    /**
     * Checks if any app (ideally Play Store) can handle Play Store URLs as a fallback verification
     *
     * @param context - the application context
     * @return true if an app can handle Play Store URLs and is the actual Play Store, false otherwise
     */
    private boolean canHandlePlayStoreUrl(@NonNull Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store"));
        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null) {
            Log("Google Play Store is not installed");
            return false;
        }

        //verify if the resolver is actually the Play Store
        return "com.android.vending".equals(resolveInfo.activityInfo.packageName);
    }

    /**
     * Returns a list of all purchased products.
     */
    public List<PurchaseInfo> getPurchasedProductsList() {
        synchronized (purchasedProductsSync) {
            return List.copyOf(purchasedProductsList);
        }
    }

    /**
     * Checks purchase state synchronously
     */
    public final PurchasedResult isPurchased(@NonNull ProductInfo productInfo) {
        return checkPurchased(productInfo.getProduct());
    }

    private PurchasedResult checkPurchased(String productId) {
        if (!isReady()) {
            return PurchasedResult.CLIENT_NOT_READY;
        } else if (!fetchedPurchasedProducts) {
            return PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET;
        } else {
            synchronized (purchasedProductsSync) {
                for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                    if (purchaseInfo.getProduct().equals(productId)) {
                        return PurchasedResult.YES;
                    }
                }
            }
            return PurchasedResult.NO;
        }
    }

    /**
     * Checks purchase signature validity
     */
    private boolean isPurchaseSignatureValid(@NonNull Purchase purchase) {
        return Security.verifyPurchase(base64Key, purchase.getOriginalJson(), purchase.getSignature());
    }

    /**
     * Returns the main thread for operations that need to be executed on the UI thread
     * <p>
     * BillingEventListener runs on it
     */
    @NonNull
    private Handler findUiHandler() {
        return uiHandler;
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

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        release();
        if (lifecycle != null) {
            lifecycle.removeObserver(this);
        }
    }
}
