package com.wsoteam.horoscopes.utils.lk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.wsoteam.horoscopes.App

class WV {
    private val variables = Variables()
    private val mPrefs = PreferenceProvider()

    @SuppressLint("SetJavaScriptEnabled")
    fun setParams(wov: WebView) {
        val webSettings = wov.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        wov.settings.pluginState = WebSettings.PluginState.ON
        wov.settings.allowFileAccess = true
        wov.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url!!.startsWith("sms:")) {
                    var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url!!))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.getInstance().startActivity(intent)

                } else {
                    view!!.loadUrl(url!!)
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (variables.OPEN == 0) {
                    variables.pageIterator++
                    if (variables.pageIterator == 1) {
                        variables.OPEN = 1
                        variables.firstUrl = wov.url!!

                        mPrefs.saveDataString(LideraSharedKeys.FirstOpenUrl.key, variables.firstUrl)
                        mPrefs.saveDataInt(LideraSharedKeys.FirstOpenView.key, variables.OPEN)
                    }
                }
            }
        }
    }
}