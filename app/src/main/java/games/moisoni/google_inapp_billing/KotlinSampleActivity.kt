package games.moisoni.google_inapp_billing

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.hariprasanths.bounceview.BounceView
import games.moisoni.google_iab.BillingConnector
import games.moisoni.google_iab.BillingEventListener
import games.moisoni.google_iab.enums.ErrorType
import games.moisoni.google_iab.enums.PurchasedResult
import games.moisoni.google_iab.enums.SupportState
import games.moisoni.google_iab.models.BillingResponse
import games.moisoni.google_iab.models.PurchaseInfo
import games.moisoni.google_iab.models.SkuInfo

/**
 * This is a sample app to demonstrate how to implement 'google-inapp-billing' library
 * <p>
 * This standalone app won't work because it's just for reference
 * <p>
 * To see real results, you need to implement the below code into a real project
 * released on Play Console and create your own in-app products ids
 */
class KotlinSampleActivity : AppCompatActivity() {

    private lateinit var exitApp: ImageView

    private lateinit var purchaseConsumable: RelativeLayout
    private lateinit var purchaseNonConsumable: RelativeLayout
    private lateinit var purchaseSubscription: RelativeLayout
    private lateinit var cancelSubscription: RelativeLayout

    private lateinit var billingConnector: BillingConnector

    private val purchasedInfoList = mutableListOf<PurchaseInfo>()
    private val fetchedSkuInfoList = mutableListOf<SkuInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)

        initViews()
        initializeBillingClient()
        clickListeners()
    }

    private fun initializeBillingClient() {
        //create a list with consumable ids
        val consumableIds = mutableListOf<String>()
        consumableIds.add("consumable_id1")
        consumableIds.add("consumable_id2")
        consumableIds.add("consumable_id3")

        //create a list with non-consumable ids
        val nonConsumableIds = mutableListOf<String>()
        nonConsumableIds.add("non_consumable_id1")
        nonConsumableIds.add("non_consumable_id2")
        nonConsumableIds.add("non_consumable_id3")

        //create a list with subscription ids
        val subscriptionIds = mutableListOf<String>()
        subscriptionIds.add("subscription_id1")
        subscriptionIds.add("subscription_id2")
        subscriptionIds.add("subscription_id3")

        billingConnector = BillingConnector(this, "license_key") //"license_key" - public developer key from Play Console
            .setConsumableIds(consumableIds) //to set consumable ids - call only for consumable products
            .setNonConsumableIds(nonConsumableIds) //to set non-consumable ids - call only for non-consumable products
            .setSubscriptionIds(subscriptionIds) //to set subscription ids - call only for subscription products
            .autoAcknowledge() //legacy option - better call this. Alternatively purchases can be acknowledge via public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
            .autoConsume() //legacy option - better call this. Alternatively purchases can be consumed via public method consumePurchase(PurchaseInfo purchaseInfo)"
            .enableLogging() //to enable logging for debugging throughout the library - this can be skipped
            .connect() //to connect billing client with Play Console

        billingConnector.setBillingEventListener(object : BillingEventListener {
            override fun onProductsFetched(skuDetails: MutableList<SkuInfo>) {
                var sku: String
                var price: String

                for (skuInfo in skuDetails) {
                    sku = skuInfo.sku
                    price = skuInfo.price

                    if (sku.equals("consumable_id1", ignoreCase = true)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product fetched: $sku")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Product fetched: $sku",
                            Toast.LENGTH_SHORT
                        ).show()

                        //TODO - do something
                        Log.d("BillingConnector", "Product price: $price")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Product price: $price",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids

                    fetchedSkuInfoList.add(skuInfo) //check "usefulPublicMethods" to see how to synchronously check a purchase state
                }
            }

            override fun onPurchasedProductsFetched(purchases: MutableList<PurchaseInfo>) {
                purchases.forEach {
                    when (it.sku) {
                        "non_consumable_id2" -> {
                            //TODO - do something
                            Log.d("BillingConnector", "Purchased product fetched: $it")
                            Toast.makeText(
                                this@KotlinSampleActivity,
                                "Purchased product fetched: $it",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        //TODO - similarly check for other ids
                    }
                }
            }

            override fun onProductsPurchased(purchases: MutableList<PurchaseInfo>) {
                var sku: String
                var purchaseToken: String

                for (purchaseInfo in purchases) {
                    sku = purchaseInfo.sku
                    purchaseToken = purchaseInfo.purchaseToken

                    if (sku.equals("subscription_id3", ignoreCase = true)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product purchased: $sku")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Product purchased: $sku",
                            Toast.LENGTH_SHORT
                        ).show()

                        //TODO - do something
                        Log.d("BillingConnector", "Purchase token: $purchaseToken")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Purchase token: $purchaseToken",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids

                    purchasedInfoList.add(purchaseInfo) //check "usefulPublicMethods" to see how to acknowledge or consume a purchase manually
                }
            }

            override fun onPurchaseAcknowledged(purchase: PurchaseInfo) {
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

                when (purchase.sku) {
                    "non_consumable_id2" -> {
                        //TODO - do something
                        Log.d("BillingConnector", "Acknowledged: ${purchase.sku}")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Acknowledged: ${purchase.sku}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids
                }
            }

            override fun onPurchaseConsumed(purchase: PurchaseInfo) {
                /*
                 * CONSUMABLE products entitlement can be granted either here or in onProductsPurchased
                 * */

                when (purchase.sku) {
                    "consumable_id1" -> {
                        //TODO - do something
                        Log.d("BillingConnector", "Consumed: ${purchase.sku}")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Consumed: ${purchase.sku}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids
                }
            }

            override fun onBillingError(
                billingConnector: BillingConnector,
                response: BillingResponse
            ) {
                when (response.errorType) {
                    ErrorType.CLIENT_NOT_READY -> {
                        //TODO - client is not ready yet
                    }
                    ErrorType.CLIENT_DISCONNECTED -> {
                        //TODO - client has disconnected
                    }
                    ErrorType.SKU_NOT_EXIST -> {
                        //TODO - sku does not exist
                    }
                    ErrorType.CONSUME_ERROR -> {
                        //TODO - error during consumption
                    }
                    ErrorType.ACKNOWLEDGE_ERROR -> {
                        //TODO - error during acknowledgment
                    }
                    ErrorType.ACKNOWLEDGE_WARNING -> {
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
                    }
                    ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR -> {
                        //TODO - error occurred while querying purchased products
                    }
                    ErrorType.BILLING_ERROR -> {
                        //TODO - error occurred during initialization / querying sku details
                    }
                    ErrorType.USER_CANCELED -> {
                        //TODO - user pressed back or canceled a dialog
                    }
                    ErrorType.SERVICE_UNAVAILABLE -> {
                        //TODO - network connection is down
                    }
                    ErrorType.BILLING_UNAVAILABLE -> {
                        //TODO - billing API version is not supported for the type requested
                    }
                    ErrorType.ITEM_UNAVAILABLE -> {
                        //TODO - requested product is not available for purchase
                    }
                    ErrorType.DEVELOPER_ERROR -> {
                        //TODO - invalid arguments provided to the API
                    }
                    ErrorType.ERROR -> {
                        //TODO - fatal error during the API action
                    }
                    ErrorType.ITEM_ALREADY_OWNED -> {
                        //TODO - item is already owned
                    }
                    ErrorType.ITEM_NOT_OWNED -> {
                        //TODO - failure to consume since item is not owned
                    }
                    else -> {
                        Log.d("BillingConnector", "None of the above ErrorType match")
                    }
                }

                Log.d(
                    "BillingConnector", "Error type: ${response.errorType}" +
                            " Response code: ${response.responseCode}" + " Message: ${response.debugMessage}"
                )

                Toast.makeText(
                    this@KotlinSampleActivity,
                    "Error type: ${response.errorType}" + " Response code: ${response.responseCode}"
                            + " Message: ${response.debugMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initViews() {
        //init purchase buttons
        purchaseConsumable = findViewById(R.id.purchase_consumable)
        purchaseNonConsumable = findViewById(R.id.purchase_non_consumable)
        purchaseSubscription = findViewById(R.id.purchase_subscription)
        cancelSubscription = findViewById(R.id.cancel_subscription)

        //init exit app button
        exitApp = findViewById(R.id.exit_app)

        //add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable)
        BounceView.addAnimTo(purchaseNonConsumable)
        BounceView.addAnimTo(purchaseSubscription)
        BounceView.addAnimTo(cancelSubscription)
        BounceView.addAnimTo(exitApp)
    }

    private fun clickListeners() {
        //purchase an item
        purchaseConsumable.setOnClickListener {
            billingConnector.purchase(this, "consumable_id1")
        }
        purchaseNonConsumable.setOnClickListener {
            billingConnector.purchase(this, "non_consumable_id2")
        }

        //purchase a subscription
        purchaseSubscription.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id3")
        }

        //cancel a subscription
        cancelSubscription.setOnClickListener {
            billingConnector.unsubscribe(this, "subscription_id3")
        }

        //exit app on button click
        exitApp.setOnClickListener {
            finish()
        }
    }

    /*
   * Check this method to learn how to implement useful public methods
   * provided by 'google-inapp-billing' library
   * */
    @Suppress("unused")
    private fun usefulPublicMethods() {
        /*
        * public final boolean isReady()
        *
        * Returns the state of the billing client
        * */
        if (billingConnector.isReady) {
            //TODO - do something
            Log.d("BillingConnector", "Billing client is ready")
        }

        /*
        * public SupportState isSubscriptionSupported()
        *
        * To check device-support for subscriptions (not all devices support subscriptions)
        * */
        when (billingConnector.isSubscriptionSupported) {
            SupportState.SUPPORTED -> {
                //TODO - do something
                Log.d("BillingConnector", "Device subscription support: SUPPORTED")
            }
            SupportState.NOT_SUPPORTED -> {
                //TODO - do something
                Log.d("BillingConnector", "Device subscription support: NOT_SUPPORTED")
            }
            SupportState.DISCONNECTED -> {
                //TODO - do something
                Log.d("BillingConnector", "Device subscription support: client DISCONNECTED")
            }
            else -> {
                Log.d("BillingConnector", "None of the above SupportState match")
            }
        }

        /*
         * public final PurchasedResult isPurchased(SkuInfo skuInfo)
         *
         * To synchronously check a purchase state
         * */
        for (skuInfo in fetchedSkuInfoList) {
            when (billingConnector.isPurchased(skuInfo)) {
                PurchasedResult.YES -> {
                    //TODO - do something
                    Log.d("BillingConnector", "The SKU: ${skuInfo.sku} is purchased")
                }
                PurchasedResult.NO -> {
                    //TODO - do something
                    Log.d("BillingConnector", "The SKU: ${skuInfo.sku} is not purchased")
                }
                PurchasedResult.CLIENT_NOT_READY -> {
                    //TODO - do something
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${skuInfo.sku} because client is not ready"
                    )
                }
                PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET -> {
                    //TODO - do something
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${skuInfo.sku} because purchased products are not fetched yet"
                    )
                }
                else -> {
                    Log.d("BillingConnector", "None of the above PurchasedResult match")
                }
            }
        }

        /*
        * public void consumePurchase(PurchaseInfo purchaseInfo)
        *
        * To consume purchases
        * */
        for (purchaseInfo in purchasedInfoList) {
            billingConnector.consumePurchase(purchaseInfo)
        }

        /*
        * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
        *
        * To acknowledge purchases
        * */
        for (purchaseInfo in purchasedInfoList) {
            billingConnector.acknowledgePurchase(purchaseInfo)
        }

        /*
         * public final void purchase(Activity activity, String skuId)
         *
         * To purchase an item
         * */
        billingConnector.purchase(this, "sku_id")

        /*
         * public final void subscribe(Activity activity, String skuId)
         *
         * To purchase a subscription
         * */
        billingConnector.subscribe(this, "sku_id")

        /*
        * public final void unsubscribe(Activity activity, String skuId)
        *
        * To cancel a subscription
        * */
        billingConnector.unsubscribe(this, "sku_id")

    }
}