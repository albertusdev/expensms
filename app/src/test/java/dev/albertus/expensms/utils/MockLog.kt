package dev.albertus.expensms.utils

import android.util.Log
import org.mockito.Mockito

object MockLog {
    fun mock() {
        Mockito.mockStatic(Log::class.java)
        Mockito.`when`(Log.d(Mockito.anyString(), Mockito.anyString())).thenReturn(0)
        Mockito.`when`(Log.e(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(0)
    }
}

class TestLogger : Logger {
    override fun d(tag: String, message: String) {
        // Do nothing or print to console for debugging
        println("DEBUG: $tag: $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        // Do nothing or print to console for debugging
        println("ERROR: $tag: $message")
        throwable?.printStackTrace()
    }
}