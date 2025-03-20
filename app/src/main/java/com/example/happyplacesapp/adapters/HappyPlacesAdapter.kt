package com.example.happyplacesapp.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplacesapp.activities.AddHappyPlaceActivity
import com.example.happyplacesapp.activities.MainActivity
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ItemHappyPlaceBinding
import com.example.happyplacesapp.models.HappyPlaceModel

open class HappyPlacesAdapter(
    private val context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            ItemHappyPlaceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            holder.ivPlaceImage.setImageURI(Uri.parse(model.image))
            holder.tvTitle.text = model.title
            holder.tvDescription.text = model.description
            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position = position, model = model)
                }
            }
        }
    }

    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

    fun removeAt(position: Int) {
        val dbHandler = DatabaseHandler(context = context)
        val isDeleted = dbHandler.deleteHappyPlace(list[position])

        if (isDeleted > 0) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener {
        fun onClick(position: Int, model: HappyPlaceModel)
    }

    private class MyViewHolder(binding: ItemHappyPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tvTitle = binding.tvTitle
        val tvDescription = binding.tvDescription
        val ivPlaceImage = binding.ivPlaceImage
    }
}
