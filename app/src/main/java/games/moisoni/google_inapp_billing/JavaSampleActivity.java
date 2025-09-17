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
import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.listeners.BillingEventListener;
import games.moisoni.google_iab.enums.PurchasedResult;
import games.moisoni.google_iab.enums.ProductType;
import games.moisoni.google_iab.enums.SupportState;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.ProductInfo;
import games.moisoni.google_iab.models.PurchaseInfo;


/**
 * This is a sample app to demonstrate how to implement 'google-inapp-billing' library
 * <p>
 * This standalone app won't work because it's just for reference
 * <p>
 * To see real results, you need to implement the below code into a real project
 * released on Play Console and create your own in-app products ids
 */
public class JavaSampleActivity extends AppCompatActivity {

    private ImageView exitApp;
    private RelativeLayout purchaseConsumable, purchaseNonConsumable, purchaseSubscription, purchaseSubscriptionOfferOne, purchaseSubscriptionOfferTwo, cancelSubscription;

    private BillingConnector billingConnector;

    //list for example purposes to demonstrate how to manually acknowledge or consume purchases
    private final List<PurchaseInfo> purchasedInfoList = new ArrayList<>();

    //list for example purposes to demonstrate how to synchronously check a purchase state
    private final List<ProductInfo> fetchedProductInfoList = new ArrayList<>();

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
        //create a list with consumable ids
        List<String> consumableIds = new ArrayList<>();
        consumableIds.add("consumable_id_1");
        consumableIds.add("consumable_id_2");
        consumableIds.add("consumable_id_3");

        //create a list with non-consumable ids
        List<String> nonConsumableIds = new ArrayList<>();
        nonConsumableIds.add("non_consumable_id_1");
        nonConsumableIds.add("non_consumable_id_2");
        nonConsumableIds.add("non_consumable_id_3");

        //create a list with subscription ids
        List<String> subscriptionIds = new ArrayList<>();
        subscriptionIds.add("subscription_id_1");
        subscriptionIds.add("subscription_id_2");
        subscriptionIds.add("subscription_id_3");

        billingConnector = new BillingConnector(this, "license_key", getLifecycle()) //"license_key" - public developer key from Play Console
                .setConsumableIds(consumableIds) //to set consumable ids - call only for consumable products
                .setNonConsumableIds(nonConsumableIds) //to set non-consumable ids - call only for non-consumable products
                .setSubscriptionIds(subscriptionIds) //to set subscription ids - call only for subscription products
                .autoAcknowledge() //legacy option - better call this. Alternatively purchases can be acknowledge via public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
                .autoConsume() //legacy option - better call this. Alternatively purchases can be consumed via public method consumePurchase(PurchaseInfo purchaseInfo)"
                .enableLogging() //to enable logging for debugging throughout the library - this can be skipped
                .connect(); //to connect billing client with Play Console

        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {
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

                    //TODO - similarly check for other ids

                    fetchedProductInfoList.add(productInfo); //check "usefulPublicMethods" to see how to synchronously check a purchase state
                }
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
                /*
                 * This will be called even when no purchased products are returned by the API
                 * */

                switch (productType) {
                    case INAPP:
                        //TODO - non-consumable/consumable products
                        break;
                    case SUBS:
                        //TODO - subscription products
                        break;
                    case COMBINED:
                        //this will be triggered on activity start
                        //the other two (INAPP and SUBS) will be triggered when the user actually buys a product
                        //TODO - restore purchases
                        break;
                }

                String product;
                for (PurchaseInfo purchaseInfo : purchases) {
                    product = purchaseInfo.getProduct();

                    if (product.equalsIgnoreCase("consumable_id_1")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Purchased product fetched: " + product);
                        Toast.makeText(JavaSampleActivity.this, "Purchased product fetched: " + product, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other ids
                }
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
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

                    //TODO - similarly check for other ids

                    purchasedInfoList.add(purchaseInfo); //check "usefulPublicMethods" to see how to acknowledge or consume a purchase manually
                }
            }

            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for NON-CONSUMABLE products and SUBSCRIPTIONS here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the purchase won't be acknowledged
                 *
                 * Google will refund users purchases that aren't acknowledged in 3 days
                 *
                 * To ensure that all valid purchases are acknowledged the library will automatically
                 * check and acknowledge all unacknowledged products at the startup
                 * */

                String acknowledgedProduct = purchase.getProduct();

                if (acknowledgedProduct.equalsIgnoreCase("consumable_id_1")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Acknowledged: " + acknowledgedProduct);
                    Toast.makeText(JavaSampleActivity.this, "Acknowledged: " + acknowledgedProduct, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                /*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the user will be able consume the product
                 * without actually paying
                 * */

                String consumedProduct = purchase.getProduct();

                if (consumedProduct.equalsIgnoreCase("consumable_id_1")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Consumed: " + consumedProduct);
                    Toast.makeText(JavaSampleActivity.this, "Consumed: " + consumedProduct, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {
                //TODO - do something
                Log.d("BillingConnector", "Product ID not found: " + productId);
                Toast.makeText(JavaSampleActivity.this, "Product ID not found: " + productId, Toast.LENGTH_SHORT).show();
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
                         * This will be triggered when a consumable purchase has a PENDING state
                         * User entitlement must be granted when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
                         * */
                        //TODO - warning during consumption
                        break;
                    case ACKNOWLEDGE_ERROR:
                        //TODO - error during acknowledgment
                        break;
                    case ACKNOWLEDGE_WARNING:
                        /*
                         * This will be triggered when a purchase can not be acknowledged because the state is PENDING
                         * A purchase can be acknowledged only when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase complete
                         * and to come back later to receive their purchase
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
                        //TODO - the purchase failed because the item is already owned
                        break;
                    case ITEM_NOT_OWNED:
                        //TODO - the requested product is not available for purchase
                        break;
                    case PLAY_STORE_NOT_INSTALLED:
                        //TODO - Google Play Store is not installed
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
        //init purchase buttons
        purchaseConsumable = findViewById(R.id.purchase_consumable);
        purchaseNonConsumable = findViewById(R.id.purchase_non_consumable);
        purchaseSubscription = findViewById(R.id.purchase_subscription);
        purchaseSubscriptionOfferOne = findViewById(R.id.purchase_subscription_offer_one);
        purchaseSubscriptionOfferTwo = findViewById(R.id.purchase_subscription_offer_two);
        cancelSubscription = findViewById(R.id.cancel_subscription);

        //init exit app button
        exitApp = findViewById(R.id.exit_app);

        //add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable);
        BounceView.addAnimTo(purchaseNonConsumable);
        BounceView.addAnimTo(purchaseSubscription);
        BounceView.addAnimTo(purchaseSubscriptionOfferOne);
        BounceView.addAnimTo(purchaseSubscriptionOfferTwo);
        BounceView.addAnimTo(cancelSubscription);
        BounceView.addAnimTo(exitApp);
    }

    private void clickListeners() {
        //purchase an item
        purchaseConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "consumable_id_1"));
        purchaseNonConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "non_consumable_id_2"));

        //purchase a subscription without an offer (only a base plan)
        purchaseSubscription.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_1"));

        //purchase a subscription with multiple offers
        //the offer index represents the different offers in the subscription (after Google Billing v5+)
        purchaseSubscriptionOfferOne.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_2", 0));
        purchaseSubscriptionOfferTwo.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id_2", 1));

        //cancel a subscription
        cancelSubscription.setOnClickListener(v -> billingConnector.unsubscribe(JavaSampleActivity.this, "subscription_id_1"));

        //exit app on button click
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
        if (billingConnector.isSubscriptionSupported() == SupportState.SUPPORTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: SUPPORTED");
        } else if (billingConnector.isSubscriptionSupported() == SupportState.NOT_SUPPORTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: NOT_SUPPORTED");
        } else if (billingConnector.isSubscriptionSupported() == SupportState.DISCONNECTED) {
            //TODO - do something
            Log.d("BillingConnector", "Device subscription support: client DISCONNECTED");
        }

        /*
         * public final PurchasedResult isPurchased(ProductInfo productInfo)
         *
         * To synchronously check a purchase state
         * */
        for (ProductInfo productInfo : fetchedProductInfoList) {
            if (billingConnector.isPurchased(productInfo) == PurchasedResult.YES) {
                //TODO - do something
                Log.d("BillingConnector", "The product: " + productInfo.getProduct() + " is purchased");
            } else if (billingConnector.isPurchased(productInfo) == PurchasedResult.NO) {
                //TODO - do something
                Log.d("BillingConnector", "The product: " + productInfo.getProduct() + " is not purchased");
            } else if (billingConnector.isPurchased(productInfo) == PurchasedResult.CLIENT_NOT_READY) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check: " + productInfo.getProduct() + " because client is not ready");
            } else if (billingConnector.isPurchased(productInfo) == PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check: " + productInfo.getProduct() + " because purchased products are not fetched yet");
            }
        }

        /*
         * public void consumePurchase(PurchaseInfo purchaseInfo)
         *
         * To consume consumable products
         * */
        for (PurchaseInfo purchaseInfo : purchasedInfoList) {
            billingConnector.consumePurchase(purchaseInfo);
        }

        /*
         * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
         *
         * To acknowledge non-consumable products & subscriptions
         * */
        for (PurchaseInfo purchaseInfo : purchasedInfoList) {
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
         * To purchase a subscription with a base plan
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "product_id");

        /*
         * public final void subscribe(Activity activity, String productId, int selectedOfferIndex)
         *
         * To purchase a subscription with multiple offers
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "product_id", 1);

        /*
         * public final void unsubscribe(Activity activity, String productId)
         *
         * To cancel a subscription
         * */
        billingConnector.unsubscribe(JavaSampleActivity.this, "product_id");
    }
}
