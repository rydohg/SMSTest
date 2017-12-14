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
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.widget.ImageView
import java.io.IOException
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader


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

        val uri = Uri.parse("content://mms-sms/conversations/" + conversation.convoID)
        val c = contentResolver.query(uri, projection, null, null, null)

        val messages = ArrayList<Message>()
        if (c.moveToFirst()) {
            do {
                val id = c.getString(c.getColumnIndex("_id"))
                var body = c.getString(c.getColumnIndex("body"))
                val address = c.getString(c.getColumnIndex("address"))
                val ct_t = c.getString(c.getColumnIndex("ct_t"))

                val smsTypeColumn = c.getString(c.getColumnIndex("type"))
                val mmsTypeColumn = c.getString(c.getColumnIndex("msg_box"))
                var bitmap: Bitmap? = null

                var received = true
                if (smsTypeColumn != null) {
                    if (smsTypeColumn.toInt() == 2) {
                        received = false
                    }
                }
                if (mmsTypeColumn != null) {
                    if (mmsTypeColumn.toInt() == 2) {
                        received = false
                        if ("text/plain" == ct_t) {
                            // Extremely inefficient
                            val mmsUri = Uri.parse("content://mms/part")
                            val selectionPart = "mid=" + id
                            val mmsCursor = contentResolver.query(mmsUri, null,
                                    selectionPart, null, null)
                            mmsCursor.moveToFirst()
                            for (i in mmsCursor.columnNames){
                                Log.d("MMSCol:", i + " = " + c.getString(c.getColumnIndex(i)))
                            }
                            val data = mmsCursor.getString(c.getColumnIndex("_data"))
                            mmsCursor.close()

                            body = if (data != null) {
                                getMmsText(id)
                            } else {
                                mmsCursor.getString(mmsCursor.getColumnIndex("text"))
                            }
                        } else if ("image/jpeg" == ct_t || "image/bmp" == ct_t ||
                                "image/gif" == ct_t || "image/jpg" == ct_t ||
                                "image/png" == ct_t) {
                            bitmap = getMmsImage(id)
                        }

                    }
                }
                // For now SMS is type 1 and MMS is type 2
//                Log.d("Messages", "SMS Type: $smsTypeColumn MMS Type: $mmsTypeColumn")
                var messageType = 1
                if (smsTypeColumn == null) {
                    messageType = 2
                }
                // 1L for msg_box is temporary
                messages.add(Message(body, address, messageType, mmsTypeColumn?.toLong(), bitmap, received))
            } while (c.moveToNext())
        }
        c.close()

        val recyclerView: RecyclerView = findViewById(R.id.message_recycler_view)
        recyclerView.setHasFixedSize(true)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MessageAdapter(messages)

        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getMmsImage(_id: String): Bitmap? {
        val partURI = Uri.parse("content://mms/part/" + _id)
        val testCursor = contentResolver.query(partURI, null,
                null, null, null)
        Log.d("MMSCol1", "_id= " + _id)
        testCursor.moveToFirst()
        for (i in testCursor.columnNames){
            Log.d("MMSCol1", i + " = " + testCursor.getString(testCursor.getColumnIndex(i)))
        }
        testCursor.close()
        var inputStream: InputStream? = null
        var bitmap: Bitmap? = null
        try {
            inputStream = contentResolver.openInputStream(partURI)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return bitmap
    }

    private fun getMmsText(id: String): String {
        val partURI = Uri.parse("content://mms/part/" + id)
        var inputStream: InputStream? = null
        val sb = StringBuilder()
        try {
            inputStream = contentResolver.openInputStream(partURI)
            if (inputStream != null) {
                val isr = InputStreamReader(inputStream, "UTF-8")
                val reader = BufferedReader(isr)
                var temp = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return sb.toString()
    }
}

//Body is null for MMS messages
data class Message(val body: String?, val address: String?, val type: Int, val msg_box: Long?, val image: Bitmap?, val received: Boolean)

class MessageAdapter constructor(private val messageList: ArrayList<Message>) : RecyclerView.Adapter<MessageAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CustomViewHolder {
        when (viewType) {
            1 -> return SentMessageViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_sent_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
            2 -> return ReceivedMessageViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_received_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
            else -> return ReceivedMessageViewHolder(
                    LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_received_mms_message,
                                    parent,
                                    false)
                            as ConstraintLayout
            )
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].received) {
            if (messageList[position].msg_box != null){
                3
            } else {
                2
            }
        } else {
            1
        }
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val message = messageList[position]

        holder.messageTextView.text = message.body
        if (holder is ReceivedMMSViewHolder){
            holder.imageView.setImageBitmap(messageList[position].image)
        }
    }

    override fun getItemCount(): Int = messageList.size

    abstract inner class CustomViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {
        abstract var messageTextView: TextView
    }

    inner class SentMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {
        override var messageTextView: TextView = rootView.findViewById(R.id.sent_message_text_view)
    }

    inner class ReceivedMessageViewHolder(rootView: View) : CustomViewHolder(rootView) {
        override var messageTextView: TextView = rootView.findViewById(R.id.received_message_text_view)
    }

    inner class ReceivedMMSViewHolder(rootView: View) : CustomViewHolder(rootView) {
        override var messageTextView: TextView = rootView.findViewById(R.id.received_message_text_view)
        var imageView: ImageView = rootView.findViewById(R.id.imageView)
    }
}
