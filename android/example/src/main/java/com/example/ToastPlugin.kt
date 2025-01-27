package com.example

import android.content.Context
import android.widget.Toast
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.racehorse.WebEvent

@Serializable
class ShowToastEvent(val message: String) : WebEvent

class ToastPlugin(private val context: Context) {

    @Subscribe
    fun onShowToast(event: ShowToastEvent) {
        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
    }
}
