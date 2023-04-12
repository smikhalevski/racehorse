package com.example.myapplication

import android.content.Context
import android.widget.Toast
import org.greenrobot.eventbus.Subscribe
import org.racehorse.WebEvent

class ShowToastEvent(val message: String) : WebEvent

class ToastController(private val context: Context) {

    @Subscribe
    fun onShowToast(event: ShowToastEvent) {
        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
    }
}
