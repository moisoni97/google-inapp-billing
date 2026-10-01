package games.moisoni.google_inapp_billing

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.hariprasanths.bounceview.BounceView
import games.moisoni.google_iab.BillingConnector
import games.moisoni.google_iab.listener.BillingEventListener
import games.moisoni.google_iab.type.ErrorType
import games.moisoni.google_iab.status.PurchasedResult
import games.moisoni.google_iab.type.ProductType
import games.moisoni.google_iab.status.SupportState
import games.moisoni.google_iab.model.BillingResponse
import games.moisoni.google_iab.model.ProductInfo
import games.moisoni.google_iab.model.PurchaseInfo

/**
 * This is a sample app to demonstrate how to implement 'google-inapp-billing' library
 * <p>
 * This standalone app won't work because it's just for reference
 * <p>
 * To see real results, you need to implement the code below in a real project
 * released on Play Console and create your own in-app products IDs
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

    // Fetched products (keyed by product ID) for example purposes to demonstrate how to synchronously check a purchase state
    // The onProductsFetched callback is triggered again after a reconnection, so a product is replaced instead of being added twice
    private val fetchedProducts = linkedMapOf<String, ProductInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)

        initViews()
        initializeBillingClient()
        clickListeners()
    }

    private fun initializeBillingClient() {
        // Create a list with consumable IDs
        val consumableIds = mutableListOf<String>()
        consumableIds.add("consumable_id_1")
        consumableIds.add("consumable_id_2")
        consumableIds.add("consumable_id_3")

        // Create a list with non-consumable IDs
        val nonConsumableIds = mutableListOf<String>()
        nonConsumableIds.add("non_consumable_id_1")
        nonConsumableIds.add("non_consumable_id_2")
        nonConsumableIds.add("non_consumable_id_3")

        // Create a list with subscription IDs
        val subscriptionIds = mutableListOf<String>()
        subscriptionIds.add("subscription_id_1")
        subscriptionIds.add("subscription_id_2")
        subscriptionIds.add("subscription_id_3")

        billingConnector = BillingConnector(
            this,
            "license_key", // "license_key" - the license key (public key) from Play Console, used to verify purchase signatures
            lifecycle
        )
            .setConsumableIds(consumableIds) // To set consumable IDs - call only for consumable products
            .setNonConsumableIds(nonConsumableIds) // To set non-consumable IDs - call only for non-consumable products
            .setSubscriptionIds(subscriptionIds) // To set subscription IDs - call only for subscription products
            .autoAcknowledge() // Recommended - acknowledges non-consumables and subscriptions automatically. Alternatively, call the public method "acknowledgePurchase(PurchaseInfo purchaseInfo)"
            .autoConsume() // Recommended - consumes consumables automatically. Alternatively, call the public method "consumePurchase(PurchaseInfo purchaseInfo)"
            .enableLogging() // To enable logging for debugging throughout the library - this can be skipped
            .connect() // To connect the billing client with Google Play

        billingConnector.setBillingEventListener(object :
            BillingEventListener {
            override fun onProductsFetched(productDetails: List<ProductInfo>) {
                /*
                 * Provides the details of the products available for purchase
                 *
                 * Triggered separately for in-app products and subscriptions after connecting,
                 * and again after the connection is re-established
                 * */

                var product: String
                var price: String? // null for subscriptions (they have no one-time purchase offer)

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

                    //TODO - similarly check for other IDs

                    fetchedProducts[productInfo.product] = productInfo
                }
            }

            override fun onPurchasedProductsFetched(
                productType: ProductType,
                purchases: List<PurchaseInfo>
            ) {
                /*
                * This will be called even when no purchased products are returned by the API
                *
                * It is triggered after connecting and again every time purchases are refreshed
                * (each time the activity resumes, or when refreshPurchases() is called)
                * */

                when (productType) {
                    ProductType.INAPP -> {
                        // Triggered for in-app (one-time) purchases
                        //TODO - restore non-consumable purchases
                    }

                    ProductType.SUBS -> {
                        // Triggered for subscription products
                        //TODO - restore subscriptions
                    }

                    ProductType.COMBINED -> {
                        // Never passed to this callback, new purchases are delivered to onProductsPurchased
                    }
                }

                /*
                * Restore entitlements only for NON-CONSUMABLE products and SUBSCRIPTIONS that are PURCHASED and acknowledged
                *
                * PENDING purchases are listed too, and CONSUMABLE products are granted in onPurchaseConsumed
                * Purchases that are not acknowledged yet are acknowledged by the library right after this callback,
                * they are granted in onPurchaseAcknowledged (Google refunds a purchase that can't be acknowledged)
                * */
                purchases.filter { it.isPurchased && it.isAcknowledged }.forEach {
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

                        //TODO - similarly check for other IDs
                    }
                }
            }

            override fun onProductsPurchased(purchases: List<PurchaseInfo>) {
                /*
                 * Triggered when a purchase flow finishes successfully
                 *
                 * Don't grant entitlement here: the purchase can still be PENDING (e.g. cash payments)
                 * Grant it in onPurchaseAcknowledged (non-consumables, subscriptions) or onPurchaseConsumed (consumables)
                 * */

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

                    //TODO - similarly check for other IDs
                }
            }

            override fun onPurchaseAcknowledged(purchase: PurchaseInfo) {
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

                    //TODO - similarly check for other IDs
                }
            }

            override fun onPurchaseConsumed(purchase: PurchaseInfo) {
                /*
                 * Grant user entitlement for CONSUMABLE products here
                 *
                 * Only PURCHASED (paid) purchases are consumed, so the payment is complete at this point
                 * A purchase is consumed only once, so the item can be added to the user's balance (e.g. coins)
                 * If multi-quantity purchases are enabled in Play Console, grant purchase.getQuantity() items
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

                    //TODO - similarly check for other IDs
                }
            }

            override fun onProductQueryError(
                productId: String,
                response: BillingResponse
            ) {
                /*
                 * Triggered when Google Play doesn't return the details of a product ID
                 *
                 * The message contains the reason: not found (not created or not active in Play Console),
                 * invalid product ID format, or no offer the user is eligible for
                 * */

                //TODO - do something
                Log.d("BillingConnector", "Product query error: ${response.debugMessage}")
                Toast.makeText(
                    this@KotlinSampleActivity,
                    "Product query error: ${response.debugMessage}",
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
                         * This will be triggered when a consumable purchase has a PENDING state (reported once per purchase)
                         * User entitlement must be granted when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase completes
                         * and to come back later to receive their purchase
                         * isPurchasePending() / purchase.isPending() can be used to show the pending state later on
                         * */
                        //TODO - warning during consumption
                    }

                    ErrorType.ACKNOWLEDGE_ERROR -> {
                        //TODO - error during acknowledgment
                    }

                    ErrorType.ACKNOWLEDGE_WARNING -> {
                        /*
                         * This will be triggered when a purchase can not be acknowledged because the state is PENDING (reported once per purchase)
                         * A purchase can be acknowledged only when the state is PURCHASED
                         *
                         * PENDING transactions usually occur when users choose cash as their form of payment
                         *
                         * Here users can be informed that it may take a while until the purchase completes
                         * and to come back later to receive their purchase
                         * isPurchasePending() / purchase.isPending() can be used to show the pending state later on
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
                        /*
                        * The library automatically refreshes purchases after this error,
                        * so a consumable that was not consumed yet gets consumed
                        * */
                        //TODO - the purchase failed because the item is already owned
                    }

                    ErrorType.ITEM_NOT_OWNED -> {
                        //TODO - failure to consume since item is not owned
                    }

                    ErrorType.PLAY_STORE_NOT_INSTALLED -> {
                        //TODO - Google Play Store is not installed
                    }

                    ErrorType.SIGNATURE_VERIFICATION_FAILED -> {
                        /*
                        * The purchase signature doesn't match the license key (wrong key or tampered purchase)
                        * The purchase is ignored (not acknowledged / consumed)
                        * */
                        //TODO - purchase signature verification failed
                    }

                    ErrorType.FEATURE_NOT_SUPPORTED -> {
                        //TODO - the requested feature is not supported by Google Play on this device
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
        // Init purchase buttons
        purchaseConsumable = findViewById(R.id.purchase_consumable)
        purchaseNonConsumable = findViewById(R.id.purchase_non_consumable)
        purchaseSubscription = findViewById(R.id.purchase_subscription)
        purchaseSubscriptionOfferOne = findViewById(R.id.purchase_subscription_offer_one)
        purchaseSubscriptionOfferTwo = findViewById(R.id.purchase_subscription_offer_two)
        cancelSubscription = findViewById(R.id.cancel_subscription)

        // Init exit app button
        exitApp = findViewById(R.id.exit_app)

        // Add bounce view animation to clickable views
        BounceView.addAnimTo(purchaseConsumable)
        BounceView.addAnimTo(purchaseNonConsumable)
        BounceView.addAnimTo(purchaseSubscription)
        BounceView.addAnimTo(purchaseSubscriptionOfferOne)
        BounceView.addAnimTo(purchaseSubscriptionOfferTwo)
        BounceView.addAnimTo(cancelSubscription)
        BounceView.addAnimTo(exitApp)
    }

    private fun clickListeners() {
        // Purchase an item
        purchaseConsumable.setOnClickListener {
            billingConnector.purchase(this, "consumable_id_1")
        }
        purchaseNonConsumable.setOnClickListener {
            billingConnector.purchase(this, "non_consumable_id_2")
        }

        // Purchase the first offer of a subscription (index 0)
        // Fine for a subscription with a single base plan and no offers, otherwise select the offer by ID (see below)
        purchaseSubscription.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_1")
        }

        // Purchase a subscription with a specific base plan / offer (IDs from Play Console)
        // Pass null as the offer ID to purchase the base plan without an offer
        purchaseSubscriptionOfferOne.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_2", "base_plan_id", "offer_id_1")
        }
        purchaseSubscriptionOfferTwo.setOnClickListener {
            billingConnector.subscribe(this, "subscription_id_2", "base_plan_id", "offer_id_2")
        }

        // Open the Google Play page where the user can cancel the subscription
        cancelSubscription.setOnClickListener {
            billingConnector.unsubscribe(this, "subscription_id_3")
        }

        // Exit app on button click
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
        for (productInfo in fetchedProducts.values) {
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
         * public final PurchasedResult isPurchased(String productId)
         *
         * To synchronously check a purchase state by product ID string
         * */
        val isPurchasedResult = billingConnector.isPurchased("non_consumable_id_1")
        if (isPurchasedResult == PurchasedResult.YES) {
            //TODO - do something
            Log.d("BillingConnector", "Product is purchased")
        }

        /*
         * public boolean isSubscriptionActive(String productId)
         *
         * To check if a subscription is currently active (PURCHASED state)
         * */
        val isSubsActive = billingConnector.isSubscriptionActive("subscription_id_1")
        Log.d("BillingConnector", "Is subscription active: $isSubsActive")

        /*
         * public boolean isSubscriptionAutoRenewing(String productId)
         *
         * To check if an active subscription is currently auto-renewing
         * */
        val isSubsAutoRenewing = billingConnector.isSubscriptionAutoRenewing("subscription_id_1")
        Log.d("BillingConnector", "Is subscription auto-renewing: $isSubsAutoRenewing")

        /*
         * public boolean isPurchasePending(String productId)
         *
         * To check if a purchase is waiting for payment completion
         * */
        val isPending = billingConnector.isPurchasePending("consumable_id_1")
        Log.d("BillingConnector", "Is purchase pending: $isPending")

        /*
         * public boolean isPlayStoreInstalled(Context context)
         *
         * To check if Google Play Store and Billing Service are available
         * */
        val isPlayStoreInstalled = billingConnector.isPlayStoreInstalled(this)
        Log.d("BillingConnector", "Is Play Store installed: $isPlayStoreInstalled")

        /*
         * public List<PurchaseInfo> getPurchasedProductsList()
         *
         * Returns a read-only snapshot of all currently owned products
         * */
        val allPurchases = billingConnector.purchasedProductsList
        Log.d("BillingConnector", "Total owned purchases: ${allPurchases.size}")

        /*
         * public ProductInfo getProductInfo() (PurchaseInfo)
         *
         * Returns the product details of a purchase
         * Can be null when Google Play doesn't return details for an owned product (e.g. deactivated in Play Console)
         * */
        for (purchaseInfo in allPurchases) {
            purchaseInfo.productInfo?.let {
                Log.d("BillingConnector", "Owned product title: ${it.title}")
            }
        }

        /*
         * public final void refreshPurchases()
         *
         * To re-sync owned purchases with Google Play (e.g. from a "Restore purchases" button)
         * Called automatically each time the activity resumes when a Lifecycle is passed to the constructor
         * */
        billingConnector.refreshPurchases()

        /*
         * public void consumePurchase(PurchaseInfo purchaseInfo)
         *
         * To consume consumable products (only needed without autoConsume())
         * Every owned purchase can be passed, the ones that are not consumables are ignored
         * */
        for (purchaseInfo in allPurchases) {
            billingConnector.consumePurchase(purchaseInfo)
        }

        /*
         * public void acknowledgePurchase(PurchaseInfo purchaseInfo)
         *
         * To acknowledge non-consumable products & subscriptions (only needed without autoAcknowledge())
         * Every owned purchase can be passed, consumables and already acknowledged purchases are ignored
         * */
        for (purchaseInfo in allPurchases) {
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
         * To purchase the first offer returned by Google Play (index 0)
         * Fine for a subscription with a single base plan and no offers, otherwise select the offer by ID
         * */
        billingConnector.subscribe(this, "product_id")

        /*
         * public final void subscribe(Activity activity, String productId, String basePlanId, String offerId)
         *
         * To purchase a subscription with a specific base plan / offer (recommended)
         * Pass null as the offer ID to purchase the base plan without an offer
         * */
        billingConnector.subscribe(this, "product_id", "base_plan_id", null)
        billingConnector.subscribe(this, "product_id", "base_plan_id", "offer_id")

        /*
         * public final void subscribe(Activity activity, String productId, int selectedOfferIndex)
         *
         * To purchase a subscription with multiple offers by index
         * Google Play doesn't guarantee the order of offers, prefer selecting them by ID
         * */
        billingConnector.subscribe(this, "product_id", 1)

        /*
         * public final void unsubscribe(Activity activity, String productId)
         *
         * To cancel a subscription (opens the Google Play subscription settings)
         * Pass null to open the general subscriptions page
         * */
        billingConnector.unsubscribe(this, "product_id")
    }
}
