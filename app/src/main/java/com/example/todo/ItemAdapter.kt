package com.example.todo

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.databinding.ElementsBinding
import com.bumptech.glide.Glide

class ItemAdapter(
    private val items: MutableList<Item>,
    private var isSelectionMode: Boolean = false,
    private val itemClickListener: (Item) -> Unit,          // waiting for the item to be clicked
    private val removeItemsListener: (List<Item>) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>(){

    private val selectedItems = mutableSetOf<Item>()

    inner class ItemViewHolder(val binding: ElementsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {

        val binding = ElementsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    fun addItem(item: Item){
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun toggleSelectionMode(isSelectionMode: Boolean) {
        this.isSelectionMode = isSelectionMode
        if(!isSelectionMode){
            selectedItems.clear() //no selected items if not selcted mode
        }
        notifyDataSetChanged()  // refresh the RecyclerView
    }

    fun removeSelectedItems() {
        val itemsToRemove = selectedItems.toList()
        itemsToRemove.forEach { item ->
            val position = items.indexOf(item)
            if (position != -1) {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }
        selectedItems.clear()
        removeItemsListener(itemsToRemove)
    }


    fun returnSelectedItems(): MutableSet<Item> {
        return selectedItems
    }

    fun updateItem(updatedItem: Item) {
        val position = items.indexOfFirst { it.name == updatedItem.name }
        if (position != -1) {
            items[position] = updatedItem
            notifyItemChanged(position)
        }
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        var current = items[position]

        holder.binding.apply{
            tvName.text = current.name

            // using glide to load img into the Item in recyclerview
            if (current.imageUri != null) {
                Glide.with(ivImage.context)
                    .load(current.imageUri)
                    .into(ivImage)
            } else {
//                ivImage.setImageResource(R.drawable.default_image) // default img
            }


            root.setBackgroundColor( ///change bg to show that selected
                if(selectedItems.contains(current)){
                    Color.parseColor("#ADD8E6")
                }else{
                    Color.TRANSPARENT
                }
            )

            root.setOnClickListener{ //listen for item to be clicked
                Log.d("RUE: ItemAdapter", "Clicked select");
                if(isSelectionMode){ //if selection mode n clicked,
                    Log.d("RUE: ItemAdapter", "Clicked select, and selectmode");
                    if (selectedItems.contains(current)) {
                        selectedItems.remove(current)
                    } else {
                        selectedItems.add(current)
                    }
                    notifyItemChanged(position)

                }else{
                    Log.d("RUE: ItemAdapter", "info");
                    itemClickListener(current) //move to info page
                }

            }

        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(newItems: List<Item>){
        selectedItems.clear()
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun returnIsSelectionMode(): Boolean {
        return isSelectionMode
    }

}