# Dependency

```kotlin
implementation 'com.android.billingclient:billing:3.0.3'
```

# Subscription

```kotlin
package com.example.androidinapp

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.example.androidinapp.databinding.ActivitySubscribeBinding
import com.example.androidinapp.databinding.ItemProductBinding
import java.util.*

object BillingClientSetup {

    private var instance: BillingClient? = null

    fun getInstance(context: Context, purchasesUpdateListener: PurchasesUpdatedListener): BillingClient? {
        if(instance == null) {
            instance = setupBillingClient(context, purchasesUpdateListener)
        }
        return instance
    }

    private fun setupBillingClient(context: Context, purchasesUpdateListener: PurchasesUpdatedListener): BillingClient {
        return BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(purchasesUpdateListener)
            .build()
    }

}

class SubscribeActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var binding: ActivitySubscribeBinding
    private var mBillingClient: BillingClient? = null
    private lateinit var mAcknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscribeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBillingClient()

        //Event
        binding.btnLoadProduct.setOnClickListener {

            if (mBillingClient != null) {
                if(mBillingClient!!.isReady) {
                    val skuDetailPrams = SkuDetailsParams.newBuilder()
                        .setSkusList(listOf("one month", "two month", "three month"))
                        .setType(BillingClient.SkuType.SUBS)
                        .build()
                    mBillingClient!!.querySkuDetailsAsync(skuDetailPrams, object: SkuDetailsResponseListener {
                        override fun onSkuDetailsResponse(
                            billingResult: BillingResult,
                            skuDetailList: MutableList<SkuDetails>?
                        ) {
                            if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                loadAllSubscribedPackage()
                            } else {
                                Toast.makeText(this@SubscribeActivity, "Error code: " + billingResult.responseCode, Toast.LENGTH_SHORT).show()
                            }
                        }

                    })
                }
            }

        }

    }

    private fun setupBillingClient() {
        mAcknowledgePurchaseResponseListener = object: AcknowledgePurchaseResponseListener {
            override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    binding.txtPremium.visibility = View.VISIBLE
                }
            }
        }
        mBillingClient = BillingClientSetup.getInstance(this, this)
        mBillingClient?.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@SubscribeActivity, "Succeed to connect billing", Toast.LENGTH_SHORT).show()
                    //Query
                    val purchaseList = mBillingClient?.queryPurchases(BillingClient.SkuType.SUBS)?.purchasesList
                    if(purchaseList != null && purchaseList.size > 0) {
                        binding.recyclerProduct.visibility = View.GONE
                        for(purchase in purchaseList) {
                            handleItemAlreadyPurchased(purchase)
                        }
                    } else {
                        binding.txtPremium.visibility = View.GONE
                        binding.recyclerProduct.visibility = View.VISIBLE
                        loadAllSubscribedPackage()
                    }
                } else {
                    Toast.makeText(this@SubscribeActivity, "Error code: ${billingResult.responseCode}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@SubscribeActivity, "You are diconnected from Billing Service", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllSubscribedPackage() {
        if(mBillingClient != null && mBillingClient!!.isReady) {
            val skuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(listOf("one month", "two month", "three month"))
                .setType(BillingClient.SkuType.SUBS)
                .build()
            mBillingClient?.querySkuDetailsAsync(skuDetailsParams, object: SkuDetailsResponseListener {
                override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: MutableList<SkuDetails>?) {
                    if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        if(skuDetailsList != null && mBillingClient != null) {
                            val subscriptionProductAdapter = ProductAdapter(this@SubscribeActivity,  skuDetailsList, mBillingClient!!)
                            binding.recyclerProduct.adapter = subscriptionProductAdapter
                        }
                    } else {
                        Toast.makeText(this@SubscribeActivity, "Error: ${billingResult.responseCode}", Toast.LENGTH_SHORT).show()
                    }

                }

            })
        } else {
            Toast.makeText(this, "Billing Client is not ready!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleItemAlreadyPurchased(purchase: Purchase) {
        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if(!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                mBillingClient?.acknowledgePurchase(acknowledgePurchaseParams, mAcknowledgePurchaseResponseListener)
            } else {
                binding.recyclerProduct.visibility = View.GONE
                binding.txtPremium.visibility = View.VISIBLE
                binding.txtPremium.text = "You are a subscribed User!"
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for(purchase in purchaseList) {
                handleItemAlreadyPurchased(purchase)
            }
        } else if(billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED){
            Toast.makeText(this, "User has been cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error: ${billingResult.responseCode}", Toast.LENGTH_SHORT).show()
        }
    }

}

class ProductAdapter(
    private val context: Context,
    private val skuDetailList: MutableList<SkuDetails>,
    private val billingClient: BillingClient
): RecyclerView.Adapter<ProductAdapter.ConsumableProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsumableProductViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemProductBinding.inflate(layoutInflater, parent, false)
        return ConsumableProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConsumableProductViewHolder, position: Int) {
        val skuDetails = skuDetailList[position]
        holder.bind(skuDetails)
    }

    override fun getItemCount(): Int = skuDetailList.size

    inner class ConsumableProductViewHolder(
        private val binding: ItemProductBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(skuDetail: SkuDetails) {
            binding.txtProductName.text = skuDetail.title
            binding.txtDecsription.text = skuDetail.description
            binding.txtPrice.text = skuDetail.price

            itemView.setOnClickListener {
                //Launch Billing Flow
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetail)
                    .build()
                val responseCode = billingClient.launchBillingFlow(context as Activity, billingFlowParams).responseCode
                when(responseCode) {
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        Toast.makeText(context, "BILLING_UNAVAILABLE", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                        Toast.makeText(context, "DEVELOPER_ERROR", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                        Toast.makeText(context, "FEATURE_NOT_SUPPORTED", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Toast.makeText(context, "ITEM_ALREADY_OWNED", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                        Toast.makeText(context, "ITEM_UNAVAILABLE", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        Toast.makeText(context, "ITEM_NOT_OWNED", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                        Toast.makeText(context, "SERVICE_DISCONNECTED", Toast.LENGTH_SHORT).show()
                    }
                    BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                        Toast.makeText(context, "SERVICE_TIMEOUT", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        //do nothing...
                    }
                }
            }

        }
    }

}
```

# Consumer Item

```kotlin
package com.example.androidinapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.example.androidinapp.databinding.ActivityPurchaseItemBinding
import java.lang.StringBuilder
import java.util.*

//object BillingClientSetup {
//
//    private var instance: BillingClient? = null
//
//    fun getInstance(context: Context, purchasesUpdateListener: PurchasesUpdatedListener): BillingClient? {
//        if(instance == null) {
//            instance = setupBillingClient(context, purchasesUpdateListener)
//        }
//        return instance
//    }
//
//    private fun setupBillingClient(context: Context, purchasesUpdateListener: PurchasesUpdatedListener): BillingClient {
//        return BillingClient.newBuilder(context)
//            .enablePendingPurchases()
//            .setListener(purchasesUpdateListener)
//            .build()
//    }
//
//}

class PurchaseItemActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var binding: ActivityPurchaseItemBinding

    private var mBillingClient: BillingClient? = null
    private lateinit var mConsumeResponseListener: ConsumeResponseListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBillingClient()
        setRecyclerView()
        //Event
        binding.btnLoadProduct.setOnClickListener {

            if (mBillingClient != null) {
                if(mBillingClient!!.isReady) {
                    val skuDetailPrams = SkuDetailsParams.newBuilder()
                        .setSkusList(Arrays.asList("item1", "item2", "item3"))
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                    mBillingClient!!.querySkuDetailsAsync(skuDetailPrams, object: SkuDetailsResponseListener {
                        override fun onSkuDetailsResponse(
                            billingResult: BillingResult,
                            skuDetailList: MutableList<SkuDetails>?
                        ) {
                            if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                loadProductToRecyclerView(skuDetailList)
                            } else {
                                Toast.makeText(this@PurchaseItemActivity, "Error code: " + billingResult.responseCode, Toast.LENGTH_SHORT).show()
                            }
                        }

                    })
                }
            }

        }
    }

    private fun loadProductToRecyclerView(skuDetailList: MutableList<SkuDetails>?) {
        if (mBillingClient != null && skuDetailList != null) {
            val consumableProductAdapter = ProductAdapter(this, skuDetailList, mBillingClient!!)
            binding.recyclerProduct.adapter = consumableProductAdapter
        }
    }

    private fun setRecyclerView() {
        binding.recyclerProduct.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerProduct.layoutManager = layoutManager
        binding.recyclerProduct.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

    }

    private fun setupBillingClient() {
        mConsumeResponseListener = object: ConsumeResponseListener {
            override fun onConsumeResponse(billingResult: BillingResult, p1: String) {
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@PurchaseItemActivity, "Consume OK!", Toast.LENGTH_LONG).show()
                }
            }
        }
        mBillingClient = BillingClientSetup.getInstance(this, this)
        mBillingClient?.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@PurchaseItemActivity, "Succeed to connect billing", Toast.LENGTH_SHORT).show()
                    //Query
                    val purchaseList = mBillingClient?.queryPurchases(BillingClient.SkuType.INAPP)?.purchasesList
                    handleItemAlreadyPurchased(purchaseList)
                } else {
                    Toast.makeText(this@PurchaseItemActivity, "Error code: ${billingResult.responseCode}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBillingServiceDisconnected() {
                    Toast.makeText(this@PurchaseItemActivity, "You are diconnected from Billing Service", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleItemAlreadyPurchased(purchaseList: List<Purchase>?) {
        val purchaseItemStringBuilder = StringBuilder(binding.txtPremium.text)
        if(purchaseList != null) {
            for(purchase in purchaseList) {
                if(purchase.sku == "item1") { // Consume Item
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    mBillingClient?.consumeAsync(consumeParams, mConsumeResponseListener)
                }
                purchaseItemStringBuilder
                    .append("\n" + purchase.sku)
                    .append("\n")
            }
        }
        binding.txtPremium.text = purchaseItemStringBuilder.toString() //showing purchased item
        binding.txtPremium.visibility = View.VISIBLE
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchaseList: MutableList<Purchase>?) {
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            handleItemAlreadyPurchased(purchaseList)
        } else if(billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED){
            Toast.makeText(this, "User has been cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error: ${billingResult.responseCode}", Toast.LENGTH_SHORT).show()
        }
    }

}

//class ProductAdapter(
//    private val context: Context,
//    private val skuDetailList: MutableList<SkuDetails>,
//    private val billingClient: BillingClient
//): RecyclerView.Adapter<ProductAdapter.ConsumableProductViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsumableProductViewHolder {
//        val layoutInflater = LayoutInflater.from(parent.context)
//        val binding = ItemProductBinding.inflate(layoutInflater, parent, false)
//        return ConsumableProductViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: ConsumableProductViewHolder, position: Int) {
//        val skuDetails = skuDetailList[position]
//        holder.bind(skuDetails)
//    }
//
//    override fun getItemCount(): Int = skuDetailList.size
//
//    inner class ConsumableProductViewHolder(
//        private val binding: ItemProductBinding
//    ):RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(skuDetail: SkuDetails) {
//            binding.txtProductName.text = skuDetail.title
//            binding.txtDecsription.text = skuDetail.description
//            binding.txtPrice.text = skuDetail.price
//
//            itemView.setOnClickListener {
//                //Launch Billing Flow
//                val billingFlowParams = BillingFlowParams.newBuilder()
//                    .setSkuDetails(skuDetail)
//                    .build()
//                val responseCode = billingClient.launchBillingFlow(context as Activity, billingFlowParams).responseCode
//                when(responseCode) {
//                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
//                        Toast.makeText(context, "BILLING_UNAVAILABLE", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
//                        Toast.makeText(context, "DEVELOPER_ERROR", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
//                        Toast.makeText(context, "FEATURE_NOT_SUPPORTED", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
//                        Toast.makeText(context, "ITEM_ALREADY_OWNED", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
//                        Toast.makeText(context, "ITEM_UNAVAILABLE", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
//                        Toast.makeText(context, "ITEM_NOT_OWNED", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
//                        Toast.makeText(context, "SERVICE_DISCONNECTED", Toast.LENGTH_SHORT).show()
//                    }
//                    BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
//                        Toast.makeText(context, "SERVICE_TIMEOUT", Toast.LENGTH_SHORT).show()
//                    }
//                    else -> {
//                        //do nothing...
//                    }
//                }
//            }
//
//        }
//    }
//
//}
```