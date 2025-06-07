package com.example.nubo.data.network

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PrettyJsonLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        // JSON 포맷 출력
        if (message.startsWith("{")) {
            try {
                val jsonObject = JSONObject(message)
                val prettyJson = jsonObject.toString(4) // 들여쓰기 4칸
                Log.d("PrettyJsonLogger", prettyJson)
                return
            } catch (e: JSONException) {
                // 기본 출력
            }
        }
        if (message.startsWith("[")) {
            try {
                val jsonArray = JSONArray(message)
                val prettyJson = jsonArray.toString(4) // 들여쓰기 4칸
                Log.d("PrettyJsonLogger", prettyJson)
                return
            } catch (e: JSONException) {
                // 기본 출력
            }
        }
        // JSON이 아니면 그대로 출력
        Log.d("PrettyJsonLogger", message)
    }
}

