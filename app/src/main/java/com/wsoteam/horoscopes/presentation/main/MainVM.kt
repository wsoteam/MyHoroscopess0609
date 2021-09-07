package com.wsoteam.horoscopes.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wsoteam.horoscopes.App
import com.wsoteam.horoscopes.models.Sign
import com.wsoteam.horoscopes.utils.PreferencesProvider
import com.wsoteam.horoscopes.utils.net.state.NetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class MainVM(application: Application) : AndroidViewModel(application) {

    private val FIRST_FILE = "number1.json"
    private val SECOND_FILE = "number2.json"
    private val THIRD_FILE = "number3.json"

    private val PT_LOCALE = "pt"

    private var job = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + job)

    private val dataLD = MutableLiveData<List<Sign>>()

    private var status: MutableLiveData<Int>? = null

    private var isStartedOpen = false
    private var isStartedAps = false



    fun preLoadData() {
        //L.log("preLoadData")
        if (NetState.isConnected()) {
            job = vmScope.launch {
                CacheData.setCachedData(getData())
            }
        }
    }

    fun reloadData() {
        //L.log("reloadData")
        if (NetState.isConnected()) {
            job = vmScope.launch {
                dataLD.value = getData()
            }
        }
    }

    fun setupCachedData() {
       // L.log("setupCachedData")
        if (CacheData.getCachedData() != null) {
            dataLD.value = CacheData.getCachedData()
            CacheData.clearCache()
        } else {

        }
    }


    fun getData(): List<Sign> {
        var path = getTodayPath()
        var json: String
        var moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        var listType = Types.newParameterizedType(List::class.java, Sign::class.java)
        var jsonAdapter = moshi.adapter<List<Sign>>(listType)
        try {
            var inputStream = App.getInstance().assets.open(path)
            var size = inputStream.available()
            var buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, charset("UTF-8"))
            return jsonAdapter.fromJson(json)!!
        } catch (e: Exception) {
        }
        return null!!
    }

    private fun getTodayPath(): String {
        var counter = PreferencesProvider.pathCounter
        var lastEnterDay = PreferencesProvider.lastEnterDay
        var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        if (currentDay != lastEnterDay){
            PreferencesProvider.lastEnterDay = currentDay
            counter ++

            if (counter > PreferencesProvider.MAX_PATH_COUNTER){
                counter = 0
            }

            PreferencesProvider.pathCounter = counter
        }


        var listPaths = arrayListOf(FIRST_FILE, SECOND_FILE, THIRD_FILE)
        return listPaths[counter]
    }

    fun getLD(): MutableLiveData<List<Sign>> {
       // L.log("getLD")
        return dataLD
    }

}