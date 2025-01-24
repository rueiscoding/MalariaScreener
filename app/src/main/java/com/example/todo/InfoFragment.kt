package com.example.todo

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.todo.databinding.FragmentInfoBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class InfoFragment : Fragment() {

    private lateinit var item: Item
    private lateinit var binding: FragmentInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.GONE

        binding = FragmentInfoBinding.inflate(inflater, container, false)

        arguments?.let {
            item = it.getSerializable("item_data") as Item
            Log.d("INFOFRAG", "recieved item " + item.outputUri)
            updateDiagnosticSummaryAndImage()
        }

        binding.slideNameEditable.setText(item.name);


//        // setup the back button click
//        binding.backArrow.setOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }

//        //confidence slider
//        val confidenceSlider = binding.confidenceSlider
//        val confidenceLabel = binding.confidenceLabel
//
//        //set initial confidence value
//        confidenceLabel.text = "Confidence: ${confidenceSlider.value.toInt()}%"
//
//        //when slider valu changes
//        confidenceSlider.addOnChangeListener { slider, value, fromUser ->
//            confidenceLabel.text = "Confidence: ${value.toInt()}%" // Update the label with the slider value
//        }

        // long-click listener to enlarge the image
        binding.photoOutput.setOnLongClickListener {
            showImageFullScreen()
            true
        }

        return binding.root
        //return inflater.inflate(R.layout.fragment_info, container, false)
    }

    fun getFileFromUri(uri: Uri): File? {
        val resolver = requireContext().contentResolver
        val fileDescriptor: ParcelFileDescriptor? = resolver.openFileDescriptor(uri, "r")

        fileDescriptor?.let {
            val inputStream = FileInputStream(it.fileDescriptor)
            val file = File(requireContext().cacheDir, "temp_image.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            return file
        }

        return null
    }

    private fun updateDiagnosticSummaryAndImage() {

        binding.diagnosticSummaryText.text = "Default diagnostic summary"
        if(item.outputUri == null)
        {
            Log.e("RUE: InfoFragment", "OUTPUTURI IS NULL")
        }
        Log.e("RUE: InfoFragment", "uri: " + item.outputUri)

        val uri = Uri.parse(item.outputUri)

        val file = getFileFromUri(uri)

        if (file != null) {
            // loads img skipping cache memory
            // forces glide to always load a fresh img
            Glide.with(requireContext())
                .load(uri)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.photoOutput)

        } else {
            Log.e("RUE: InfoFragment", "failed to retrieve image from URI")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // shows bottom navigation when this fragment is destroyed (back to previous fragment)
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
    }

    private fun showImageFullScreen() {
        // sreate a Dialog for fullscreen image display
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val fullScreenImageView = dialog.findViewById<android.widget.ImageView>(R.id.fullScreenImageView)

        Glide.with(requireContext()).load(item.outputUri).into(fullScreenImageView)

        fullScreenImageView.setOnClickListener {
            dialog.dismiss() //closes dialog when image is clicked
        }

        dialog.show()
    }

}
