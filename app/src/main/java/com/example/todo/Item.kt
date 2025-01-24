package com.example.todo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "ItemTable")
data class Item(
    var name: String,
    val imageUri: String?,
    val date: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // primary key
    var outputUri: String?,
    var infected: Int? = null // if not infected, null, otherwise corresponding int

)  : Serializable {


    override fun equals(other: Any?): Boolean {
        if (other !is Item) return false
        return name == other.name && imageUri == other.imageUri
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (imageUri?.hashCode() ?: 0)
        return result
    }

}