package dev.albertus.expensms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {
    val transactions: LiveData<List<Transaction>> = repository.getAllTransactions()

    private val _senderFilter = MutableLiveData<String>("")
    val senderFilter: LiveData<String> = _senderFilter

    fun setSenderFilter(sender: String) {
        _senderFilter.value = sender
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }
}