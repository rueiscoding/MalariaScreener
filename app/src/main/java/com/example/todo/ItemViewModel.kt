package com.example.todo

import android.app.Application
import android.util.Log
import androidx.compose.ui.window.application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ItemViewModel(application: Application) : AndroidViewModel(application) {

    private val itemRepository: AccessItems
    private val _allItems = MutableLiveData<List<Item>>()
    val allItems: LiveData<List<Item>> get() = _allItems

    init {
        val itemDao = ItemDatabase.getDatabase(application).itemDao()
        itemRepository = AccessItems(itemDao)

        viewModelScope.launch {
            val items = itemRepository.getAllItems()
            Log.d("RUE: ItemViewModel", "Fetched items from DB: ${items.size}")
            _allItems.postValue(items)
        }
    }

    fun addItem(item: Item) {
        viewModelScope.launch{
            itemRepository.insertItem(item)
            _allItems.postValue(itemRepository.getAllItems())
        }
    }

    fun removeItem(item: Item) {
        viewModelScope.launch{
            itemRepository.deleteItem(item)
            _allItems.postValue(itemRepository.getAllItems())
        }
    }

    fun removeItems(items: List<Item>){
        viewModelScope.launch{
            items.forEach { item ->
                itemRepository.deleteItem(item)
                _allItems.postValue(itemRepository.getAllItems())
            }
        }
    }

    fun refreshItems() {
        Log.d("RUE: ItemViewModel", "refreshItems")
        viewModelScope.launch {
            val items = itemRepository.getAllItems()
            Log.d("RUE: ItemViewModel", "Fetch items from repository: $items")
            (allItems as MutableLiveData).postValue(items)
        }
    }

    fun getItemById(name: String): Item? {
        return allItems.value?.find { it.name == name }
    }

    suspend fun updateItem(item: Item) {
        viewModelScope.launch{
            itemRepository.updateItem(item)
            _allItems.postValue(itemRepository.getAllItems())
            Log.d("RUE: ItemViewModel", " Finished updatingItem")
        }

    }

}
