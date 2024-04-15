package org.racehorse

import android.Manifest
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.database.getStringOrNull
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.launchActivityForResult
import org.racehorse.utils.queryContent
import java.io.Serializable

class Contact(
    val name: String?,
    val photoUri: String?,
    val emails: List<String>,
    val phoneNumbers: List<String>
) : Serializable

class PickContactEvent : RequestEvent() {
    class ResultEvent(val contact: Contact?) : ResponseEvent()
}

open class ContactsPlugin(private val activity: ComponentActivity) {

    @Subscribe
    open fun onPickContact(event: PickContactEvent) {
        activity.askForPermission(Manifest.permission.READ_CONTACTS) { isGranted ->
            if (!isGranted) {
                event.respond(PickContactEvent.ResultEvent(null))
                return@askForPermission
            }

            val isLaunched =
                activity.launchActivityForResult(ActivityResultContracts.PickContact(), null) { contactUri ->
                    event.respond {
                        val contactId = contactUri?.lastPathSegment
                            ?: return@respond PickContactEvent.ResultEvent(null)

                        activity.queryContent(
                            contactUri,
                            arrayOf(
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts.PHOTO_URI
                            ),
                        ) {
                            moveToFirst()

                            PickContactEvent.ResultEvent(
                                Contact(
                                    name = getStringOrNull(getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)),
                                    photoUri = getStringOrNull(getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)),
                                    emails = queryEmails(contactId),
                                    phoneNumbers = queryPhoneNumbers(contactId)
                                )
                            )
                        }
                    }
                }

            if (!isLaunched) {
                event.respond(PickContactEvent.ResultEvent(null))
            }
        }
    }

    private fun queryEmails(contactId: String) = activity.queryContent(
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

    private fun queryPhoneNumbers(contactId: String) = activity.queryContent(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        ),
        ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=$contactId"
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
