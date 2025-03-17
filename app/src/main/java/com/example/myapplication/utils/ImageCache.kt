package com.example.myapplication.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

object ImageCache {
    // Singleton để quản lý cache hình ảnh

    // Tải hình ảnh với Glide và không sử dụng cache
    fun loadImageWithoutCache(context: Context, url: String, imageView: android.widget.ImageView, placeholderResId: Int, errorResId: Int) {
        Glide.with(context)
            .load(url)
            .apply(RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(placeholderResId)
                .error(errorResId))
            .into(imageView)
    }

    // Tải hình ảnh với Glide và sử dụng cache
    fun loadImage(context: Context, url: String, imageView: android.widget.ImageView, placeholderResId: Int, errorResId: Int) {
        Glide.with(context)
            .load(url)
            .placeholder(placeholderResId)
            .error(errorResId)
            .into(imageView)
    }

    // Xóa cache của Glide
    fun clearCache(context: Context) {
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
        Glide.get(context).clearMemory()
    }
}