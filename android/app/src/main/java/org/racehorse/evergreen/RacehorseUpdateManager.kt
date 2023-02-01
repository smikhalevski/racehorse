package org.racehorse.evergreen

import androidx.lifecycle.LifecycleOwner
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import org.racehorse.webview.RacehorseWebView
import java.net.URL

class RacehorseUpdateManager(
    webView: RacehorseWebView,
    private val eventBus: EventBus,
    private val updateDescriptorUrl: String,
    downloadWorkerClass: Class<out DownloadWorker> = DownloadWorker::class.java
) : UpdateManager(webView.context, webView as LifecycleOwner, downloadWorkerClass) {
    override suspend fun getUpdateDescriptor(): UpdateDescriptor {
        val jsonObject = JSONObject(URL(updateDescriptorUrl).readText())

        return UpdateDescriptor(
            jsonObject.getString("version"),
            jsonObject.getString("url"),
            jsonObject.getBoolean("blocking")
        )
    }

    override fun onUpToDate() {}

    override fun onNonBlockingUpdateStarted() {}

    override fun onProgress(contentLength: Int, readLength: Int) {}

    override fun onBundleDownloaded() {}
}
