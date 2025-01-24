package com.example.todo

import androidx.lifecycle.LiveData

/**
 * Wrapper class so that the app can interact with the database.
 */
class AccessItems(private val itemDao: ItemDao) {

    suspend fun insertItem(item: Item) = itemDao.insertItem(item)

    suspend fun insertItems(items: List<Item>) = itemDao.insertItems(items)

    suspend fun getAllItems(): List<Item> = itemDao.getAllItems().toList()

    suspend fun getItemById(itemId: Long): Item? = itemDao.getItemById(itemId)

    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

    suspend fun deleteItems(items: List<Item>) = itemDao.deleteItems(items)

    suspend fun deleteAllItems() = itemDao.deleteAllItems()

    suspend fun updateItem(item : Item) = itemDao.updateItem(item)
}