package com.mediatek.calendar.nfc;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.android.calendar.EventInfoFragment;
import com.mediatek.calendar.MTKUtils;

/**
 * This class implements sharing the currently displayed event to another
 * device using NFC. NFC sharing is only enabled when the activity is in the
 * foreground and resumed. When an NFC link is established,
 * {@link #createMessage} will be called to create the data to be sent over the
 * link, which is a vCalendar in this case.
 */
public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "CalendarNfcHandler";
    private final EventInfoFragment mEventInfoFragment;

    public static void register(Activity activity, EventInfoFragment eventInfoFragment) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (adapter == null) {
            Log.w(TAG, "register nfc, NFC not available on this device!");
            return; // NFC not available on this device
        }
        adapter.setNdefPushMessageCallback(new NfcHandler(eventInfoFragment), activity);
    }

    public NfcHandler(EventInfoFragment eventInfoFragment) {
        mEventInfoFragment = eventInfoFragment;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        Log.i(TAG, "createNdefMessage..............");

        // Get the current event URI
        Uri eventUri = mEventInfoFragment.getUri();
        ContentResolver resolver = mEventInfoFragment.getActivity().getContentResolver();
        if (eventUri != null) {
            final String eventId = eventUri.getLastPathSegment();
            Log.i(TAG, "createNdefMessage, eventId=" + eventId);
            final Uri shareUri = Uri.parse(MTKUtils.VCALENDAR_URI + eventId);

            ByteArrayOutputStream ndefBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int r;
            try {
                InputStream vcalendarInputStream = resolver.openInputStream(shareUri);
                while ((r = vcalendarInputStream.read(buffer)) > 0) {
                    ndefBytes.write(buffer, 0, r);
                }
                Log.i(TAG, "createNdefMessage, ndefBytes=" + ndefBytes);
                NdefRecord record = NdefRecord.createMime(MTKUtils.VCALENDAR_TYPE, ndefBytes
                        .toByteArray());
                Log.i(TAG, "createNdefMessage, record=" + record);
                return new NdefMessage(record);
            } catch (IOException e) {
                Log.e(TAG, "IOException creating vcalendar.");
                return null;
            }
        } else {
            Log.w(TAG, "No event URI to share.");
            return null;
        }
    }
}
