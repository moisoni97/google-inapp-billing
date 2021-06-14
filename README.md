# Google In-App Billing Library v4+
A simple implementation of the Android In-App Billing API.

It supports: in-app purchases (both consumable and non-consumable) and subscriptions.

![image preview](https://i.postimg.cc/Bbf6Cgd2/Google-In-App-Billing-Sample-App.jpg)
![video example](https://i.postimg.cc/DZX0sDY2/Google-In-App-Billing-Purchase.gif)

# Getting Started

* You project should build against Android 5.0 (minSdkVersion 21).

* Add the JitPack repository to your project's build.gradle file

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

* Add the dependency in your app's build.gradle file

```
dependencies {
    implementation 'com.github.moisoni97:google-inapp-billing:1.0.1'
}
```

* Open the AndroidManifest.xml of your application and add this permission:

```
  <uses-permission android:name="com.android.vending.BILLING" />
```

# Usage

* Create an instance of BillingConnector class. Constructor will take 2 parameters:
  - *Context*
  - *License key from `Play Console`*
  
```
billingConnector = new BillingConnector(this, "license_key")
                .setConsumableIds(consumableIds)
                .setNonConsumableIds(nonConsumableIds)
                .setSubscriptionIds(subscriptionIds)
                .autoAcknowledge()
                .autoConsume()
                .enableLogging()
                .connect();
```

* Implement the listener to handle event results and errors:

```
billingConnector.setBillingEventListener(new BillingEventListener() {
            @Override
            public void onProductsFetched(@NonNull List<SkuInfo> skuDetails) {
                /*Provides a list with fetched products*/
            }

            @Override
            public void onPurchasedProductsFetched(@NonNull List<PurchaseInfo> purchases) {
                /*Provides a list with fetched purchased products*/
            }

            @Override
            public void onProductsPurchased(@NonNull List<PurchaseInfo> purchases) {
                /*Callback after a product is purchased*/
            }

            @Override
            public void onPurchaseAcknowledged(@NonNull PurchaseInfo purchase) {
                /*Callback after a purchase is acknowledged*/
            }

            @Override
            public void onPurchaseConsumed(@NonNull PurchaseInfo purchase) {
                /*Callback after a purchase is consumed*/
            }

            @Override
            public void onBillingError(@NonNull BillingConnector billingConnector, @NonNull BillingResponse response) {
                /*Callback after an error occurs*/
            }
        });
```

* Make a purchase

```
billingConnector.purchase(this, "sku_id");
```

# Sample App

Go through the sample app to see a more advanced integration of the library. 

It also shows how to implement some `useful public methods`.
