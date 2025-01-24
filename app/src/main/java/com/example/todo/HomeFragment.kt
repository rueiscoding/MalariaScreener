package com.example.todo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.todo.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val button_cam = view.findViewById<Button>(R.id.btn_capture) //gets button
        button_cam.setOnClickListener {
            (activity as MainActivity).navigateToCamera() //cast to mainactiviy
        }

        val button_upload = view.findViewById<Button>(R.id.btn_upload) //get upload
        button_upload.setOnClickListener{
            (activity as MainActivity).navigateToList()
        }

        return view
    }
}