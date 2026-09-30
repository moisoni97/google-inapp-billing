package games.moisoni.google_inapp_billing;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import games.moisoni.google_iab.BillingConnector;
import games.moisoni.google_iab.listener.BillingEventListener;
import games.moisoni.google_iab.status.PurchasedResult;
import games.moisoni.google_iab.type.ProductType;
import games.moisoni.google_iab.model.BillingResponse;
import games.moisoni.google_iab.model.ProductInfo;
import games.moisoni.google_iab.model.PurchaseInfo;

/**
 * This is an example of how to implement a one-time product purchase
 * Below, you'll see a simple "remove ads button" scenario
 * <p>
 * Following this logic, you'll be able to integrate any one-time product purchase or subscriptions
 * <p>
 * We have a boolean variable "userPrefersAdFree" that will be `true` only when the API successfully acknowledges the purchase
 * The state of the variable will be saved using SharedPreferences so we can retrieve it in other activities/fragments
 * Before showing the ads, we'll always check the value of the variable and proceed only if its value is set to `false`
 * <p>
 * The logic is simple and should be self-explanatory
 */
public class RemoveAdsExampleActivity extends AppCompatActivity {

    private BillingConnector billingConnector;

    // This is the variable in which we'll store the status of the purchase
    // Once we'll have the data stored, we can retrieve it in any activity or fragment,
    // to update the code and the UI accordingly to the user purchase
    private boolean userPrefersAdFree = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_ads_example);

        loadUserPreferences();
        initializeBillingClient();
        removeAds();
    }

    private void loadUserPreferences() {
        // Must be initialized once before it's used (usually in your Application class)
        new SharedPrefsHelper.Builder()
                .setContext(getApplicationContext())
                .setUseDefaultSharedPreference(true)
                .build();

        // Here we are loading the data into our variable
        // It's very important to call this before trying to access the variable so you'll have the correct status of the purchase
        // Notice this is the first thing called in the "onCreate" method
        userPrefersAdFree = SharedPrefsHelper.getBoolean("userPrefersAdFree", false);
    }

    private void initializeBillingClient() {
        List<String> nonConsumableIds = new ArrayList<>();
        nonConsumableIds.add(getString(R.string.remove_ads_play_console_id));

        billingConnector = new BillingConnector(this, getString(R.string.license_key_play_console), getLifecycle())
                .setNonConsumableIds(nonConsumableIds)
                .autoAcknowledge()
                .enableLogging()
                .connect();

        billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<ProductInfo> productDetails) {

            }

            // This IS the listener in which we can restore previous purchases
            // It's triggered after connecting and each time the activity resumes, so it also keeps the status in sync (e.g. after a refund)
            @Override
            public void onPurchasedProductsFetched(@NonNull ProductType productType, @NonNull List<PurchaseInfo> purchases) {
                boolean isOwned = false;
                boolean isAcknowledged = false;

                for (PurchaseInfo purchaseInfo : purchases) {
                    // PENDING purchases are listed too, only PURCHASED ones count
                    if (purchaseInfo.getProduct().equalsIgnoreCase(getString(R.string.remove_ads_play_console_id)) && purchaseInfo.isPurchased()) {
                        isOwned = true;
                        isAcknowledged = purchaseInfo.isAcknowledged();
                        break;
                    }
                }

                if (isOwned && isAcknowledged && !userPrefersAdFree) {
                    // Here we are saving the purchase status into our "userPrefersAdFree" variable
                    // A purchase that is not acknowledged yet is granted in onPurchaseAcknowledged
                    userPrefersAdFree = true;
                    SharedPrefsHelper.putBoolean("userPrefersAdFree", true);

                    Toast.makeText(RemoveAdsExampleActivity.this, "The previous purchase was successfully restored.", Toast.LENGTH_SHORT).show();
                } else if (!isOwned && userPrefersAdFree
                        && billingConnector.isPurchased(getString(R.string.remove_ads_play_console_id)) == PurchasedResult.NO) {
                    // This callback is triggered separately for in-app products and subscriptions,
                    // so the product missing from "purchases" is not enough: isPurchased() confirms it's no longer owned
                    // The product is no longer owned (e.g. refunded), so ads are shown again
                    userPrefersAdFree = false;
                    SharedPrefsHelper.putBoolean("userPrefersAdFree", false);
                }
            }

            // This IS NOT the listener in which we'll give user entitlement for purchases (see ReadMe.md why)
            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {

            }

            // This IS the listener in which we'll give user entitlement for purchases (the ReadMe.md explains why)
            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                String acknowledgedProduct = purchase.getProduct();

                if (acknowledgedProduct.equalsIgnoreCase(getString(R.string.remove_ads_play_console_id))) {

                    // Here we are saving the purchase status into our "userPrefersAdFree" variable
                    userPrefersAdFree = true;
                    SharedPrefsHelper.putBoolean("userPrefersAdFree", true);

                    Toast.makeText(RemoveAdsExampleActivity.this, "The purchase was successfully made.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {

            }

            @Override
            public void onProductQueryError(@NonNull String productId, @NonNull BillingResponse response) {

            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                switch (response.getErrorType()) {
                    case ACKNOWLEDGE_WARNING:
                        // This response will be triggered when the purchase is still PENDING
                        Toast.makeText(RemoveAdsExampleActivity.this, "The transaction is still pending. Please come back later to receive the purchase!", Toast.LENGTH_SHORT).show();
                        break;
                    case BILLING_UNAVAILABLE:
                    case SERVICE_UNAVAILABLE:
                        Toast.makeText(RemoveAdsExampleActivity.this, "Billing is unavailable at the moment. Check your internet connection!", Toast.LENGTH_SHORT).show();
                        break;
                    case ERROR:
                        Toast.makeText(RemoveAdsExampleActivity.this, "Something happened, the transaction was canceled!", Toast.LENGTH_SHORT).show();
                        break;
                    case ITEM_ALREADY_OWNED:
                        // The library refreshes the purchases after this error, onPurchasedProductsFetched restores the purchase
                        Toast.makeText(RemoveAdsExampleActivity.this, "You already own this product, restoring the purchase...", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void removeAds() {
        Button removeAdsButton = findViewById(R.id.remove_ads_button);
        removeAdsButton.setOnClickListener(v -> billingConnector.purchase(RemoveAdsExampleActivity.this, getString(R.string.remove_ads_play_console_id)));
    }
}