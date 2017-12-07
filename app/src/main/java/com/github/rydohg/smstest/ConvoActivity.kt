package com.github.rydohg.smstest

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class ConvoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convo)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
        supportActionBar?.setIcon(R.drawable.ic_person_white_24dp)

        val conversation: Conversation = intent.extras.getSerializable("convo") as Conversation
        supportActionBar?.title = nameOrPhoneNumber(conversation.displayName, conversation.number)
        Log.d("ConvoActivity", conversation.convoID.toString())

        val projection = arrayOf("_id", "address", "body", "ct_t", "type", "msg_box")
        //Get only SMS and MMS messages that have been sent
        val selection = "(type = 2 OR msg_box = 2)"
        val uri = Uri.parse("content://mms-sms/conversations/" + conversation.convoID)
        val c = contentResolver.query(uri, projection, selection, null, null)

        val messages = ArrayList<Message>()
        if (c.moveToFirst()){
            do {
                val body = c.getString(c.getColumnIndex("body"))
                val address = c.getString(c.getColumnIndex("address"))
                var received = false
                if (address != null){
                    received = true
                }

                val smsTypeColumn = c.getString(c.getColumnIndex("type"))
                // For now SMS is type 1 and MMS is type 2
                var type = 1
                if (smsTypeColumn == null){
                    type = 2
                }
                // 1L for msg_box is temporary
                messages.add(Message(body, address, type, 1L, received))
            } while (c.moveToNext())
        }
        c.close()

        val recyclerView: RecyclerView = findViewById(R.id.message_recycler_view)
        recyclerView.setHasFixedSize(true)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MessageAdapter(messages)

        recyclerView.scrollToPosition(messages.size - 1)
    }
}

//Address is null if I send the message
//Body is null for MMS messages
data class Message(val body: String?, val address: String?, val type: Int, val msg_box: Long, val received: Boolean)

class MessageAdapter constructor(private val messageList: ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CustomViewHolder {
        return if (viewType == 1) {
            CustomViewHolder(
                    LayoutInflater.from(parent?.context)
                        .inflate(R.layout.list_item_sent_message,
                                parent,
                                false)
                            as ConstraintLayout
            )
        } else {
            CustomViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_received_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if(messageList[position].received) {
            1
        } else {
            2
        }
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val message = messageList[position]

        holder.messageTextView.text = message.body
    }

    override fun getItemCount(): Int = messageList.size

    open inner class CustomViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {
        var messageTextView: TextView = rootView.findViewById(R.id.message_text_view)
    }

    inner class SentMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {

    }

    inner class ReceivedMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {

    }
}
