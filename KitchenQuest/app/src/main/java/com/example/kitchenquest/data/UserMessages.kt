package com.example.kitchenquest.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.kitchenquest.data.preferences.AppLanguagePreferences

interface UserMessages {
    fun show(@StringRes message: Int)
}

object NoOpUserMessages : UserMessages {
    override fun show(message: Int) = Unit
}

class ToastUserMessages(context: Context) : UserMessages {

    private val appContext = context.applicationContext
    private val mainThread = Handler(Looper.getMainLooper())

    override fun show(message: Int) {
        mainThread.post {
            val text = AppLanguagePreferences.localizedContext(appContext).getString(message)
            Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()
        }
    }
}