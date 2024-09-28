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
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _senderFilter = MutableLiveData<String>()
    val senderFilter: LiveData<String> = _senderFilter

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = repository.getAllTransactions()
        }
    }

    fun setSenderFilter(sender: String) {
        _senderFilter.value = sender
        loadTransactions()
    }
}