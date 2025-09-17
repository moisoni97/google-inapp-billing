package games.moisoni.google_inapp_billing

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.hariprasanths.bounceview.BounceView
import games.moisoni.google_iab.BillingConnector
import games.moisoni.google_iab.listeners.BillingEventListener
import games.moisoni.google_iab.enums.ErrorType
import games.moisoni.google_iab.enums.PurchasedResult
import games.moisoni.google_iab.enums.ProductType
import games.moisoni.google_iab.enums.SupportState
import games.moisoni.google_iab.models.BillingResponse
import games.moisoni.google_iab.models.ProductInfo
import games.moisoni.google_iab.models.PurchaseInfo


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
    private lateinit var purchaseSubscriptionOfferOne: RelativeLayout
    private lateinit var purchaseSubscriptionOfferTwo: RelativeLayout
    private lateinit var cancelSubscription: RelativeLayout

    private lateinit var billingConnector: BillingConnector

    private val purchasedInfoList = mutableListOf<PurchaseInfo>()
    private val fetchedProductInfoList = mutableListOf<ProductInfo>()

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
        consumableIds.add("consumable_id_1")
        consumableIds.add("consumable_id_2")
        consumableIds.add("consumable_id_3")

        //create a list with non-consumable ids
        val nonConsumableIds = mutableListOf<String>()
        nonConsumableIds.add("non_consumable_id_1")
        nonConsumableIds.add("non_consumable_id_2")
        nonConsumableIds.add("non_consumable_id_3")

        //create a list with subscription ids
        val subscriptionIds = mutableListOf<String>()
        subscriptionIds.add("subscription_id_1")
        subscriptionIds.add("subscription_id_2")
        subscriptionIds.add("subscription_id_3")

        billingConnector = BillingConnector(
            this,
            "license_key",
            lifecycle
        ) //"license_key" - public developer key from Play Console
            .setConsumableIds(consumableIds) //to set consumable ids - call only for consumable products
            .setNonConsumableIds(nonConsumableIds) //to set non-consumable ids - call only for non-consumable products
            .setSubscriptionIds(subscriptionIds) //to set subscription ids - call only for subscription products
            .autoAcknowledge() //legacy option - better call this. Alternatively purchases can be acknowledge via public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
            .autoConsume() //legacy option - better call this. Alternatively purchases can be consumed via public method consumePurchase(PurchaseInfo purchaseInfo)"
            .enableLogging() //to enable logging for debugging throughout the library - this can be skipped
            .connect() //to connect billing client with Play Console

        billingConnector.setBillingEventListener(object :
            BillingEventListener {
            override fun onProductsFetched(productDetails: MutableList<ProductInfo>) {
                var product: String
                var price: String

                for (productInfo in productDetails) {
                    product = productInfo.product
                    price = productInfo.oneTimePurchaseOfferFormattedPrice

                    if (product.equals("consumable_id_1", ignoreCase = true)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product fetched: $product")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Product fetched: $product",
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

                    fetchedProductInfoList.add(productInfo) //check "usefulPublicMethods" to see how to synchronously check a purchase state
                }
            }

            override fun onPurchasedProductsFetched(
                productType: ProductType,
                purchases: MutableList<PurchaseInfo>
            ) {
                /*
                * This will be called even when no purchased products are returned by the API
                * */

                when (productType) {
                    ProductType.INAPP -> {
                        //TODO - non-consumable/consumable products
                    }

                    ProductType.SUBS -> {
                        //TODO - subscription products
                    }

                    ProductType.COMBINED -> {
                        //this will be triggered on activity start
                        //the other two (INAPP and SUBS) will be triggered when the user actually buys a product
                        //TODO - restore purchases
                    }
                }

                purchases.forEach {
                    when (it.product) {
                        "non_consumable_id_2" -> {
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
                var product: String
                var purchaseToken: String

                for (purchaseInfo in purchases) {
                    product = purchaseInfo.product
                    purchaseToken = purchaseInfo.purchaseToken

                    if (product.equals("subscription_id_3", ignoreCase = true)) {
                        //TODO - do something
                        Log.d("BillingConnector", "Product purchased: $product")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Product purchased: $product",
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

                when (purchase.product) {
                    "non_consumable_id_2" -> {
                        //TODO - do something
                        Log.d("BillingConnector", "Acknowledged: ${purchase.product}")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Acknowledged: ${purchase.product}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids
                }
            }

            override fun onPurchaseConsumed(purchase: PurchaseInfo) {
                /*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Even though onProductsPurchased is triggered when a purchase is successfully made
                 * there might be a problem along the way with the payment and the user will be able consume the product
                 * without actually paying
                 * */

                when (purchase.product) {
                    "consumable_id_1" -> {
                        //TODO - do something
                        Log.d("BillingConnector", "Consumed: ${purchase.product}")
                        Toast.makeText(
                            this@KotlinSampleActivity,
                            "Consumed: ${purchase.product}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    //TODO - similarly check for other ids
                }
            }

            override fun onProductQueryError(
                productId: String,
                response: BillingResponse
            ) {
                //TODO - do something
                Log.d("BillingConnector", "Product ID not found: $productId")
                Toast.makeText(
                    this@KotlinSampleActivity,
                    "Product ID not found: $productId",
                    Toast.LENGTH_SHORT
                ).show()
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

                    ErrorType.PRODUCT_NOT_EXIST -> {
                        //TODO - product does not exist
                    }

                    ErrorType.CONSUME_ERROR -> {
                        //TODO - error during consumption
                    }

                    ErrorType.CONSUME_WARNING -> {
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
                        //TODO - error occurred during initialization / querying product details
                    }

                    ErrorType.USER_CANCELED -> {
                        //TODO - transaction was canceled by the user
                    }

                    ErrorType.SERVICE_UNAVAILABLE -> {
                        //TODO - the service is currently unavailable
                    }

                    ErrorType.NETWORK_ERROR -> {
                        //TODO - a network error occurred during the operation
                    }

                    ErrorType.BILLING_UNAVAILABLE -> {
                        //TODO - a user billing error occurred during processing
                    }

                    ErrorType.ITEM_UNAVAILABLE -> {
                        //TODO - requested product is not available for purchase
                    }

                    ErrorType.DEVELOPER_ERROR -> {
                        //TODO - error resulting from incorrect usage of the API
                    }

                    ErrorType.ERROR -> {
                        //TODO - fatal error during the API action
                    }

                    ErrorType.ITEM_ALREADY_OWNED -> {
                        //TODO - the purchase failed because the item is already owned
                    }

                    ErrorType.ITEM_NOT_OWNED -> {
                        //TODO - the requested product is not available for purchase
                    }

                    ErrorType.PLAY_STORE_NOT_INSTALLED -> {
                        //TODO - Google Play Store is not installed
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
        purchaseSubscriptionOfferOne = findViewById(R.id.purchase_subscription_offer_one)
        purchaseSubscriptionOfferTwo = findViewById(R.id.purchase_subscription_offer_two)
        cancelSubscription = findViewById(R.id.cancel_subscription)

        //init exit app button
        exitApp = findViewById(R.id.exit_app)

        //add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable)
        BounceView.addAnimTo(purchaseNonConsumable)
        BounceView.addAnimTo(purchaseSubscription)
        BounceView.addAnimTo(purchaseSubscriptionOfferOne)
        BounceView.addAnimTo(purchaseSubscriptionOfferTwo)
        BounceView.addAnimTo(cancelSubscription)
        BounceView.addAnimTo(exitApp)
    }

    private fun clickListeners() {
        //purchase an item
        purchaseConsumable.setOnClickListener {
            billingConnector.purchase(this, "consumable_id_1")
        }
        purchaseNonConsumable.setOnClickListener {
            billingConnector.purchase(this, "non_consumable_id_2")
        }

        //purchase a subscription without an offer (only a base plan)
        purchaseSubscription.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_1")
        }

        //purchase a subscription with multiple offers
        //the offer index represents the different offers in the subscription (after Google Billing v5+)
        purchaseSubscriptionOfferOne.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_2", 0)
        }
        purchaseSubscriptionOfferTwo.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_2", 1)
        }

        //cancel a subscription
        cancelSubscription.setOnClickListener {
            billingConnector.unsubscribe(this, "subscription_id_3")
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
         * public final PurchasedResult isPurchased(ProductInfo productInfo)
         *
         * To synchronously check a purchase state
         * */
        for (productInfo in fetchedProductInfoList) {
            when (billingConnector.isPurchased(productInfo)) {
                PurchasedResult.YES -> {
                    //TODO - do something
                    Log.d("BillingConnector", "The product: ${productInfo.product} is purchased")
                }

                PurchasedResult.NO -> {
                    //TODO - do something
                    Log.d(
                        "BillingConnector",
                        "The product: ${productInfo.product} is not purchased"
                    )
                }

                PurchasedResult.CLIENT_NOT_READY -> {
                    //TODO - do something
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${productInfo.product} because client is not ready"
                    )
                }

                PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET -> {
                    //TODO - do something
                    Log.d(
                        "BillingConnector",
                        "Cannot check: ${productInfo.product} because purchased products are not fetched yet"
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
        * To consume consumable products
        * */
        for (purchaseInfo in purchasedInfoList) {
            billingConnector.consumePurchase(purchaseInfo)
        }

        /*
        * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
        *
        * To acknowledge non-consumable products & subscriptions
        * */
        for (purchaseInfo in purchasedInfoList) {
            billingConnector.acknowledgePurchase(purchaseInfo)
        }

        /*
         * public final void purchase(Activity activity, String productId)
         *
         * To purchase a non-consumable/consumable product
         * */
        billingConnector.purchase(this, "product_id")

        /*
         * public final void subscribe(Activity activity, String productId)
         *
         * To purchase a subscription with a base plan
         * */
        billingConnector.subscribe(this, "product_id")

        /*
         * public final void subscribe(Activity activity, String productId, int selectedOfferIndex)
         *
         * To purchase a subscription with multiple offers
         * */
        billingConnector.subscribe(this, "product_id", 1)

        /*
        * public final void unsubscribe(Activity activity, String productId)
        *
        * To cancel a subscription
        * */
        billingConnector.unsubscribe(this, "product_id")
    }
}
