package com.hiepsummer.dataaccessmedia

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hiepsummer.dataaccessmedia.databinding.ActivityMainBinding
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.content.pm.PackageManager.PERMISSION_GRANTED

private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045
private const val DELETE_PERMISSION_REQUEST = 0x1033

class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val galleryAdapter = GalleryAdapter { image ->
            deleteImage(image)

        }
        binding.gallery.also {
            it.layoutManager = GridLayoutManager(this, 3)
            it.adapter = galleryAdapter
        }
        viewModel.images.observe(this, Observer<List<MediaStoreImage>> { images ->
            galleryAdapter.submitList(images)
        })
        viewModel.permisionNeedForDelete.observe(this, Observer { intentSender ->
            intentSender.let {
                startIntentSenderForResult(
                    intentSender,
                    DELETE_PERMISSION_REQUEST,
                    null, 0, 0, 0, null
                )
            }
        })
        binding.openAlbum.setOnClickListener({ openMediaStore() })
        binding.grantPermissionButton.setOnClickListener({ openMediaStore() })
        if (!haveStoragePermission()) {
            binding.welcomeView.visibility = View.VISIBLE
        } else {
            showImage()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    showImage()
                } else {
                    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    if (showRationale) {
                        showNoAccess()
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            viewModel.deletePendingImage()
        }
    }

    private fun showImage() {
        viewModel.loadImage()
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.GONE
    }

    private fun showNoAccess() {
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.VISIBLE
    }

    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showImage()
        } else {
            requestPermision()
        }
    }

    private fun goToSettings() {
        Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("packge:$packageName")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermision() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    private fun deleteImage(image: MediaStoreImage) {
        MaterialAlertDialogBuilder(this).setTitle("Delete?")
            .setMessage("Delete ${image.displayName}?")
            .setPositiveButton("Delete") { _: DialogInterface, _: Int ->
                viewModel.deleteImage(image)
            }.setNegativeButton("Cancel") { dialog: DialogInterface?, which: Int ->
                dialog?.dismiss()
            }.show()
    }

    private inner class GalleryAdapter(val onclick: (MediaStoreImage) -> Unit) :
        ListAdapter<MediaStoreImage, ImageViewHolder>(MediaStoreImage.DiffCallBack) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.gallery_layout, parent, false)
            return ImageViewHolder(view, onclick)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val mediaStoreImage = getItem(position)
            holder.rootView.tag = mediaStoreImage

            Glide.with(holder.imageView).load(mediaStoreImage.contentUri).thumbnail(0.33f)
                .centerCrop()
                .into(holder.imageView)
        }

    }

    private class ImageViewHolder(view: View, onclick: (MediaStoreImage) -> Unit) :
        RecyclerView.ViewHolder(view) {
        val rootView = view
        val imageView: ImageView = view.findViewById(R.id.image)

        init {
            imageView.setOnClickListener {
                val image = rootView.tag as? MediaStoreImage ?: return@setOnClickListener
                onclick(image)
            }
        }
    }
}
