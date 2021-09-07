package com.wsoteam.horoscopes

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.amplitude.api.Amplitude
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.firebase.FirebaseApp
import com.qonversion.android.sdk.Qonversion
import com.wsoteam.horoscopes.utils.id.Creator
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaConfig
import java.io.IOException

class App : MultiDexApplication() {

    @Volatile
    var applicationHandler: Handler? = null
    private val afDevKey = "fTHMhfusDFFptFAiXDJ2fU"

    override fun onCreate() {
        super.onCreate()
        sInstance = this
        val config =
            YandexMetricaConfig.newConfigBuilder(getString(R.string.yam_id)).build()
        YandexMetrica.activate(applicationContext, config)
        YandexMetrica.enableActivityAutoTracking(this)
        Amplitude.getInstance()
            .initialize(this, getString(R.string.amplitude_id))
            .enableForegroundTracking(this)

        applicationHandler =  Handler(applicationContext.mainLooper)


        Qonversion.initialize(this, getString(R.string.qonversion_id), Creator.getId())
        //Smartlook.setupAndStartRecording(getString(R.string.smartlock_id))
        FacebookSdk.sdkInitialize(this)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        AppEventsLogger.activateApp(applicationContext)

        ///////////////////
        FirebaseApp.initializeApp(this)

        val conversionDataListener  = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                campaign = data!!["campaign"].toString()
            }
            override fun onConversionDataFail(error: String?) {
                //Log.e("LOG_TAG", "error onAttributionFailure :  $error")
            }
            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                data?.map {
                    //Log.d("LOG_TAG", "onAppOpen_attribute: ${it.key} = ${it.value}")
                }
            }
            override fun onAttributionFailure(error: String?) {
                //Log.e("LOG_TAG", "error onAttributionFailure :  $error")
            }
        }
        AppsFlyerLib.getInstance().init(afDevKey, conversionDataListener, applicationContext)
        AppsFlyerLib.getInstance().start(this)
        ///////////////////////////
    }

    fun getAdvertisingId(context: Context) {
        AsyncTask.execute {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                gadid = adInfo?.id
            } catch (exception: IOException) {
                Log.i("TAG_EXCEPTION", exception.toString())
            } catch (exception: GooglePlayServicesRepairableException) {
                Log.i("TAG_EXCEPTION", exception.toString())
            } catch (exception: GooglePlayServicesNotAvailableException) {
                Log.i("TAG_EXCEPTION", exception.toString())
            }
        }
    }

    companion object {
        var campaign: String = "null"
        var gadid:String? = null

        private lateinit var sInstance: App

        fun getInstance(): App {
            return sInstance
        }
    }
}