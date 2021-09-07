package com.wsoteam.horoscopes.utils.lk

import com.wsoteam.horoscopes.App
import java.util.ArrayList

class FlyerParams {
    private val variables = Variables()
    private val mPrefs = PreferenceProvider()

    fun paramReceiver() {
        if (App.campaign != "null") {
            if (App.campaign.contains("_")) {
                val charArray: Array<String> = App.campaign.split("_").toTypedArray()
                val listOfString: MutableList<String> = ArrayList(listOf(*charArray))
                if (listOfString.size < 5) {
                    for (i in listOfString.size - 1..5) {
                        listOfString.add("proeb")
                    }
                }
                variables.sub_name_1 = listOfString[0]
                variables.sub_name_2 = listOfString[1]
                variables.sub_name_3 = listOfString[2]
                variables.sub_name_4 = listOfString[3]

                mPrefs.saveDataString(LideraSharedKeys.P1.key, variables.sub_name_1)
                mPrefs.saveDataString(LideraSharedKeys.P2.key, variables.sub_name_2)
                mPrefs.saveDataString(LideraSharedKeys.P3.key, variables.sub_name_3)
                mPrefs.saveDataString(LideraSharedKeys.P4.key, variables.sub_name_4)
            }
        }
    }

    fun getDocName(): String {
        if (App.campaign != "null" && App.campaign != "None") {
            variables.DOCUMENT = variables.NONORGANIC
        } else {
            Variables.CC += "org"
            variables.DOCUMENT = variables.ORGANIC
        }
        return variables.DOCUMENT
    }
}