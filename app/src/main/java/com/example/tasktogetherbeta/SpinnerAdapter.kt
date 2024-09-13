package com.example.tasktogetherbeta

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomSpinnerAdapter(context: Context, resource: Int,
                           private val items: List<String>) : ArrayAdapter<String>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = createCustomView(position, convertView, parent)

        // Load animation from the anim folder
        val animation = AnimationUtils.loadAnimation(context, R.anim.dropdown_animation)

        // Apply the animation to each dropdown item
        view.startAnimation(animation)

        return view
    }

    private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.spinner_item, parent, false)

        val textView = view.findViewById<TextView>(R.id.spinnerItemText)
        textView.text = items[position]

        return view
    }
}
