package com.example.todo

import android.database.Cursor
import androidx.room.*
import java.util.Objects

/**
 * Database operations that can be performed on ItemTable.
 * functions are marked as "suspend" so that they do not occur on the main thread and stall UI.
 */


@Dao
interface ItemDao {

    @Insert
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Delete
    suspend fun deleteItem(item: Item)

    @Delete
    suspend fun deleteItems(items: List<Item>)

    @Query("DELETE FROM ItemTable")
    suspend fun deleteAllItems()

    //@Query("SELECT * FROM ItemTable ORDER BY date DESC")
    @Query("SELECT * FROM ItemTable")
    suspend fun getAllItems(): Array<Item>

    @Query("SELECT * FROM ItemTable WHERE id = :id")
    suspend fun getItemById(id: Long): Item

    @Update
    suspend fun updateItem(item: Item)
}