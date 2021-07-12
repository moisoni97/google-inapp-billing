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
import games.moisoni.google_iab.BillingEventListener;
import games.moisoni.google_iab.enums.PurchasedResult;
import games.moisoni.google_iab.enums.SupportState;
import games.moisoni.google_iab.models.BillingResponse;
import games.moisoni.google_iab.models.PurchaseInfo;
import games.moisoni.google_iab.models.SkuInfo;

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
    private RelativeLayout purchaseConsumable, purchaseNonConsumable, purchaseSubscription, cancelSubscription;

    private BillingConnector billingConnector;

    //list for example purposes to demonstrate how to manually acknowledge or consume purchases
    private final List<PurchaseInfo> purchasedInfoList = new ArrayList<>();

    //list for example purposes to demonstrate how to synchronously check a purchase state
    private final List<SkuInfo> fetchedSkuInfoList = new ArrayList<>();

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
        consumableIds.add("consumable_id1");
        consumableIds.add("consumable_id2");
        consumableIds.add("consumable_id3");

        //create a list with non-consumable ids
        List<String> nonConsumableIds = new ArrayList<>();
        nonConsumableIds.add("non_consumable_id1");
        nonConsumableIds.add("non_consumable_id2");
        nonConsumableIds.add("non_consumable_id3");

        //create a list with subscription ids
        List<String> subscriptionIds = new ArrayList<>();
        subscriptionIds.add("subscription_id1");
        subscriptionIds.add("subscription_id2");
        subscriptionIds.add("subscription_id3");

        billingConnector = new BillingConnector(this, "license_key") //"license_key" - public developer key from Play Console
                .setConsumableIds(consumableIds) //to set consumable ids - call only for consumable products
                .setNonConsumableIds(nonConsumableIds) //to set non-consumable ids - call only for non-consumable products
                .setSubscriptionIds(subscriptionIds) //to set subscription ids - call only for subscription products
                .autoAcknowledge() //legacy option - better call this. Alternatively purchases can be acknowledge via public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
                .autoConsume() //legacy option - better call this. Alternatively purchases can be consumed via public method consumePurchase(PurchaseInfo purchaseInfo)"
                .enableLogging() //to enable logging for debugging throughout the library - this can be skipped
                .connect(); //to connect billing client with Play Console

        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<SkuInfo> skuDetails) {
                String sku;

                for (SkuInfo skuInfo : skuDetails) {
                    sku = skuInfo.getSkuId();

                    if (sku.equalsIgnoreCase("consumable_id1")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product fetched:  " + sku);
                        Toast.makeText(JavaSampleActivity.this, "Product fetched: " + sku, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other ids

                    fetchedSkuInfoList.add(skuInfo); //check "usefulPublicMethods" to see how to synchronously check a purchase state
                }
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull List<PurchaseInfo> purchases) {
                String purchase;

                for (PurchaseInfo purchaseInfo : purchases) {
                    purchase = purchaseInfo.getSkuId();

                    if (purchase.equalsIgnoreCase("non_consumable_id2")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Purchased product fetched: " + purchase);
                        Toast.makeText(JavaSampleActivity.this, "Purchased product fetched: " + purchase, Toast.LENGTH_SHORT).show();
                    }

                    //TODO - similarly check for other ids
                }
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
                String purchase;

                for (PurchaseInfo purchaseInfo : purchases) {
                    purchase = purchaseInfo.getSkuId();

                    if (purchase.equalsIgnoreCase("subscription_id3")) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product purchased: " + purchase);
                        Toast.makeText(JavaSampleActivity.this, "Product purchased: " + purchase, Toast.LENGTH_SHORT).show();
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

                String acknowledgedSku = purchase.getSkuId();

                if (acknowledgedSku.equalsIgnoreCase("non_consumable_id2")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Acknowledged: " + acknowledgedSku);
                    Toast.makeText(JavaSampleActivity.this, "Acknowledged: " + acknowledgedSku, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                /*
                 * CONSUMABLE products entitlement can be granted either here or in onProductsPurchased
                 * */

                String consumedSku = purchase.getSkuId();

                if (consumedSku.equalsIgnoreCase("consumable_id1")) {
                    //TODO - do something
                    Log.d("BillingConnector", "Consumed: " + consumedSku);
                    Toast.makeText(JavaSampleActivity.this, "Consumed: " + consumedSku, Toast.LENGTH_SHORT).show();
                }

                //TODO - similarly check for other ids
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                switch (response.getErrorType()) {
                    case CLIENT_NOT_READY:
                        //TODO - client is not ready
                        break;
                    case CLIENT_DISCONNECTED:
                        //TODO - client has disconnected
                        break;
                    case ITEM_NOT_EXIST:
                        //TODO - item doesn't exist
                        break;
                    case ITEM_ALREADY_OWNED:
                        //TODO - item is already owned
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
                    case CONSUME_ERROR:
                        //TODO - error during consumption
                        break;
                    case FETCH_PURCHASED_PRODUCTS_ERROR:
                        //TODO - error occurred while querying purchases
                        break;
                    case BILLING_ERROR:
                        //TODO - error occurred during initialization / querying sku details
                        break;
                }

                Log.d("BillingConnector", "Error type: " + response.getErrorType() +
                        " Response code: " + response.getResponseCode() + " Message: " + response.getMessage());

                Toast.makeText(JavaSampleActivity.this, "Error type: " + response.getErrorType() +
                        " Response code: " + response.getResponseCode() + " Message: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        //init purchase buttons
        purchaseConsumable = findViewById(R.id.purchase_consumable);
        purchaseNonConsumable = findViewById(R.id.purchase_non_consumable);
        purchaseSubscription = findViewById(R.id.purchase_subscription);
        cancelSubscription = findViewById(R.id.cancel_subscription);

        //init exit app button
        exitApp = findViewById(R.id.exit_app);

        //add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable);
        BounceView.addAnimTo(purchaseNonConsumable);
        BounceView.addAnimTo(purchaseSubscription);
        BounceView.addAnimTo(cancelSubscription);
        BounceView.addAnimTo(exitApp);
    }

    private void clickListeners() {
        //purchase an item
        purchaseConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "consumable_id1"));
        purchaseNonConsumable.setOnClickListener(v -> billingConnector.purchase(JavaSampleActivity.this, "non_consumable_id2"));

        //purchase a subscription
        purchaseSubscription.setOnClickListener(v -> billingConnector.subscribe(JavaSampleActivity.this, "subscription_id3"));

        //cancel a subscription
        cancelSubscription.setOnClickListener(v -> billingConnector.unsubscribe(JavaSampleActivity.this, "subscription_id3"));

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
         * public final PurchasedResult isPurchased(SkuInfo skuInfo)
         *
         * To synchronously check a purchase state
         * */
        for (SkuInfo skuInfo : fetchedSkuInfoList) {
            if (billingConnector.isPurchased(skuInfo) == PurchasedResult.YES) {
                //TODO - do something
                Log.d("BillingConnector", "The SKU: " + skuInfo.getSkuId() + " is purchased");
            } else if (billingConnector.isPurchased(skuInfo) == PurchasedResult.NO) {
                //TODO - do something
                Log.d("BillingConnector", "The SKU: " + skuInfo.getSkuId() + " is not purchased");
            } else if (billingConnector.isPurchased(skuInfo) == PurchasedResult.CLIENT_NOT_READY) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check:  " + skuInfo.getSkuId() + " because client is not ready");
            } else if (billingConnector.isPurchased(skuInfo) == PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET) {
                //TODO - do something
                Log.d("BillingConnector", "Cannot check:  " + skuInfo.getSkuId() + " because purchased products are not fetched yet");
            }
        }

        /*
         * public void consumePurchase(PurchaseInfo purchaseInfo)
         *
         * To consume purchases
         * */
        for (PurchaseInfo purchaseInfo : purchasedInfoList) {
            billingConnector.consumePurchase(purchaseInfo);
        }

        /*
         * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
         *
         * To acknowledge purchases
         * */
        for (PurchaseInfo purchaseInfo : purchasedInfoList) {
            billingConnector.acknowledgePurchase(purchaseInfo);
        }

        /*
         * public final void purchase(Activity activity, String skuId)
         *
         * To purchase an item
         * */
        billingConnector.purchase(JavaSampleActivity.this, "sku_id");

        /*
         * public final void subscribe(Activity activity, String skuId)
         *
         * To purchase a subscription
         * */
        billingConnector.subscribe(JavaSampleActivity.this, "sku_id");

        /*
         * public final void unsubscribe(Activity activity, String skuId)
         *
         * To cancel a subscription
         * */
        billingConnector.unsubscribe(JavaSampleActivity.this, "sku_id");
    }
}