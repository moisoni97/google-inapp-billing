package games.moisoni.google_inapp_billing;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.hariprasanths.bounceview.BounceView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.listener.BillingEventListener;
import games.moisoni.google_iab.status.PurchasedResult;
import games.moisoni.google_iab.type.ProductType;
import games.moisoni.google_iab.status.SupportState;
import games.moisoni.google_iab.model.BillingResponse;
import games.moisoni.google_iab.model.ProductInfo;
import games.moisoni.google_iab.model.PurchaseInfo;

/**
 * This is a sample app to demonstrate how to implement 'google-inapp-billing' library
 * <p>
 * This standalone app won't work because it's just for reference
 * <p>
 * To see real results, you need to implement the code below in a real project
 * released on Play Console and create your own in-app products IDs
 */
public class JavaSampleActivity extends AppCompatActivity {

    private ImageView exitApp;
    private RelativeLayout purchaseConsumable, purchaseNonConsumable, purchaseSubscription, purchaseSubscriptionOfferOne, purchaseSubscriptionOfferTwo, cancelSubscription;

    private BillingConnector billingConnector;

    // Fetched products (keyed by product ID) for example purposes to demonstrate how to synchronously check a purchase state
    // The onProductsFetched callback is triggered again after a reconnection, so a product is replaced instead of being added twice
    private final Map<String, ProductInfo> fetchedProducts = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        initViews();
        initializeBillingClient();
        clickListeners();
    }

    private void initializeBillingClient() {
        // Create a list with consumable IDs
        List<String> consumableIds = new ArrayList<>();
        consumableIds.add("consumable_id_1");
        consumableIds.add("consumable_id_2");
        consumableIds.add("consumable_id_3");

        // Create a list with non-consumable IDs
        List<String> nonConsumableIds = new ArrayList<>();
        nonConsumableIds.add("non_consumable_id_1");
        nonConsumableIds.add("non_consumable_id_2");
        nonConsumableIds.add("non_consumable_id_3");

        // Create a list with subscription IDs
        List<String> subscriptionIds = new ArrayList<>();
        subscriptionIds.add("subscription_id_1");
        subscriptionIds.add("subscription_id_2");
        subscriptionIds.add("subscription_id_3");

        billingConnector = new BillingConnector(this, "license_key", getLifecycle()) // "license_key" - public developer key from Play Console
                .setConsumableIds(consumableIds) // To set consumable IDs - call only for consumable products
                .setNonConsumableIds(nonConsumableIds) // To set non-consumable IDs - call only for non-consumable products
                .setSubscriptionIds(subscriptionIds) // To set subscription IDs - call only for subscription products
                .autoAcknowledge() // Recommended - acknowledges non-consumables and subscriptions automatically. Alternatively, call the public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
                .autoConsume() // Recommended - consumes consumables automatically. Alternatively, call the public method "consumePurchase(PurchaseInfo purchaseInfo)"
                .enableLogging() // To enable logging for debugging throughout the library - this can be skipped
                .connect(); // To connect the billing client with Google Play

        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
                /*
                 * Provides the details of the products available for purchase
                 *
                 * Triggered separately for in-app products and subscriptions after connecting,
                 * and again after the connection is re-established
                 * */

                String product;
                String price;

                for (ProductInfo productInfo : productDetails) {
                    product = productInfo.getProduct();
                    price = productInfo.getOneTimePurchaseOfferFormattedPrice();

                    if (product.equalsIgnoreCase("consumable_id_1")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product fetched: " + product);
                        Toast.makeText(JavaSampleActivity.this, "Product fetched: " + product, Toast.LENGTH_SHORT).show();

                        //TODO - do something
                        Log.d("BillingConnector", "Product price: " + price);
                        Toast.makeText(JavaSampleActivity.this, "Product price: " + price, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other IDs

                    fetchedProducts.put(productInfo.getProduct(), productInfo);
                }
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
                /*
                 * This will be called even when no purchased products are returned by the API
                 *
                 * It is triggered after connecting and again every time purchases are refreshed
                 * (each time the activity resumes, or when refreshPurchases() is called)
                 * */

                switch (productType) {
                    case INAPP:
                        // Triggered for in-app (one-time) purchases
                        //TODO - restore non-consumable purchases
                        break;
                    case SUBS:
                        // Triggered for subscription products
                        //TODO - restore subscriptions
                        break;
                    case COMBINED:
                        // Never passed to this callback, new purchases are delivered to onProductsPurchased
                        break;
                }

                String product;
                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();

                    /*
                     * Restore entitlements only for NON-CONSUMABLE products and SUBSCRIPTIONS that are PURCHASED and acknowledged
                     *
                     * PENDING purchases are listed too, and CONSUMABLE products are granted in onPurchaseConsumed
                     * Purchases that are not acknowledged yet are acknowledged by the library right after this callback,
                     * they are granted in onPurchaseAcknowledged (Google refunds a purchase that can't be acknowledged)
                     * */
                    if (product.equalsIgnoreCase("non_consumable_id_2") && purchaseInfo.isPurchased() && purchaseInfo.isAcknowledged()) {
                        //TODO - do something
                        Log.d("BillingConnector", "Purchased product fetched: " + product);
                        Toast.makeText(JavaSampleActivity.this, "Purchased product fetched: " + product, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other IDs
                }
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
                /*
                 * Triggered when a purchase flow finishes successfully
                 *
                 * Don't grant entitlement here: the purchase can still be PENDING (e.g. cash payments)
                 * Grant it in onPurchaseAcknowledged (non-consumables, subscriptions) or onPurchaseConsumed (consumables)
                 * */

                String product;
                String purchaseToken;

                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();
                    purchaseToken = purchaseInfo.getPurchaseToken();

                    if (product.equalsIgnoreCase("consumable_id_1")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product purchased: " + product);
                        Toast.makeText(JavaSampleActivity.this, "Product purchased: " + product, Toast.LENGTH_SHORT).show();

                        //TODO - do something
                        Log.d("BillingConnector", "Purchase token: " + purchaseToken);
                        Toast.makeText(JavaSampleActivity.this, "Purchase token: " + purchaseToken, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other IDs
                }
            }

            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here
                 *
                 * Only PURCHASED (paid) purchases are acknowledged, and Google refunds purchases that aren't acknowledged in 3 days
                 *
                 * To ensure that all valid purchases are acknowledged the library will automatically
                 * check and acknowledge all unacknowledged products at startup and each time purchases are refreshed
                 * So this is also triggered for purchases made earlier (e.g. the app was closed before the acknowledgment)
                 *
                 * The entitlement is also restored in onPurchasedProductsFetched, so granting it must be safe to repeat
                 * (e.g. set a flag, don't add to a counter)
                 *
                 * Note: purchase.isAcknowledged() still returns false here, it reflects the state before the acknowledgment
                 * */

                String acknowledgedProduct = purchase.getProduct();

                if (acknowledgedProduct.equalsIgnoreCase("non_consumable_id_2")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Acknowledged: " + acknowledgedProduct);
                    Toast.makeText(JavaSampleActivity.this, "Acknowledged: " + acknowledgedProduct, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other IDs
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Only PURCHASED (paid) purchases are consumed, so the payment is complete at this point
                 * A purchase is consumed only once, so the item can be added to the user's balance (e.g. coins)
                 * If multi-quantity purchases are enabled in Play Console, grant purchase.getQuantity() items
                 * */

                String consumedProduct = purchase.getProduct();

                if (consumedProduct.equalsIgnoreCase("consumable_id_1")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Consumed: " + consumedProduct);
                    Toast.makeText(JavaSampleActivity.this, "Consumed: " + consumedProduct, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other IDs
            }

            @Override
            public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {
                /*
                 * Triggered when Google Play doesn't return the details of a product ID
                 *
                 * The message contains the reason: not found (not created or not active in Play Console),
                 * invalid product ID format, or no offer the user is eligible for
                 * */

                //TODO - do something
                Log.d("BillingConnector", "Product query error: " + response.getDebugMessage());
                Toast.makeText(JavaSampleActivity.this, "Product query error: " + response.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                switch (response.getErrorType()) {
                    case CLIENT_NOT_READY:
                        //TODO - client is not ready yet
                        break;
                    case CLIENT_DISCONNECTED:
                        //TODO - client has disconnected
                        break;
                    case PRODUCT_NOT_EXIST:
                        //TODO - product does not exist
                        break;
                    case CONSUME_ERROR:
                        //TODO - error during consumption
                        break;
                    case CONSUME_WARNING:
                        /*
                         * This will be triggered when a consumable purchase has a PENDING state (reported once per purchase)
                         * User entitlement must be granted when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * isPurchasePending() / purchase.isPending() can be used to show the pending state later on
                         * */
                        //TODO - warning during consumption
                        break;
                    case ACKNOWLEDGE_ERROR:
                        //TODO - error during acknowledgment
                        break;
                    case ACKNOWLEDGE_WARNING:
                        /*
                         * This will be triggered when a purchase can not be acknowledged because the state is PENDING (reported once per purchase)
                         * A purchase can be acknowledged only when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * isPurchasePending() / purchase.isPending() can be used to show the pending state later on
                         * */
                        //TODO - warning during acknowledgment
                        break;
                    case FETCH_PURCHASED_PRODUCTS_ERROR:
                        //TODO - error occurred while querying purchased products
                        break;
                    case BILLING_ERROR:
                        //TODO - error occurred during initialization / querying product details
                        break;
                    case USER_CANCELED:
                        //TODO - transaction was canceled by the user
                        break;
                    case SERVICE_UNAVAILABLE:
                        //TODO - the service is currently unavailable
                        break;
                    case NETWORK_ERROR:
                        //TODO - a network error occurred during the operation
                        break;
                    case BILLING_UNAVAILABLE:
                        //TODO - a user billing error occurred during processing
                        break;
                    case ITEM_UNAVAILABLE:
                        //TODO - requested product is not available for purchase
                        break;
                    case DEVELOPER_ERROR:
                        //TODO - error resulting from incorrect usage of the API
                        break;
                    case ERROR:
                        //TODO - fatal error during the API action
                        break;
                    case ITEM_ALREADY_OWNED:
                        /*
                         * The library automatically refreshes purchases after this error,
                         * so a consumable that was not consumed yet gets consumed
                         * */
                        //TODO - the purchase failed because the item is already owned
                        break;
                    case ITEM_NOT_OWNED:
                        //TODO - failure to consume since item is not owned
                        break;
                    case PLAY_STORE_NOT_INSTALLED:
                        //TODO - Google Play Store is not installed
                        break;
                    case SIGNATURE_VERIFICATION_FAILED:
                        /*
                         * The purchase signature doesn't match the license key (wrong key or tampered purchase)
                         * The purchase is ignored (not acknowledged / consumed)
                         * */
                        //TODO - purchase signature verification failed
                        break;
                    case FEATURE_NOT_SUPPORTED:
                        //TODO - the requested feature is not supported by Google Play on this device
                        break;
                }

                Log.d("BillingConnector", "Error type: " + response.getErrorType() +
                        " Response code: " + response.getResponseCode() + " Message: " + response.getDebugMessage());

                Toast.makeText(JavaSampleActivity.this, "Error type: " + response.getErrorType() +
                        " Response code: " + response.getResponseCode() + " Message: " + response.getDebugMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        // Init purchase buttons
        purchaseConsumable = findViewById(R.id.purchase_consumable);
        purchaseNonConsumable = findViewById(R.id.purchase_non_consumable);
        purchaseSubscription = findViewById(R.id.purchase_subscription);
        purchaseSubscriptionOfferOne = findViewById(R.id.purchase_subscription_offer_one);
        purchaseSubscriptionOfferTwo = findViewById(R.id.purchase_subscription_offer_two);
        cancelSubscription = findViewById(R.id.cancel_subscription);

        // Init exit app button
        exitApp = findViewById(R.id.exit_app);

        // Add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable);
        BounceView.addAnimTo(purchaseNonConsumable);
        BounceView.addAnimTo(purchaseSubscription);
        BounceView.addAnimTo(purchaseSubscriptionOfferOne);
        BounceView.addAnimTo(purchaseSubscriptionOfferTwo);
        BounceView.addAnimTo(cancelSubscription);
        BounceView.addAnimTo(exitApp);
    }

    private void clickListeners() {
        // Purchase an item
        purchaseConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "consumable_id_1"));
        purchaseNonConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "non_consumable_id_2"));

        // Purchase the first offer of a subscription (index 0)
        // Fine for a subscription with a single base plan and no offers, otherwise select the offer by ID (see below)
        purchaseSubscription.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_1"));

        // Purchase a subscription with a specific base plan / offer (IDs from Play Console)
        // Pass null as the offer ID to purchase the base plan without an offer
        purchaseSubscriptionOfferOne.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_2", "base_plan_id", "offer_id_1"));
        purchaseSubscriptionOfferTwo.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_2", "base_plan_id", "offer_id_2"));

        // Cancel a subscription
        cancelSubscription.setOnClickListener(v -> billingConnector.unsubscribe(JavaSampleActivity.this, "subscription_id_1"));

        // Exit app on button click
        exitApp.setOnClickListener(v -> finish());
    }

    /*
     * Check this method to learn how to implement useful public methods
     * provided by 'google-inapp-billing' library
     * */
    @SuppressWarnings("unused")
    private void usefulPublicMethods() {
        /*
         * public final boolean isReady()
         *
         * Returns the state of the billing client
         * */
        if (billingConnector.isReady()) {
            //TODO - do something
            Log.d("BillingConnector", "Billing client is ready");
        }

        /*
         * public SupportState isSubscriptionSupported()
         *
         * To check device-support for subscriptions (not all devices support subscriptions)
         * */
        SupportState subscriptionSupport = billingConnector.isSubscriptionSupported();
        if (subscriptionSupport == SupportState.SUPPORTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: SUPPORTED");
        } else if (subscriptionSupport == SupportState.NOT_SUPPORTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: NOT_SUPPORTED");
        } else if (subscriptionSupport == SupportState.DISCONNECTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: client DISCONNECTED");
        }

        /*
         * public final PurchasedResult isPurchased(ProductInfo productInfo)
         *
         * To synchronously check a purchase state
         * */
        for (ProductInfo productInfo : fetchedProducts.values()) {
            PurchasedResult purchasedResult = billingConnector.isPurchased(productInfo);
            if (purchasedResult == PurchasedResult.YES) {
                //TODO - do something
                Log.d("BillingConnector", "The product: " + productInfo.getProduct() + " is purchased");
            } else if (purchasedResult == PurchasedResult.NO) {
                //TODO - do something
                Log.d("BillingConnector", "The product: " + productInfo.getProduct() + " is not purchased");
            } else if (purchasedResult == PurchasedResult.CLIENT_NOT_READY) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check: " + productInfo.getProduct() + " because client is not ready");
            } else if (purchasedResult == PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check: " + productInfo.getProduct() + " because purchased products are not fetched yet");
            }
        }

        /*
         * public final PurchasedResult isPurchased(String productId)
         *
         * To synchronously check a purchase state by product ID string
         * */
        PurchasedResult isPurchasedResult = billingConnector.isPurchased("non_consumable_id_1");
        if (isPurchasedResult == PurchasedResult.YES) {
            //TODO - do something
            Log.d("BillingConnector", "Product is purchased");
        }

        /*
         * public boolean isSubscriptionActive(String productId)
         *
         * To check if a subscription is currently active (PURCHASED state)
         * */
        boolean isSubsActive = billingConnector.isSubscriptionActive("subscription_id_1");
        Log.d("BillingConnector", "Is subscription active: " + isSubsActive);

        /*
         * public boolean isSubscriptionAutoRenewing(String productId)
         *
         * To check if an active subscription is currently auto-renewing
         * */
        boolean isSubsAutoRenewing = billingConnector.isSubscriptionAutoRenewing("subscription_id_1");
        Log.d("BillingConnector", "Is subscription auto-renewing: " + isSubsAutoRenewing);

        /*
         * public boolean isPurchasePending(String productId)
         *
         * To check if a purchase is waiting for payment completion
         * */
        boolean isPending = billingConnector.isPurchasePending("consumable_id_1");
        Log.d("BillingConnector", "Is purchase pending: " + isPending);

        /*
         * public boolean isPlayStoreInstalled(Context context)
         *
         * To check if Google Play Store and Billing Service are available
         * */
        boolean isPlayStoreInstalled = billingConnector.isPlayStoreInstalled(this);
        Log.d("BillingConnector", "Is Play Store installed: " + isPlayStoreInstalled);

        /*
         * public List<PurchaseInfo> getPurchasedProductsList()
         *
         * Returns a read-only snapshot of all currently owned products
         * */
        List<PurchaseInfo> allPurchases = billingConnector.getPurchasedProductsList();
        Log.d("BillingConnector", "Total owned purchases: " + allPurchases.size());

        /*
         * public ProductInfo getProductInfo() (PurchaseInfo)
         *
         * Returns the product details of a purchase
         * Can be null when Google Play doesn't return details for an owned product (e.g. deactivated in Play Console)
         * */
        for (PurchaseInfo purchaseInfo : allPurchases) {
            ProductInfo productInfo = purchaseInfo.getProductInfo();
            if (productInfo != null) {
                Log.d("BillingConnector", "Owned product title: " + productInfo.getTitle());
            }
        }

        /*
         * public final void refreshPurchases()
         *
         * To re-sync owned purchases with Google Play (e.g. from a "Restore purchases" button)
         * Called automatically each time the activity resumes when a Lifecycle is passed to the constructor
         * */
        billingConnector.refreshPurchases();

        /*
         * public void consumePurchase(PurchaseInfo purchaseInfo)
         *
         * To consume consumable products (only needed without autoConsume())
         * Every owned purchase can be passed, the ones that are not consumables are ignored
         * */
        for (PurchaseInfo purchaseInfo : allPurchases) {
            billingConnector.consumePurchase(purchaseInfo);
        }

        /*
         * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
         *
         * To acknowledge non-consumable products & subscriptions (only needed without autoAcknowledge())
         * Every owned purchase can be passed, consumables and already acknowledged purchases are ignored
         * */
        for (PurchaseInfo purchaseInfo : allPurchases) {
            billingConnector.acknowledgePurchase(purchaseInfo);
        }

        /*
         * public final void purchase(Activity activity, String productId)
         *
         * To purchase a non-consumable/consumable product
         * */
        billingConnector.purchase(JavaSampleActivity.this, "product_id");

        /*
         * public final void subscribe(Activity activity, String productId)
         *
         * To purchase the first offer returned by Google Play (index 0)
         * Fine for a subscription with a single base plan and no offers, otherwise select the offer by ID
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "product_id");

        /*
         * public final void subscribe(Activity activity, String productId, String basePlanId, String offerId)
         *
         * To purchase a subscription with a specific base plan / offer (recommended)
         * Pass null as the offer ID to purchase the base plan without an offer
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "product_id", "base_plan_id", null);
        billingConnector.subscribe(JavaSampleActivity.this, "product_id", "base_plan_id", "offer_id");

        /*
         * public final void subscribe(Activity activity, String productId, int selectedOfferIndex)
         *
         * To purchase a subscription with multiple offers by index
         * Google Play doesn't guarantee the order of offers, prefer selecting them by ID
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "product_id", 1);

        /*
         * public final void unsubscribe(Activity activity, String productId)
         *
         * To cancel a subscription (opens the Google Play subscription settings)
         * Pass null to open the general subscriptions page
         * */
        billingConnector.unsubscribe(JavaSampleActivity.this, "product_id");
    }
}
