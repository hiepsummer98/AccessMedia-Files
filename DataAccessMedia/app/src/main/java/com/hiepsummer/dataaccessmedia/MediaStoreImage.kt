package com.hiepsummer.dataaccessmedia

import android.annotation.SuppressLint
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import java.util.*

class MediaStoreImage(
    val id: Long,
    val displayName: String,
    val dateTaken: Date,
    val contentUri: Uri
) {
    companion object {
        val DiffCallBack = object : DiffUtil.ItemCallback<MediaStoreImage>() {
            override fun areItemsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem.id == newItem.id

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem == newItem

        }
    }
}