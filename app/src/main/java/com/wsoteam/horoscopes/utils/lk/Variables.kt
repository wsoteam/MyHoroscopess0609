package com.wsoteam.horoscopes.utils.lk

class Variables (var sub_name_1: String = "",
                 var sub_name_2: String = "",
                 var sub_name_3: String = "",
                 var sub_name_4: String = "",
                 var DOCUMENT: String = "",
                 var ORGANIC: String = "Organic",
                 var NONORGANIC: String = "NonOrganic",
                 var FIREBASE: String = "",
                 var isRun: Boolean = false,
                 var appsFlyerId: String = "",
                 var lastUrl: String = "",
                 var outCode: Int = 0,
                 var runIterator: Int = 0,
                 var OPEN: Int = 0,
                 var pageIterator: Int = 0,
                 var firstUrl: String = ""
) {

    override fun toString(): String {
        return firstUrl
    }

    companion object {
        var CC: String = ""
    }
}