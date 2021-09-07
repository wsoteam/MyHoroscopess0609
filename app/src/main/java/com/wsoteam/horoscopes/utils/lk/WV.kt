package com.wsoteam.horoscopes.utils.lk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.Exception

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
                return if (url == null || url.startsWith("http://") || url.startsWith("https://")) false else try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    view.context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.i("TAG", "shouldOverrideUrlLoading Exception:$e")
                    true
                }
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