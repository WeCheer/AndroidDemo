package com.wyc.androiddemo.utils

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object Log {

    private const val TAG = "AndroidQ-"

    private val LINE_SEPARATOR = System.getProperty("line.separator")

    @JvmStatic
    fun d(localTag: String, logInfo: String) {
        Log.d(TAG + localTag, logInfo)
    }

    @JvmStatic
    fun v(localTag: String, logInfo: String) {
        Log.v(TAG + localTag, logInfo)
    }

    @JvmStatic
    fun e(localTag: String, logInfo: String) {
        Log.e(TAG + localTag, logInfo)
    }

    @JvmStatic
    fun e(localTag: String, logInfo: String, e: Throwable) {
        Log.e(TAG + localTag, logInfo, e)
    }

    @JvmStatic
    fun i(localTag: String, logInfo: String) {
        Log.i(TAG + localTag, logInfo)
    }

    @JvmStatic
    fun w(localTag: String, logInfo: String) {
        Log.i(TAG + localTag, logInfo)
    }

    @JvmStatic
    fun w(localTag: String, logInfo: String, ex: Throwable) {
        Log.w(TAG + localTag, logInfo, ex)
    }

    @JvmStatic
    fun printStackTrace(throwable: Throwable) {
        Log.v(TAG, Log.getStackTraceString(throwable))
    }

    private fun printLine(tag: String, isTop: Boolean) {
        if (isTop) {
            Log.d(tag, "╔═══════════════════════════════════════════════════════════════════════════════════════")
        } else {
            Log.d(tag, "╚═══════════════════════════════════════════════════════════════════════════════════════")
        }
    }

    @JvmStatic
    fun printJson(tag: String, msg: String, headString: String) {
        var message: String
        message = try {
            when {
                msg.startsWith("{") -> {
                    val jsonObject = JSONObject(msg)
                    jsonObject.toString(4) //最重要的方法，就一行，返回格式化的json字符串，其中的数字4是缩进字符数
                }
                msg.startsWith("[") -> {
                    val jsonArray = JSONArray(msg)
                    jsonArray.toString(4)
                }
                else -> {
                    msg
                }
            }
        } catch (e: JSONException) {
            msg
        }
        printLine(tag, true)
        message = headString + LINE_SEPARATOR + message
        val lines = message.split(LINE_SEPARATOR).toTypedArray()
        for (line in lines) {
            d(tag, "║ $line")
        }
        printLine(tag, false)
    }
}