@file:UseSerializers(AnySerializer::class)

package org.racehorse.utils

import android.content.Intent
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * The [Intent] representation passed from and to the web application.
 *
 * @param action The general action to be performed, such as [Intent.ACTION_VIEW].
 * @param type The explicit MIME type included in the intent.
 * @param data The URI-encoded data that intent is operating on.
 * @param flags Special flags associated with this intent, such as [Intent.FLAG_ACTIVITY_NEW_TASK].
 * @param extras A map of extended data from the intent.
 */
@Serializable
class SerializableIntent(
    var action: String? = null,
    val type: String? = null,
    var data: String? = null,
    var flags: Int = 0,
    val selector: SerializableIntent? = null,
    var extras: Map<String, Any?>? = null,
    var categories: Set<String>? = null,
) {

    constructor(intent: Intent) : this(
        action = intent.action,
        type = intent.type,
        data = intent.dataString,
        flags = intent.flags,
        selector = intent.selector?.let(::SerializableIntent),
        extras = @Suppress("DEPRECATION") intent.extras?.keySet()?.associateWith(intent::getSerializableExtra),
        categories = intent.categories,
    )

    fun toIntent(): Intent = Intent(action).also { intent ->
        intent.setDataAndType(data?.toUri(), type)
        intent.addFlags(flags)
        intent.selector = selector?.toIntent()
        extras?.forEach {
            intent.putExtra(it.key, it.value as java.io.Serializable?)
        }
        categories?.forEach(intent::addCategory)
    }
}
