package org.racehorse

import android.Manifest
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.database.getStringOrNull
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.launchActivityForResult
import org.racehorse.utils.queryAll
import java.io.Serializable

class Contact(
    val id: Long,
    val name: String?,
    val photoUri: String?,
    val emails: List<String>,
    val phoneNumbers: List<String>
) : Serializable

class PickContactEvent : RequestEvent() {
    class ResultEvent(val contact: Contact?) : ResponseEvent()
}

class GetContactEvent(val contactId: Long) : RequestEvent() {
    class ResultEvent(val contact: Contact?) : ResponseEvent()
}

open class ContactsPlugin(private val activity: ComponentActivity) {

    @Subscribe
    open fun onPickContact(event: PickContactEvent) {
        activity.askForPermission(Manifest.permission.READ_CONTACTS) { isGranted ->
            isGranted || return@askForPermission event.respond(PickContactEvent.ResultEvent(null))

            val isLaunched = activity.launchActivityForResult(ActivityResultContracts.PickContact(), null) {
                event.respond { PickContactEvent.ResultEvent(it?.lastPathSegment?.toLong()?.let(::getContact)) }
            }

            if (!isLaunched) {
                event.respond(PickContactEvent.ResultEvent(null))
            }
        }
    }

    @Subscribe
    open fun onGetContact(event: GetContactEvent) {
        activity.askForPermission(Manifest.permission.READ_CONTACTS) { isGranted ->
            event.respond { GetContactEvent.ResultEvent(if (isGranted) getContact(event.contactId) else null) }
        }
    }

    private fun getContact(contactId: Long) = activity.contentResolver.queryAll(
        ContactsContract.Contacts.CONTENT_URI.buildUpon().appendPath(contactId.toString()).build(),
        arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI
        ),
    ) {
        moveToFirst()

        Contact(
            id = contactId,
            name = getStringOrNull(getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)),
            photoUri = getStringOrNull(getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)),
            emails = getContactEmails(contactId),
            phoneNumbers = getContactPhoneNumbers(contactId)
        )
    }

    private fun getContactEmails(contactId: Long) = activity.contentResolver.queryAll(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=$contactId"
    ) {
        buildList {
            while (moveToNext()) {
                add(getString(getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)))
            }
        }
    }

    private fun getContactPhoneNumbers(contactId: Long) = activity.contentResolver.queryAll(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        ),
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=$contactId"
    ) {
        buildList {
            while (moveToNext()) {
                add(
                    getStringOrNull(getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                        ?: getString(getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                )
            }
        }
    }
}
