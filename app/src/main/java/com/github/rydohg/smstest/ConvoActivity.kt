package com.github.rydohg.smstest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class ConvoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convo)

        val conversation: Conversation = intent.extras.getSerializable("convo") as Conversation
        Log.d("ConvoActivity", conversation.convoID.toString())
    }
}
