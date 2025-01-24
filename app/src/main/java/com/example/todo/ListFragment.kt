package com.example.todo

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo.databinding.FragmentListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ListFragment : Fragment() {

    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemViewModel: ItemViewModel
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!


    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val itemTitle = getFileNameFromUri(uri) ?: "sample.jpg"
            val item = Item(itemTitle, uri.toString(), outputUri = null)
//            itemViewModel.addItem(item)
            evaluateImage(item)

            hideProgressDialog()
            Log.d("RUE", "getContent: " + item.outputUri)
        }

    }


    private fun evaluateImage(item: Item) {
        //launch the coroutine so entire process is in background thread
        CoroutineScope(Dispatchers.Main).launch {

            val inputUri = Uri.parse(item.imageUri)
            val inputStream = requireContext().contentResolver.openInputStream(inputUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            val imageProcessor = ImageProcessor(requireContext(), "best_float32.tflite", "labels.txt")
            imageProcessor.setup()

            //run img processing tasks in background as well
            val preprocessedBitmap = withContext(Dispatchers.IO) {
                imageProcessor.cropMicroscopeRegion(originalBitmap)
            }

            val parasiteBoxes = withContext(Dispatchers.IO) {
                imageProcessor.detect(preprocessedBitmap)
            }

            if (parasiteBoxes == null) {
                Log.d("RUE: ListFragment", "ParasiteBoxes are null")
            }

            if (parasiteBoxes != null) {
                //process output img in background
                val outputBitmap = withContext(Dispatchers.IO) {
                    imageProcessor.overlayBoundingBoxes(preprocessedBitmap, parasiteBoxes)
                }

                val outputUri = withContext(Dispatchers.IO) {
                    imageProcessor.saveImage(outputBitmap)
                }

                //if the output is valid, launch a coroutine to update and refresh items
                if (outputUri != null) {
                    item.outputUri = outputUri.toString() //set item's outputURI

                    itemViewModel.viewModelScope.launch {

                        val updateResult = async { itemViewModel.addItem(item) }

                        //wait for update to finish
                        updateResult.await()

                        itemViewModel.refreshItems()

                        Log.d("RUE: ListFragment", "Updated Item outputURI: ${item.outputUri}")
                    }
                }
            }

            imageProcessor.clear()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemViewModel = ViewModelProvider(requireActivity()).get(ItemViewModel::class.java)

        itemAdapter = ItemAdapter(mutableListOf(),
            itemClickListener = { item ->
                navigateToInfoFragment(item)
            },
            removeItemsListener = { itemsToRemove ->
                itemViewModel.removeItems(itemsToRemove)
            }
        )

        binding.rvAddImages.adapter = itemAdapter
        binding.rvAddImages.layoutManager = LinearLayoutManager(requireContext())
        itemAdapter.toggleSelectionMode(false)
        binding.btnDelete.visibility = View.GONE
        binding.btnExport.visibility = View.GONE


        itemViewModel.allItems.observe(viewLifecycleOwner) { items ->
            Log.d("ListFragment", "Observed items: ${items.size}")
            itemAdapter.submitList(items) // update adapter with latest items
            toggleSelectButtonVisibility(items.isNotEmpty())
        }

        setupButtonListeners()

    }

    /**
     * Helper function to get the file name from a URI.
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (columnIndex != -1) {
                    return it.getString(columnIndex) //return file name
                }
            }
        }
        return null
    }

    private fun setupButtonListeners(){
        ///toggle between select n cancel
        binding.btnSelect.setOnClickListener {
            if (itemAdapter.returnIsSelectionMode()) {
                itemAdapter.toggleSelectionMode(false) //exit selection mode
                binding.btnSelect.text = "Select" // change text to "Select"
                binding.btnDelete.visibility = View.GONE
                binding.btnExport.visibility = View.GONE
            } else {
                itemAdapter.toggleSelectionMode(true) //enter selection mode
                binding.btnSelect.text = "Cancel" // change text to "Cancel"
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnExport.visibility = View.VISIBLE
            }
        }

        binding.btnDelete.setOnClickListener {
            val selectedItems = itemAdapter.returnSelectedItems()
            itemViewModel.removeItems(selectedItems.toList()) // removes from backend
            itemAdapter.removeSelectedItems()
            binding.btnDelete.visibility = View.GONE
            binding.btnExport.visibility = View.GONE
            itemAdapter.toggleSelectionMode(false)
            binding.btnSelect.text="Select"
        }

        binding.btnExport.setOnClickListener {
            //EXPORT LOGIC HERE
            binding.btnDelete.visibility = View.GONE
            binding.btnExport.visibility = View.GONE
            itemAdapter.toggleSelectionMode(false)
            binding.btnSelect.text="Select"
        }

        binding.btnAdd.setOnClickListener {
            launchPhotoLibrary()
        }
    }

    private fun toggleSelectButtonVisibility(isVisible: Boolean) {
        binding.btnSelect.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun navigateToInfoFragment(item: Item) {
        itemViewModel.refreshItems()
        val fragment = InfoFragment()
        val bundle = Bundle()
        bundle.putSerializable("item_data", item)
        Log.d("ListFragment", "Item outputUri before passing: ${item.outputUri}")
        fragment.arguments = bundle

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun hideProgressDialog() {
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(requireContext(), "Model processing completed", Toast.LENGTH_SHORT).show()
        }, 3000)  //some delay
    }

    fun launchPhotoLibrary(){
        getContent.launch("image/*")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
