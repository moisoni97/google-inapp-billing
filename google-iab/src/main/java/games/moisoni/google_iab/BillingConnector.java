package games.moisoni.google_iab;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import games.moisoni.google_iab.listener.BillingEventAction;
import games.moisoni.google_iab.type.ErrorType;
import games.moisoni.google_iab.type.ProductType;
import games.moisoni.google_iab.status.PurchasedResult;
import games.moisoni.google_iab.type.SkuProductType;
import games.moisoni.google_iab.status.SupportState;
import games.moisoni.google_iab.listener.BillingEventListener;
import games.moisoni.google_iab.model.BillingResponse;
import games.moisoni.google_iab.model.ProductInfo;
import games.moisoni.google_iab.model.PurchaseInfo;

public class BillingConnector implements DefaultLifecycleObserver {

    private final Handler uiHandler;

    private static final String TAG = "BillingConnector";
    private static final int defaultResponseCode = 99; // Custom response code not used by the official BillingClient API

    private static final int notAnOffer = -1;

    private static final long RECONNECT_TIMER_START_MILLISECONDS = 1000L;
    private static final long RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L;
    private final AtomicLong reconnectMilliseconds = new AtomicLong(RECONNECT_TIMER_START_MILLISECONDS);

    private final String base64Key;

    private final Context context;
    private Lifecycle lifecycle;

    private BillingClient billingClient;
    private volatile BillingEventListener billingEventListener;
    private volatile boolean isReleased = false;

    private List<String> consumableIds;
    private List<String> nonConsumableIds;
    private List<String> subscriptionIds;

    private final List<String> allProductList = new ArrayList<>();

    private final List<ProductInfo> fetchedProductInfoList = new CopyOnWriteArrayList<>();
    private final List<PurchaseInfo> purchasedProductsList = new ArrayList<>();

    private final Object purchasedProductsSync = new Object(); // Object for thread safety

    private final AtomicInteger productDetailsQueriesPending = new AtomicInteger(0);

    private final AtomicInteger connectionGeneration = new AtomicInteger(0);

    // Purchase tokens with a consume/acknowledge request in progress or already completed by this instance
    // Prevents duplicate requests and callbacks when purchase flows overlap (e.g. a purchase update during a purchases query)
    private final Set<String> handledPurchaseTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private boolean shouldAutoAcknowledge = false;
    private boolean shouldAutoConsume = false;
    private boolean shouldEnableLogging = false;

    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    private volatile boolean isConnected = false;

    // Tracked per product type so one query does not wait for (or hide the failure of) the other
    // Only set by a successful query and never reset, so later refreshes keep the last known state
    private volatile boolean fetchedInAppPurchases = false;
    private volatile boolean fetchedSubsPurchases = false;

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
        int responseCode = billingResult.getResponseCode();

        if (responseCode == OK) {
            if (purchases != null) {
                processPurchases(ProductType.COMBINED, purchases, false);
            } else {
                Log("Purchase flow: finished with OK response but no purchases were returned");
            }
            return;
        }

        ErrorType errorType = findErrorType(responseCode);

        if (responseCode == USER_CANCELED) {
            Log("Purchase flow: user pressed back or canceled a dialog." + " Response code: " + responseCode);
        } else {
            Log("Purchase flow: failed -> " + new BillingResponse(errorType, billingResult));
        }

        postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                new BillingResponse(errorType, billingResult)));

        // Re-sync owned purchases, e.g. to consume a consumable that is still owned
        if (responseCode == ITEM_ALREADY_OWNED) {
            refreshPurchases();
        }
    }

    /**
     * To attach an event listener to establish a bridge with the caller
     */
    public final void setBillingEventListener(BillingEventListener billingEventListener) {
        this.billingEventListener = billingEventListener;
    }

    /**
     * To set consumable products IDs
     */
    public final BillingConnector setConsumableIds(List<String> consumableIds) {
        this.consumableIds = consumableIds != null ? new ArrayList<>(consumableIds) : null;
        return this;
    }

    /**
     * To set non-consumable products IDs
     */
    public final BillingConnector setNonConsumableIds(List<String> nonConsumableIds) {
        this.nonConsumableIds = nonConsumableIds != null ? new ArrayList<>(nonConsumableIds) : null;
        return this;
    }

    /**
     * To set subscription products IDs
     */
    public final BillingConnector setSubscriptionIds(List<String> subscriptionIds) {
        this.subscriptionIds = subscriptionIds != null ? new ArrayList<>(subscriptionIds) : null;
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
     * <p>
     * True when the billing client is connected and product details have been fetched
     */
    public final boolean isReady() {
        return isConnected && billingClient != null && billingClient.isReady() && !fetchedProductInfoList.isEmpty();
    }

    /**
     * Returns a boolean state of the product
     *
     * @param productId - is the product ID that has to be checked
     */
    private boolean checkProductBeforeInteraction(String productId) {
        if (!isReady()) {
            Log("Billing client is not ready yet: not connected or product details not fetched");
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_NOT_READY,
                    "Client is not ready yet", defaultResponseCode)));
            return false;
        }

        if (productId == null || productId.trim().isEmpty()) {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Product ID cannot be null or empty", defaultResponseCode)));
            return false;
        }

        boolean productExists = false;
        for (ProductInfo productInfo : fetchedProductInfoList) {
            if (productInfo.getProduct().equals(productId)) {
                productExists = true;
                break;
            }
        }

        if (!productExists) {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PRODUCT_NOT_EXIST,
                    "The product ID: " + productId + " doesn't seem to exist on Play Console", defaultResponseCode)));
            return false;
        }
        return true;
    }

    /**
     * Returns a boolean state of the purchase before consuming or acknowledging it
     * <p>
     * Only requires a connected billing client, not fetched product details,
     * so owned products without available details can still be consumed or acknowledged
     *
     * @param purchaseInfo - is the purchase that has to be checked
     */
    private boolean checkPurchaseBeforeInteraction(@NonNull PurchaseInfo purchaseInfo) {
        if (billingClient == null || !billingClient.isReady()) {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_NOT_READY,
                    "Client is not ready yet", defaultResponseCode)));
            return false;
        }

        String purchaseToken = purchaseInfo.getPurchaseToken();
        if (purchaseToken == null || purchaseToken.trim().isEmpty()) {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Purchase token cannot be null or empty", defaultResponseCode)));
            return false;
        }
        return true;
    }

    /**
     * Maps Google Billing response codes to ErrorType
     */
    private ErrorType findErrorType(int responseCode) {
        switch (responseCode) {
            case USER_CANCELED:
                return ErrorType.USER_CANCELED;
            case SERVICE_UNAVAILABLE:
                return ErrorType.SERVICE_UNAVAILABLE;
            case BILLING_UNAVAILABLE:
                return ErrorType.BILLING_UNAVAILABLE;
            case ITEM_UNAVAILABLE:
                return ErrorType.ITEM_UNAVAILABLE;
            case DEVELOPER_ERROR:
                return ErrorType.DEVELOPER_ERROR;
            case ERROR:
                return ErrorType.ERROR;
            case ITEM_ALREADY_OWNED:
                return ErrorType.ITEM_ALREADY_OWNED;
            case ITEM_NOT_OWNED:
                return ErrorType.ITEM_NOT_OWNED;
            case SERVICE_DISCONNECTED:
                return ErrorType.CLIENT_DISCONNECTED;
            case NETWORK_ERROR:
                return ErrorType.NETWORK_ERROR;
            case FEATURE_NOT_SUPPORTED:
                return ErrorType.FEATURE_NOT_SUPPORTED;
            default:
                return ErrorType.BILLING_ERROR;
        }
    }

    /**
     * To connect the billing client with Play Console
     */
    public final BillingConnector connect() {
        if (isReleased) {
            Log("Cannot connect: BillingConnector has already been released");
            return this;
        }

        if (isConnected || (billingClient != null && billingClient.isReady())) {
            Log("Billing service: already connected");
            return this;
        }

        // Atomically prevent concurrent connection attempts
        if (!isConnecting.compareAndSet(false, true)) {
            Log("Billing service: connection already in progress");
            return this;
        }

        if (!isPlayStoreInstalled(context)) {
            isConnecting.set(false);
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PLAY_STORE_NOT_INSTALLED,
                    "Google Play Store is not installed", BILLING_UNAVAILABLE)));
            return this;
        }

        List<String> productInAppList = getInAppProductIds();
        List<String> productSubsList = getSubsProductIds();

        // Clear the list to prevent duplicates during a reconnection attempt
        allProductList.clear();

        allProductList.addAll(productInAppList);
        allProductList.addAll(productSubsList);

        // Check if any list is provided
        if (allProductList.isEmpty()) {
            isConnecting.set(false);
            throw new IllegalArgumentException("At least one list of consumables, non-consumables or subscriptions is needed");
        }

        // Check for duplicates product IDs
        int allIdsSize = allProductList.size();
        int allIdsSizeDistinct = new HashSet<>(allProductList).size();
        if (allIdsSize != allIdsSizeDistinct) {
            isConnecting.set(false);
            throw new IllegalArgumentException("The product ID must appear only once in a list. Also, it must not be in different lists");
        }

        Log("Billing service: connecting...");
        try {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    isConnected = false;
                    isConnecting.set(false);

                    if (isReleased) {
                        return;
                    }

                    postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CLIENT_DISCONNECTED,
                            "Billing service: disconnected", defaultResponseCode)));

                    Log("Billing service: Trying to reconnect...");
                    retryBillingClientConnection();
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    isConnecting.set(false);

                    if (isReleased) {
                        return;
                    }

                    switch (billingResult.getResponseCode()) {
                        case OK:
                            // Start a new generation so late responses from a previous connection are ignored
                            int generation = connectionGeneration.incrementAndGet();

                            // Clear previously fetched products once so new queries accumulate cleanly for this connection
                            fetchedProductInfoList.clear();

                            int queryCount = 0;
                            if (!productInAppList.isEmpty()) queryCount++;
                            if (!productSubsList.isEmpty()) queryCount++;

                            // Set before isConnected, so a concurrent refreshPurchases() sees the sync in progress
                            productDetailsQueriesPending.set(queryCount);

                            isConnected = true;
                            Log("Billing service: connected");

                            // Reset the reconnect timer on successful connection
                            reconnectMilliseconds.set(RECONNECT_TIMER_START_MILLISECONDS);

                            // Query consumable and non-consumable product details
                            if (!productInAppList.isEmpty()) {
                                queryProductDetails(INAPP, productInAppList, generation);
                            }

                            // Query subscription product details
                            if (!productSubsList.isEmpty()) {
                                queryProductDetails(SUBS, productSubsList, generation);
                            }
                            break;
                        case BILLING_UNAVAILABLE:
                            Log("Billing service: unavailable");
                            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.BILLING_UNAVAILABLE, billingResult)));
                            retryBillingClientConnection();
                            break;
                        default:
                            Log("Billing service: error -> " + billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                    new BillingResponse(findErrorType(billingResult.getResponseCode()), billingResult)));
                            retryBillingClientConnection();
                            break;
                    }
                }
            });
        } catch (Exception e) {
            isConnecting.set(false);
            Log("Billing service: startConnection failed: " + e.getMessage());
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                    "Billing service connection failed: " + e.getMessage(), defaultResponseCode)));
        }

        return this;
    }

    /**
     * Returns the consumable and non-consumable product IDs
     */
    @NonNull
    private List<String> getInAppProductIds() {
        List<String> productInAppList = new ArrayList<>();

        if (consumableIds != null) {
            productInAppList.addAll(consumableIds);
        }

        if (nonConsumableIds != null) {
            productInAppList.addAll(nonConsumableIds);
        }
        return productInAppList;
    }

    /**
     * Returns the subscription product IDs
     */
    @NonNull
    private List<String> getSubsProductIds() {
        List<String> productSubsList = new ArrayList<>();

        if (subscriptionIds != null) {
            productSubsList.addAll(subscriptionIds);
        }
        return productSubsList;
    }

    /**
     * Re-syncs product details and owned purchases with Google Play
     * <p>
     * Called automatically when the lifecycle owner resumes (if a Lifecycle was provided to the constructor)
     * <p>
     * Re-queries product details of a product type that has none fetched (e.g. after a failed query while offline),
     * which then queries owned purchases. Otherwise, it queries owned purchases directly, so purchases completed
     * outside the app (e.g. PENDING payments that cleared) are acknowledged/consumed
     * <p>
     * Does nothing while the billing client is not connected, purchases are synced once the connection is established
     */
    public final void refreshPurchases() {
        if (isReleased) {
            return;
        }

        if (!isConnected || billingClient == null || !billingClient.isReady()) {
            Log("Refresh purchases: billing client is not connected yet, purchases will sync once connected");
            return;
        }

        List<String> productInAppList = getInAppProductIds();
        List<String> productSubsList = getSubsProductIds();

        boolean hasInAppDetails = false;
        boolean hasSubsDetails = false;
        for (ProductInfo productInfo : fetchedProductInfoList) {
            if (productInfo.getSkuProductType() == SkuProductType.SUBSCRIPTION) {
                hasSubsDetails = true;
            } else {
                hasInAppDetails = true;
            }
        }

        boolean shouldQueryInApp = !productInAppList.isEmpty() && !hasInAppDetails;
        boolean shouldQuerySubs = !productSubsList.isEmpty() && !hasSubsDetails;

        int queryCount = 0;
        if (shouldQueryInApp) queryCount++;
        if (shouldQuerySubs) queryCount++;

        // Skip while product details queries are running, they fetch owned purchases when they finish
        if (!productDetailsQueriesPending.compareAndSet(0, queryCount)) {
            Log("Refresh purchases: sync already in progress");
            return;
        }

        if (queryCount == 0) {
            Log("Refresh purchases: querying owned purchases...");
            fetchPurchasedProducts();
            return;
        }

        Log("Refresh purchases: re-querying missing product details...");

        int generation = connectionGeneration.get();

        if (shouldQueryInApp) {
            queryProductDetails(INAPP, productInAppList, generation);
        }

        if (shouldQuerySubs) {
            queryProductDetails(SUBS, productSubsList, generation);
        }
    }

    /**
     * Retries the billing client connection with exponential backoff
     * Max out at the time specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS (15 minutes)
     */
    private void retryBillingClientConnection() {
        if (isReleased) {
            return;
        }
        long currentDelay = reconnectMilliseconds.get();
        findUiHandler().postDelayed(() -> {
            if (!isReleased) {
                connect();
            }
        }, currentDelay);

        long currentVal, newVal;
        do {
            currentVal = reconnectMilliseconds.get();
            newVal = Math.min(currentVal * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS);
        } while (!reconnectMilliseconds.compareAndSet(currentVal, newVal));
    }

    /**
     * Fires a query in Play Console to show products available to purchase
     *
     * @param generation - is the connection generation the query belongs to, responses from an earlier one are ignored
     */
    private void queryProductDetails(String productType, @NonNull List<String> productList, int generation) {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String productId : productList) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build());
        }

        QueryProductDetailsParams productDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();

        billingClient.queryProductDetailsAsync(productDetailsParams, (billingResult, productDetailsResult) -> {
            // A reconnection happened since this query was sent, the new connection runs its own queries
            if (generation != connectionGeneration.get()) {
                Log("Query Product Details: ignoring a response from a previous connection");
                return;
            }

            if (billingResult.getResponseCode() == OK) {
                List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();

                HashSet<String> foundProductIds = new HashSet<>();
                for (ProductDetails details : productDetailsList) {
                    foundProductIds.add(details.getProductId());
                }

                for (String productId : productList) {
                    if (!foundProductIds.contains(productId)) {
                        Log("Error: Product ID '" + productId + "' not found. " +
                                "Make sure it is configured correctly in the Play Console");
                        postBillingEvent(listener -> listener.onProductQueryError(productId, new BillingResponse(ErrorType.PRODUCT_ID_QUERY_FAILED,
                                "Product ID '" + productId + "' not found", defaultResponseCode)));
                    }
                }

                if (productDetailsList.isEmpty()) {
                    Log("Query Product Details: No valid products found. Make sure product IDs are configured on Play Console");
                    postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                            "No products found", defaultResponseCode)));
                } else {
                    Log("Query Product Details: data found for " + productDetailsList.size() + " products");

                    List<ProductInfo> fetchedProductInfo = new ArrayList<>();
                    for (ProductDetails productDetails : productDetailsList) {
                        fetchedProductInfo.add(generateProductInfo(productDetails));
                    }

                    // Append newly fetched products
                    fetchedProductInfoList.addAll(fetchedProductInfo);

                    switch (productType) {
                        case INAPP:
                        case SUBS:
                            postBillingEvent(listener -> listener.onProductsFetched(fetchedProductInfo));
                            break;
                        default:
                            throw new IllegalStateException("Product type is not implemented");
                    }
                }
            } else {
                Log("Query Product Details: failed with response code: " + billingResult.getResponseCode());
                postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                        new BillingResponse(ErrorType.BILLING_ERROR, billingResult)));
            }

            // Always unblock the pipeline when a query finishes (success, empty, or error)
            // Unblock the pipeline even if this specific query failed (API response code is not OK)
            if (productDetailsQueriesPending.decrementAndGet() == 0) {
                fetchPurchasedProducts();
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
                boolean consumable = findSkuProductType(productDetails.getProductId()) == SkuProductType.CONSUMABLE;
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

    /**
     * Returns the product type based on the product ID lists provided by the developer
     * <p>
     * Does not rely on ProductDetails, so owned products can be handled even when their details are unavailable
     *
     * @param productId - is the product ID to look up
     * @return the product type, or null if the product ID is not in any list
     */
    @Nullable
    private SkuProductType findSkuProductType(String productId) {
        if (productId == null) {
            return null;
        }

        if (consumableIds != null && consumableIds.contains(productId)) {
            return SkuProductType.CONSUMABLE;
        }

        if (nonConsumableIds != null && nonConsumableIds.contains(productId)) {
            return SkuProductType.NON_CONSUMABLE;
        }

        if (subscriptionIds != null && subscriptionIds.contains(productId)) {
            return SkuProductType.SUBSCRIPTION;
        }

        return null;
    }

    /**
     * Returns purchases details for currently owned items without a network request
     */
    private void fetchPurchasedProducts() {
        if (billingClient.isReady()) {
            SupportState subsSupportState = isSubscriptionSupported();
            boolean isSubsSupported = subsSupportState == SupportState.SUPPORTED;

            // Devices without subscription support cannot own subscriptions
            if (subsSupportState == SupportState.NOT_SUPPORTED) {
                fetchedSubsPurchases = true;
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
                            Log("Query IN-APP Purchases: failed with response code: " + billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR, billingResult)));
                        }
                    }
            );

            // Query subscription purchases for supported devices
            if (isSubsSupported) {
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
                                Log("Query SUBS Purchases: failed with response code: " + billingResult.getResponseCode() + " " + billingResult.getDebugMessage());
                                postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                        new BillingResponse(ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR, billingResult)));
                            }
                        }
                );
            }

        } else {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR,
                    "Billing client is not ready yet", defaultResponseCode)));
        }
    }

    /**
     * Before using subscriptions, device-support must be checked
     * Not all devices support subscriptions
     */
    public SupportState isSubscriptionSupported() {
        if (billingClient == null || !billingClient.isReady()) {
            Log("Subscriptions support check: client is not ready");
            return SupportState.DISCONNECTED;
        }

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
            } else {
                // Report rejected purchases so a wrong license key or a tampered purchase does not fail silently
                String rejectedProducts = purchase.getProducts().toString();
                Log("Handling purchases: signature verification failed for products: " + rejectedProducts +
                        ". Make sure the license key matches the one from Play Console");

                postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.SIGNATURE_VERIFICATION_FAILED,
                        "Purchase signature verification failed for products: " + rejectedProducts, defaultResponseCode)));
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
                    continue;
                }

                // Keep owned products even without details (deactivated product or failed details query)
                // Otherwise the user loses the entitlement and unacknowledged purchases get refunded
                SkuProductType skuProductType = findSkuProductType(productId);
                if (skuProductType != null) {
                    Log("Handling purchases: product details unavailable for owned product: " + productId);
                    signatureValidPurchases.add(new PurchaseInfo(skuProductType, productId, purchase));
                } else {
                    Log("Handling purchases: skipping product: " + productId + " because it is not in any product ID list");
                }
            }
        }

        // Synchronize access to purchasedProductsList
        synchronized (purchasedProductsSync) {
            // Clear existing purchases of this type when fetching (to avoid duplicates)
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
            } else {
                // Remove any existing entries with matching tokens to prevent duplicates (when updating from onPurchasesUpdated)
                for (PurchaseInfo newPurchase : signatureValidPurchases) {
                    Iterator<PurchaseInfo> iterator = purchasedProductsList.iterator();
                    while (iterator.hasNext()) {
                        PurchaseInfo existingPurchase = iterator.next();
                        if (existingPurchase.getPurchaseToken().equals(newPurchase.getPurchaseToken())) {
                            iterator.remove();
                        }
                    }
                }
            }

            // Add new purchases
            purchasedProductsList.addAll(signatureValidPurchases);
        }

        if (purchasedProductsFetched) {
            // Mark as fetched before posting, so isPurchased() is accurate inside onPurchasedProductsFetched
            if (productType == ProductType.INAPP) {
                fetchedInAppPurchases = true;
            } else if (productType == ProductType.SUBS) {
                fetchedSubsPurchases = true;
            }

            postBillingEvent(listener -> listener.onPurchasedProductsFetched(productType, signatureValidPurchases));
        } else if (!signatureValidPurchases.isEmpty()) {
            // Skip the callback when every purchase was rejected, the errors were already reported
            postBillingEvent(listener -> listener.onProductsPurchased(signatureValidPurchases));
        }

        // Track processed tokens to prevent duplicate consume/acknowledge on multi-product purchases
        Set<String> processedConsumeTokens = new HashSet<>();
        Set<String> processedAcknowledgeTokens = new HashSet<>();

        for (PurchaseInfo purchaseInfo : signatureValidPurchases) {
            String token = purchaseInfo.getPurchaseToken();

            if (shouldAutoConsume && purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE) {
                if (processedConsumeTokens.add(token)) {
                    consumePurchase(purchaseInfo);
                }
            }

            if (shouldAutoAcknowledge) {
                boolean isProductConsumable = purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE;
                if (!isProductConsumable) {
                    if (processedAcknowledgeTokens.add(token)) {
                        acknowledgePurchase(purchaseInfo);
                    }
                }
            }
        }
    }

    /**
     * Consume consumable products so that the user can buy the item again
     * <p>
     * Consumable products might be bought/consumed by users multiple times (for e.g. diamonds, coins etc.)
     * They have to be consumed within 3 days, otherwise Google will refund the products
     */
    public void consumePurchase(@NonNull PurchaseInfo purchaseInfo) {
        if (checkPurchaseBeforeInteraction(purchaseInfo)) {
            if (purchaseInfo.getSkuProductType() == SkuProductType.CONSUMABLE) {
                if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    String token = purchaseInfo.getPurchaseToken();
                    if (!handledPurchaseTokens.add(token)) {
                        Log("Handling consumables: purchase is already being consumed or was consumed: " + purchaseInfo.getProduct());
                        return;
                    }

                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(token).build();

                    billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
                        if (billingResult.getResponseCode() == OK) {
                            // Remove every entry of this purchase (multi-product purchases share the same token)
                            synchronized (purchasedProductsSync) {
                                Iterator<PurchaseInfo> iterator = purchasedProductsList.iterator();
                                while (iterator.hasNext()) {
                                    if (iterator.next().getPurchaseToken().equals(token)) {
                                        iterator.remove();
                                    }
                                }
                            }
                            postBillingEvent(listener -> listener.onPurchaseConsumed(purchaseInfo));
                        } else {
                            // Allow a later retry
                            handledPurchaseTokens.remove(token);

                            Log("Handling consumables: error during consumption attempt: " + billingResult.getDebugMessage());

                            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                    new BillingResponse(ErrorType.CONSUME_ERROR, billingResult)));
                        }
                    });
                } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    Log("Handling consumables: purchase can not be consumed because the state is PENDING. " +
                            "A purchase can be consumed only when the state is PURCHASED");

                    postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.CONSUME_WARNING,
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
        if (checkPurchaseBeforeInteraction(purchaseInfo)) {
            switch (purchaseInfo.getSkuProductType()) {
                case NON_CONSUMABLE:
                case SUBSCRIPTION:
                    if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        if (!purchaseInfo.getPurchase().isAcknowledged()) {
                            String token = purchaseInfo.getPurchaseToken();
                            if (!handledPurchaseTokens.add(token)) {
                                Log("Handling acknowledges: purchase is already being acknowledged or was acknowledged: " + purchaseInfo.getProduct());
                                return;
                            }

                            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(token).build();

                            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                                if (billingResult.getResponseCode() == OK) {
                                    postBillingEvent(listener -> listener.onPurchaseAcknowledged(purchaseInfo));
                                } else {
                                    // Allow a later retry
                                    handledPurchaseTokens.remove(token);

                                    Log("Handling acknowledges: error during acknowledgment attempt: " + billingResult.getDebugMessage());

                                    postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                                            new BillingResponse(ErrorType.ACKNOWLEDGE_ERROR, billingResult)));
                                }
                            });
                        }
                    } else if (purchaseInfo.getPurchase().getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        Log("Handling acknowledges: purchase can not be acknowledged because the state is PENDING. " +
                                "A purchase can be acknowledged only when the state is PURCHASED");

                        postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.ACKNOWLEDGE_WARNING,
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
        ProductDetails productDetails = findProductDetailsBeforePurchase(activity, productId);
        if (productDetails == null) {
            return;
        }

        String offerToken = null;

        if (productDetails.getProductType().equals(SUBS)) {
            // The purchase() method was called with a subscription ID
            if (selectedOfferIndex == notAnOffer) {
                Log("Product: " + productId + " is a subscription. Use subscribe() instead of purchase()");
                postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                        "Product " + productId + " is a subscription. Use subscribe() instead of purchase()", defaultResponseCode)));
                return;
            }

            List<ProductDetails.SubscriptionOfferDetails> offerDetails = productDetails.getSubscriptionOfferDetails();
            if (offerDetails != null && selectedOfferIndex >= 0 && selectedOfferIndex < offerDetails.size()) {
                // The offer index represents the different offers in the subscription
                // Offer index is only available for subscriptions starting with Google Billing v5+
                offerToken = offerDetails.get(selectedOfferIndex).getOfferToken();
            }
            // Handle invalid selectedOfferIndex for subscriptions
            else {
                Log("Invalid selectedOfferIndex: " + selectedOfferIndex + " for product: " + productId +
                        ". Offer details size: " + (offerDetails != null ? offerDetails.size() : "null"));
                postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                        "Invalid subscription offer index provided", defaultResponseCode)));
                return; // Prevent proceeding with an invalid index
            }
        }

        launchBillingFlow(activity, productDetails, offerToken);
    }

    /**
     * Validates the activity and the product before launching a billing flow
     *
     * @return the product details, or null if the billing flow can not be launched (the error is already reported)
     */
    @Nullable
    private ProductDetails findProductDetailsBeforePurchase(Activity activity, String productId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log("Billing client can not launch billing flow because activity is invalid");
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Activity is null or finishing", defaultResponseCode)));
            return null;
        }

        if (!checkProductBeforeInteraction(productId)) {
            return null;
        }

        for (ProductInfo productInfo : fetchedProductInfoList) {
            if (productInfo.getProduct().equals(productId)) {
                return productInfo.getProductDetails();
            }
        }

        Log("Billing client can not launch billing flow because product details are missing for product: " + productId);
        postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.PRODUCT_NOT_EXIST,
                "Product details not found for " + productId, defaultResponseCode)));
        return null;
    }

    /**
     * Launches the Google Play billing flow for a single product
     *
     * @param offerToken - selects the subscription base plan / offer, null for in-app products
     */
    private void launchBillingFlow(@NonNull Activity activity, @NonNull ProductDetails productDetails, @Nullable String offerToken) {
        BillingFlowParams.ProductDetailsParams.Builder productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails);

        if (offerToken != null) {
            productDetailsParams.setOfferToken(offerToken);
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productDetailsParams.build()))
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);

        int responseCode = billingResult.getResponseCode();
        if (responseCode != OK) {
            Log("Launch billing flow failed with response code: " + responseCode + " " + billingResult.getDebugMessage());
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this,
                    new BillingResponse(findErrorType(responseCode), billingResult)));

            // Re-sync owned purchases, e.g. to consume a consumable that is still owned
            if (responseCode == ITEM_ALREADY_OWNED) {
                refreshPurchases();
            }
        }
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
     * Called to purchase a simple subscription
     * <p>
     * This method assumes the desired offer is the first one available (index 0)
     * For subscriptions with multiple offers, use subscribe(activity, productId, selectedOfferIndex)
     */
    public final void subscribe(Activity activity, String productId) {
        purchase(activity, productId, 0);
    }

    /**
     * Called to purchase a subscription with a specific base plan or offer
     * <p>
     * Safer than selecting an offer by index, since Google Play does not guarantee the order of offers
     * The available IDs can be read from ProductInfo.getSubscriptionOfferDetails() (getBasePlanId() / getOfferId())
     * <p>
     * Google Play only returns offers the user is eligible for (e.g. a free trial that was already used is not returned)
     *
     * @param basePlanId - is the base plan ID from Play Console
     * @param offerId    - is the offer ID from Play Console, or null to purchase the base plan without an offer
     */
    public final void subscribe(Activity activity, String productId, String basePlanId, @Nullable String offerId) {
        ProductDetails productDetails = findProductDetailsBeforePurchase(activity, productId);
        if (productDetails == null) {
            return;
        }

        if (!productDetails.getProductType().equals(SUBS)) {
            Log("Product: " + productId + " is not a subscription. Use purchase() instead of subscribe()");
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Product " + productId + " is not a subscription. Use purchase() instead of subscribe()", defaultResponseCode)));
            return;
        }

        if (basePlanId == null || basePlanId.trim().isEmpty()) {
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Base plan ID cannot be null or empty", defaultResponseCode)));
            return;
        }

        boolean basePlanFound = false;
        String offerToken = null;

        List<ProductDetails.SubscriptionOfferDetails> offerDetails = productDetails.getSubscriptionOfferDetails();
        if (offerDetails != null) {
            for (ProductDetails.SubscriptionOfferDetails details : offerDetails) {
                if (!basePlanId.equals(details.getBasePlanId())) {
                    continue;
                }
                basePlanFound = true;

                // The base plan itself has no offer ID
                boolean isSameOffer = offerId == null ? details.getOfferId() == null : offerId.equals(details.getOfferId());
                if (isSameOffer) {
                    offerToken = details.getOfferToken();
                    break;
                }
            }
        }

        // Base plan is missing: wrong ID or inactive base plan
        if (!basePlanFound) {
            Log("Base plan: " + basePlanId + " not found for subscription: " + productId);
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Base plan " + basePlanId + " not found for subscription " + productId, defaultResponseCode)));
            return;
        }

        // Offer is missing: wrong ID, inactive offer or the user is not eligible for it
        if (offerToken == null) {
            Log("Offer: " + offerId + " is not available for base plan: " + basePlanId + " of subscription: " + productId);
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.ITEM_UNAVAILABLE,
                    "Offer " + offerId + " is not available for base plan " + basePlanId + " (not found or the user is not eligible)", defaultResponseCode)));
            return;
        }

        launchBillingFlow(activity, productDetails, offerToken);
    }

    /**
     * Called to cancel a subscription
     */
    public final void unsubscribe(Activity activity, String productId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log("Handling subscription cancellation: invalid activity");
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.DEVELOPER_ERROR,
                    "Activity is null or finishing", defaultResponseCode)));
            return;
        }

        try {
            Uri.Builder subscriptionUri = Uri.parse("https://play.google.com/store/account/subscriptions").buildUpon();

            // Open the subscription's own page when a product ID is given, otherwise the general subscriptions page
            if (productId != null && !productId.trim().isEmpty()) {
                subscriptionUri.appendQueryParameter("sku", productId)
                        .appendQueryParameter("package", activity.getPackageName());
            } else {
                Log("Handling subscription cancellation: no product ID provided, opening the subscriptions page");
            }

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(subscriptionUri.build());

            activity.startActivity(intent);
        } catch (Exception e) {
            Log("Handling subscription cancellation: error while trying to unsubscribe"
                    + "\nError: " + e.getMessage());
            postBillingEvent(listener -> listener.onBillingError(BillingConnector.this, new BillingResponse(ErrorType.BILLING_ERROR,
                    "Error opening subscription settings: " + e.getMessage(), defaultResponseCode)));
        }
    }

    /**
     * Checks if a subscription is currently active
     *
     * @param productId - is the subscription product ID to check
     */
    public boolean isSubscriptionActive(String productId) {
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId) && purchaseInfo.isPurchased()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a subscription is currently active and auto-renewing
     *
     * @param productId - is the subscription product ID to check
     */
    public boolean isSubscriptionAutoRenewing(String productId) {
        synchronized (purchasedProductsSync) {
            for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                if (purchaseInfo.getProduct().equals(productId) && purchaseInfo.isPurchased()) {
                    return purchaseInfo.isAutoRenewing();
                }
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
                if (purchaseInfo.getProduct().equals(productId) && purchaseInfo.isPending()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if Google Play Store is installed on the device using a three-step verification:
     * 1. Checks for the In-App Billing service ("com.android.vending.billing.InAppBillingService.BIND")
     * 2. Checks for the Play Store package ("com.android.vending")
     * 3. Verifies if the Play Store app can handle market URLs (fallback)
     *
     * @param context - the application context
     * @return true if Play Store is installed, false otherwise
     */
    public boolean isPlayStoreInstalled(@NonNull Context context) {
        if (isBillingServiceAvailable(context)) {
            return true;
        }
        if (isPlayStoreInstalledByPackage(context)) {
            return true;
        }
        return canHandlePlayStoreUrl(context);
    }

    /**
     * Checks if Google Play Store billing service is available
     * Works on Android 11+ (API 30+) without requiring broad package visibility
     *
     * @param context - the application context
     * @return true if the billing service is available, false otherwise
     */
    private boolean isBillingServiceAvailable(@NonNull Context context) {
        try {
            Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            intent.setPackage("com.android.vending");
            List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentServices(intent, 0);
            return !resolveInfoList.isEmpty();
        } catch (Exception e) {
            return false;
        }
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
            pm.getPackageInfo("com.android.vending", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log("Google Play Store is not installed by package check");
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the Play Store app can handle market URLs as a fallback verification
     *
     * @param context - the application context
     * @return true if Play Store handles market URLs, false otherwise
     */
    private boolean canHandlePlayStoreUrl(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
            intent.setPackage("com.android.vending");
            PackageManager pm = context.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            return resolveInfo != null && "com.android.vending".equals(resolveInfo.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns a list of all purchased products
     */
    public List<PurchaseInfo> getPurchasedProductsList() {
        synchronized (purchasedProductsSync) {
            return List.copyOf(purchasedProductsList);
        }
    }

    /**
     * Checks purchase state synchronously by ProductInfo
     */
    public final PurchasedResult isPurchased(@NonNull ProductInfo productInfo) {
        return isPurchased(productInfo.getProduct());
    }

    /**
     * Checks purchase state synchronously by product ID
     */
    public final PurchasedResult isPurchased(String productId) {
        // Check the fetch state of this product's type only (unknown product IDs are treated as in-app)
        boolean isSubscription = findSkuProductType(productId) == SkuProductType.SUBSCRIPTION;
        boolean fetchedPurchasesOfType = isSubscription ? fetchedSubsPurchases : fetchedInAppPurchases;

        if (!isReady()) {
            return PurchasedResult.CLIENT_NOT_READY;
        } else if (!fetchedPurchasesOfType) {
            return PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET;
        } else {
            synchronized (purchasedProductsSync) {
                for (PurchaseInfo purchaseInfo : purchasedProductsList) {
                    if (purchaseInfo.getProduct().equals(productId) && purchaseInfo.isPurchased()) {
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
        if (base64Key == null || base64Key.trim().isEmpty()) {
            return true;
        }
        return Security.verifyPurchase(base64Key, purchase.getOriginalJson(), purchase.getSignature(), shouldEnableLogging);
    }

    /**
     * Posts a listener callback on the UI thread, discarding it if the connector was released
     * or the listener was cleared
     * <p>
     * Covers races where BillingClient completes after release()
     */
    private void postBillingEvent(@NonNull BillingEventAction action) {
        if (isReleased) {
            return;
        }

        findUiHandler().post(() -> {
            if (isReleased) {
                return;
            }

            BillingEventListener listener = billingEventListener;
            if (listener != null) {
                action.dispatch(listener);
            }
        });
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
        // Mark as released first so concurrent BillingClient callbacks cannot enqueue listener work
        isReleased = true;

        isConnecting.set(false);

        if (lifecycle != null) {
            lifecycle.removeObserver(this);
            lifecycle = null;
        }

        // End the connection in any state (not only when ready) so a connection still being established is not leaked
        if (billingClient != null) {
            Log("BillingConnector instance release: ending connection...");
            billingClient.endConnection();
        }

        isConnected = false;

        // Prevent memory leaks and NPEs from pending exponential backoff tasks after the lifecycle is destroyed
        uiHandler.removeCallbacksAndMessages(null);

        billingEventListener = null;
    }

    /**
     * Syncs purchases each time the lifecycle owner resumes
     * <p>
     * Handles purchases completed while the app was in background (e.g. PENDING payments that cleared)
     */
    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        refreshPurchases();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        release();
    }
}
