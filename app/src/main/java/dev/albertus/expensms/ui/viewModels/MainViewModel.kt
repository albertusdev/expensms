package dev.albertus.expensms.ui.viewModels

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.utils.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _senderFilter = MutableLiveData("")
    val senderFilter: LiveData<String> = _senderFilter

    fun setSenderFilter(sender: String) {
        _senderFilter.value = sender
        loadSmsMessages()
    }

    fun loadSmsMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = readSmsMessages()
            val parsedTransactions = messages.mapNotNull { (sender, body, timestamp) ->
                if (sender.contains(_senderFilter.value ?: "", ignoreCase = true)) {
                    SmsParser.parseTransaction(body, timestamp)
                } else {
                    null
                }
            }
            _transactions.postValue(parsedTransactions)
        }
    }

    private fun readSmsMessages(): List<Triple<String, String, Long>> {
        val messages = mutableListOf<Triple<String, String, Long>>()
        val contentResolver: ContentResolver = getApplication<Application>().contentResolver
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)

        cursor?.use {
            val senderIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(senderIndex)
                val body = it.getString(bodyIndex)
                val timestamp = it.getLong(dateIndex)
                messages.add(Triple(sender, body, timestamp))
            }
        }

        return messages
    }
}