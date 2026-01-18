package com.example.quickmdcapture

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class TemplateAdapter(
    context: Context,
    private val items: List<String>,
    private val textColor: Int
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as TextView).apply {
            setTextColor(textColor)
            gravity = android.view.Gravity.CENTER
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as TextView).apply {
            setTextColor(textColor)
            gravity = android.view.Gravity.CENTER
            setPadding(paddingLeft, 24, paddingRight, 24)
        }
        return view
    }
}