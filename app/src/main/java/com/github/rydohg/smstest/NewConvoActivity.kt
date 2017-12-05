package com.github.rydohg.smstest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.telephony.SmsManager


class NewConvoActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_convo)
    }

    fun sendMessage(view: View){
       val numberEditText: EditText = findViewById(R.id.number_edit_text)
       val messageEditText: EditText = findViewById(R.id.message_edit_text)

        val phoneNumber = numberEditText.text.toString()
        val message = messageEditText.text.toString()

        val sms = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber, null, message, null, null)
    }
}