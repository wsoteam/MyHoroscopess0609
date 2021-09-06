package com.wsoteam.horoscopes

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import com.amplitude.api.Amplitude
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.wsoteam.horoscopes.bl.Client
import com.wsoteam.horoscopes.models.Sign
import com.wsoteam.horoscopes.notification.AlarmReceiver
import com.wsoteam.horoscopes.notification.EveningAlarmReceiver
import com.wsoteam.horoscopes.presentation.form.FormActivity
import com.wsoteam.horoscopes.presentation.main.CacheData
import com.wsoteam.horoscopes.presentation.main.ICachedData
import com.wsoteam.horoscopes.presentation.main.MainVM
import com.wsoteam.horoscopes.presentation.onboarding.EnterActivity
import com.wsoteam.horoscopes.utils.Analytics
import com.wsoteam.horoscopes.utils.PreferencesProvider
import com.wsoteam.horoscopes.utils.ads.AdWorker
import com.wsoteam.horoscopes.utils.ads.BannerFrequency
import com.wsoteam.horoscopes.utils.analytics.Analytic
import com.wsoteam.horoscopes.utils.analytics.FBAnalytic
import com.wsoteam.horoscopes.utils.choiceSign
import com.wsoteam.horoscopes.utils.loger.L
import com.wsoteam.horoscopes.utils.remote.ABConfig
import kotlinx.android.synthetic.main.splash_activity.*
import kotlinx.android.synthetic.main.stories_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit


class SplashActivity : AppCompatActivity(R.layout.splash_activity) {

    var counter = 0
    var MAX = 4
    var isNextScreenLoading = false
    lateinit var vm: MainVM
    var isAdLoaded = false

    var isCanGoNext = false


    lateinit var view: WebView
    var counterBack = 0
    val MAX_BEFORE_SKIP = 2
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null
    private var instanceState: Bundle? = null
    private val IMG_PICK = 1


    private fun postGoNext(i: Int, tag: String) {
        counter += i
       // L.log("postGoNext -- $counter -- $tag")
        if (counter >= MAX && isCanGoNext) {
            if (!isNextScreenLoading) {
                isNextScreenLoading = true
                goNext()
            }
        }
    }

    private fun goNext() { //
        //L.log("goNext")
        Analytics.openWhite()
        var intent: Intent
        if (PreferencesProvider.getName() != "" && PreferencesProvider.getBirthday() != "") {
            intent = Intent(this, MainActivity::class.java)

        } else {
            if (!PreferencesProvider.isShowOnboard) {
                PreferencesProvider.isShowOnboard = true
                intent = Intent(this, EnterActivity::class.java)

            } else {
                intent = Intent(this, FormActivity::class.java)

            }
        }
        startActivity(intent)
        finish()
    }

    private fun goBlack() {
        Analytics.openBlack()
        makeWorkScren()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (view != null) {
            view.saveState(outState)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (view != null) {
            view.restoreState(savedInstanceState)
        }
    }

    override fun onBackPressed() {
        if (view != null) {
            if (view.canGoBack()) {
                view.goBack()
            } else {
                counterBack++
                if (counterBack >= MAX_BEFORE_SKIP) {
                    counterBack = 0
                    var url = PreferencesProvider.startUrl
                    view.loadUrl(url)
                }
            }
        }else{
            super.onBackPressed()
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        var results = arrayOf(Uri.parse(data!!.dataString))
        mUploadMessage!!.onReceiveValue(results)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun makeWorkScren() {
        fl_splash.visibility = View.GONE

        view = WebView(this)

        view.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        fl_container.addView(view)


        view.settings.allowFileAccess = true
        view.settings.allowFileAccess = true
        view.settings.allowContentAccess = true
        view.settings.supportZoom()
        view.settings.useWideViewPort = true

        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.userAgentString =
            view.settings.userAgentString + "MobileAppClient/Android/0.9"
        view.webViewClient = Client()


        if (instanceState == null) {
            if (PreferencesProvider.lastUrl == "") {
                var url = PreferencesProvider.url
                view.loadUrl(url)//URLL//url
                // Log.e("LOOL", "url:  $url")
            } else {
                var url = PreferencesProvider.lastUrl
                view.loadUrl(url)//URLL//url
            }
        }

        view.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mUploadMessage = filePathCallback
                val pickIntent = Intent()
                pickIntent.type = "image/*"
                pickIntent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(pickIntent, "Select Picture"), IMG_PICK)
                return true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdWorker.unSubscribe()
    }

    private fun getKey(){
        val info: PackageInfo
        try {
            info = packageManager.getPackageInfo("", PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                var md: MessageDigest
                md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val something = String(Base64.encode(md.digest(), 0))
                //String something = new String(Base64.encodeBytes(md.digest()));
                //Log.e("LOL", something)
            }
        } catch (e1: PackageManager.NameNotFoundException) {
            //Log.e("name not found", e1.toString())
        } catch (e: NoSuchAlgorithmException) {
            //Log.e("no such an algorithm", e.toString())
        } catch (e: Exception) {
            //Log.e("exception", e.toString())
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(this)
        Analytics.open()
        BannerFrequency.runSetup()
       if (!PreferencesProvider.isSetuped) {
            AppEventsLogger
                .newLogger(this)
                .logEvent("fb_mobile_first_app_launch")
            FBAnalytic.logFirstLaunch(this)
            PreferencesProvider.isSetuped = true
        }

        //Server query ollo
        var vm = ViewModelProviders
            .of(this)
            .get(MainVM::class.java)
        vm.preLoadData()

        vm.getStatusLD().observe(this, androidx.lifecycle.Observer {
            when (it) {
                MainVM.BLACK -> {
                    goBlack()
                }
                MainVM.WHITE -> {
                    isCanGoNext = true
                    postGoNext(1, "")
                }
            }
        })
        //goNext() //

        if (PreferencesProvider.getBirthday() != "") {
            CacheData.setObserver(object : ICachedData {
                override fun cachedDataReady() {
                    makeCurrentScreen(CacheData.getCachedData()!!)
                    CacheData.removeObservers()
                }
            })
        }

        bindRetention()
        Analytic.start()
        PreferencesProvider.setBeforePremium(Analytic.start_premium)
        bindTest()
        refreshNotifications()
        try {
            trackUser()
        } catch (ex: Exception) {
            L.log("crash")
            Analytic.crashAttr()
        }
        CoroutineScope(Dispatchers.IO).launch {
            TimeUnit.SECONDS.sleep(4)
            if (isAdLoaded){
                postGoNext(2, "sleep4")
            }else{
                postGoNext(1, "sleep4")
            }
            Log.e("LOL", "sleep")
        }
        if (intent.getStringExtra(Config.OPEN_FROM_NOTIFY) != null) {
            when (intent.getStringExtra(Config.OPEN_FROM_NOTIFY)) {
                Config.OPEN_FROM_NOTIFY -> {
                    Analytic.openFromNotif()
                    PreferencesProvider.setLastDayNotification(PreferencesProvider.DEF_TODAY_NOTIF)
                }
                Config.OPEN_FROM_EVENING_NOTIF -> Analytic.openFromEveningNotif()
            }
        }
    }

    private fun makeCurrentScreen(it: List<Sign>) {
        val index = choiceSign(PreferencesProvider.getBirthday()!!)
        tvTopTitleStories.text = resources.getStringArray(R.array.names_signs)[index]
        ivSignStories.setImageResource(
            resources.obtainTypedArray(R.array.imgs_signs)
                .getResourceId(index, -1)
        )
        tvTitleStories.text = "${getString(R.string.my_horoscope_on)} ${Calendar.getInstance()
            .get(Calendar.DAY_OF_MONTH)} ${Calendar.getInstance()
            .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)}"
        tvTextStories.setText(getCutText(it[index].today.text))

        CoroutineScope(Dispatchers.IO).launch {
            TimeUnit.SECONDS.sleep(2)
            //makeImage()
        }
    }

    /*private fun makeImage() {
        var view = findViewById<View>(R.id.llParentLayout)
        view.isDrawingCacheEnabled = true
        view.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        view.buildDrawingCache()
        val uri = saveImage(view.drawingCache, applicationContext)
        view.isDrawingCacheEnabled = false

        PreferencesProvider.screenURI = uri.toString()
        postGoNext(1, "screenURI")
    }*/

    private fun getCutText(text: String): String {
        var array = text.split(".")
        var cutString = ""
        for (i in 0..1) {
            cutString = "$cutString${array[i]}. "
        }
        return cutString
    }

    private fun saveImage(bitmap: Bitmap, context: Context): Uri? {
        var imagesFolder = File(context.cacheDir, "images")
        var uri: Uri? = null

        try {
            imagesFolder.mkdirs()
            var file = File(imagesFolder, "shared_image.png")

            var outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            uri = FileProvider.getUriForFile(context, "com.mydomain.fileprovider", file)
        } catch (ex: Exception) {
            L.log("save error")
        }
        return uri
    }

    private fun bindRetention() {
        var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (PreferencesProvider.firstEnter == -1) {
            PreferencesProvider.firstEnter = currentDay
        } else {
            when (currentDay - PreferencesProvider.firstEnter) {
                2 -> FBAnalytic.logTwoDays(this)
                7 -> FBAnalytic.logSevenDays(this)
            }
        }
    }


    private fun bindTest() {
        val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setDefaults(R.xml.default_config)

        firebaseRemoteConfig.fetch(3600).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                firebaseRemoteConfig.activateFetched()
                Amplitude.getInstance().logEvent("norm_ab")
            } else {
                Amplitude.getInstance().logEvent("crash_ab")
            }
            setABTestConfig(
                firebaseRemoteConfig.getString(ABConfig.REQUEST_STRING),
                firebaseRemoteConfig.getLong(ABConfig.REQUEST_STRING_PRICE).toInt()
            )
        }
    }

    private fun setABTestConfig(version: String, priceIndex: Int) {
        L.log("set test")
        L.log("$priceIndex")
        PreferencesProvider.setVersion(version)
        PreferencesProvider.priceIndex = priceIndex
        Analytic.setABVersion(version, priceIndex)
        Analytic.setVersion()
        postGoNext(1, "setABTestConfig")
    }

    private fun refreshNotifications() {
        if (PreferencesProvider.getNotifTime() == PreferencesProvider.DEFAULT_TIME_NOTIFY && PreferencesProvider.getNotifStatus()) {
            AlarmReceiver.startNotification(this, Config.DEF_HOUR_NOTIF, Config.DEF_MIN_NOTIF)
            EveningAlarmReceiver.startNotification(
                this,
                Config.DEF_HOUR_EVENING_NOTIF,
                Config.DEF_MIN_EVENING_NOTIF
            )
            L.log("refreshNotifications")
        } else if (PreferencesProvider.getNotifTime() != PreferencesProvider.DEFAULT_TIME_NOTIFY && PreferencesProvider.getNotifStatus()) {
            val (hours, minutes) = PreferencesProvider.getNotifTime().split(":").map { it.toInt() }
            L.log("$hours $minutes refresh")
            AlarmReceiver.startNotification(this, hours, minutes)
            EveningAlarmReceiver.startNotification(
                this,
                Config.DEF_HOUR_EVENING_NOTIF,
                Config.DEF_MIN_EVENING_NOTIF
            )
        }
    }

    private fun trackUser() {
        var client = InstallReferrerClient.newBuilder(this).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> sendAnal(client.installReferrer.installReferrer)
                    InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR -> sendAnal("DEVELOPER_ERROR")
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> sendAnal(
                        "FEATURE_NOT_SUPPORTED"
                    )
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED -> sendAnal("SERVICE_DISCONNECTED")
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> sendAnal("SERVICE_UNAVAILABLE")
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                sendAnal("onInstallReferrerServiceDisconnected")
            }
        })
    }

    private fun sendAnal(s: String) {
        val clickId = getClickId(s)
        var mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        var bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, clickId)
        bundle.putString(FirebaseAnalytics.Param.MEDIUM, clickId)
        bundle.putString(FirebaseAnalytics.Param.SOURCE, clickId)
        bundle.putString(FirebaseAnalytics.Param.ACLID, clickId)
        bundle.putString(FirebaseAnalytics.Param.CONTENT, clickId)
        bundle.putString(FirebaseAnalytics.Param.CP1, clickId)
        bundle.putString(FirebaseAnalytics.Param.VALUE, clickId)
        mFirebaseAnalytics.logEvent("traffic_id", bundle)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, bundle)

        postGoNext(1, "sendAnal")
    }

    private fun getClickId(s: String): String {
        var id = s
        if (s.contains("&")) {
            id = s.split("&")[0]
        }
        return id
    }
}
