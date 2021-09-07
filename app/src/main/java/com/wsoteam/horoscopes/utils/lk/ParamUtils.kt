package com.wsoteam.horoscopes.utils.lk

class ParamUtils {
    private var mPrefs = PreferenceProvider()

    fun replaceParamHome(a: String, adid: String?, afid: String?): String {
        var a = a
        if (a.contains("gadid")) {
            a = a.replace("gadid", adid!!)
        }
        if (a.contains("afid")) {
            a = a.replace("afid", afid!!)
        }
        return a
    }

    fun replaceParamOut(b: String, sub_id1: String?,
                        sub_id2: String?,sub_id3: String?,sub_id4: String?): String {
        var b = b
        var subId1 = sub_id1
        var subId2 = sub_id2
        var subId3 = sub_id3
        var subId4 = sub_id4
        if (b.contains("wp1")) {
            subId1 = mPrefs.getDataString(LideraSharedKeys.P1.key)
            b = b.replace("wp1", subId1)
        }
        if (b.contains("wp2")) {
            subId2 = mPrefs.getDataString(LideraSharedKeys.P2.key)
            b = b.replace("wp2", subId2)
        }
        if (b.contains("wp3")) {
            subId3 = mPrefs.getDataString(LideraSharedKeys.P3.key)
            b = b.replace("wp3", subId3)
        }
        if (b.contains("wp4")) {
            subId4 = mPrefs.getDataString(LideraSharedKeys.P4.key)
            b = b.replace("wp4", subId4)
        }
        return b
    }
}