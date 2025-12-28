package eu.faircode.netguard

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.vending.billing.IInAppBillingService
import org.json.JSONException
import org.json.JSONObject

class IAB(
    private val delegate: Delegate,
    context: Context,
) : ServiceConnection {
    private val context: Context = context.applicationContext
    private var service: IInAppBillingService? = null

    interface Delegate {
        fun onReady(iab: IAB)
    }

    fun bind() {
        Log.i(TAG, "Bind")
        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.setPackage("com.android.vending")
        context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (service != null) {
            Log.i(TAG, "Unbind")
            context.unbindService(this)
            service = null
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Log.i(TAG, "Connected")
        service = IInAppBillingService.Stub.asInterface(binder)
        delegate.onReady(this)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.i(TAG, "Disconnected")
        service = null
    }

    @Throws(RemoteException::class, JSONException::class)
    fun isAvailable(sku: String): Boolean {
        val skuList = ArrayList<String>()
        skuList.add(sku)
        val query = Bundle()
        query.putStringArrayList("ITEM_ID_LIST", skuList)
        val bundle = service?.getSkuDetails(IAB_VERSION, context.packageName, "inapp", query)
        Log.i(TAG, "getSkuDetails")
        Util.logBundle(bundle)
        val response = bundle?.getInt("RESPONSE_CODE", -1) ?: -1
        Log.i(TAG, "Response=" + getResult(response))
        if (response != 0) throw IllegalArgumentException(getResult(response))

        var found = false
        val details = bundle?.getStringArrayList("DETAILS_LIST")
        if (details != null) {
            for (item in details) {
                val obj = JSONObject(item)
                if (sku == obj.getString("productId")) {
                    found = true
                    break
                }
            }
        }
        Log.i(TAG, "$sku=$found")
        return found
    }

    @Throws(RemoteException::class)
    fun updatePurchases() {
        val bundle = service?.getPurchases(IAB_VERSION, context.packageName, "inapp", null)
        Log.i(TAG, "getPurchases")
        Util.logBundle(bundle)
        val response = bundle?.getInt("RESPONSE_CODE", -1) ?: -1
        Log.i(TAG, "Response=" + getResult(response))
        if (response != 0) throw IllegalArgumentException(getResult(response))

        val list = bundle?.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: arrayListOf()
        for (item in list) {
            try {
                val obj = JSONObject(item)
                val product = obj.getString("productId")
                if (ActivityPro.SKU_DONATION != product) {
                    setBought(product, context)
                }
            } catch (ex: Throwable) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
            }
        }
    }

    @Suppress("DEPRECATION")
    @Throws(RemoteException::class)
    fun getBuyIntent(sku: String, isDonation: Boolean): PendingIntent? {
        val bundle = service?.getBuyIntent(IAB_VERSION, context.packageName, sku, "inapp", "")
        Log.i(TAG, "getBuyIntent")
        Util.logBundle(bundle)
        val response = bundle?.getInt("RESPONSE_CODE", -1) ?: -1
        Log.i(TAG, "Response=" + getResult(response))
        if (response != 0) throw IllegalArgumentException(getResult(response))

        val intent = bundle?.getParcelable("BUY_INTENT") as PendingIntent?
        if (isDonation) setBought(ActivityPro.SKU_DONATION, context)
        return intent
    }

    companion object {
        private const val TAG = "NetGuard.IAB"
        private const val IAB_VERSION = 3

        fun setBought(sku: String, context: Context) {
            Log.i(TAG, "Bought $sku")
            val prefs = context.getSharedPreferences("IAB", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(sku, true).apply()
        }

        fun isPurchased(sku: String, context: Context): Boolean {
            return try {
                if (Util.isDebuggable(context)) {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    return !prefs.getBoolean("debug_iab", false)
                }

                val prefs = context.getSharedPreferences("IAB", Context.MODE_PRIVATE)
                if (ActivityPro.SKU_SUPPORT1 == sku || ActivityPro.SKU_SUPPORT2 == sku) {
                    return prefs.getBoolean(sku, false)
                }

                prefs.getBoolean(sku, false) ||
                    prefs.getBoolean(ActivityPro.SKU_PRO1, false) ||
                    prefs.getBoolean(ActivityPro.SKU_SUPPORT1, false) ||
                    prefs.getBoolean(ActivityPro.SKU_SUPPORT2, false) ||
                    prefs.getBoolean(ActivityPro.SKU_DONATION, false)
            } catch (ignored: SecurityException) {
                false
            }
        }

        fun isPurchasedAny(context: Context): Boolean {
            return try {
                if (Util.isDebuggable(context)) {
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    return !prefs.getBoolean("debug_iab", false)
                }

                val prefs = context.getSharedPreferences("IAB", Context.MODE_PRIVATE)
                for (key in prefs.all.keys) {
                    if (prefs.getBoolean(key, false)) return true
                }
                false
            } catch (ignored: SecurityException) {
                false
            }
        }

        private fun getResult(response: Int): String {
            return when (response) {
                0 -> "OK"
                1 -> "User canceled"
                2 -> "Service unavailable"
                3 -> "Billing unavailable"
                4 -> "Item unavailable"
                5 -> "Developer error"
                6 -> "Error"
                7 -> "Item already owned"
                8 -> "Item not owned"
                else -> "Unknown"
            }
        }
    }
}
