package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.model.Movie
import com.example.myapplication.utils.ImageCache

class MovieAdapter(
    private val movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit,
    private val onBookClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivMoviePoster)
        val tvTitle: TextView = view.findViewById(R.id.tvMovieTitle)
        val tvGenre: TextView = view.findViewById(R.id.tvMovieGenre)
        val tvDuration: TextView = view.findViewById(R.id.tvMovieDuration)
        val btnBook: Button = view.findViewById(R.id.btnBookNow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.tvTitle.text = movie.title
        holder.tvGenre.text = movie.genre
        holder.tvDuration.text = movie.duration

        // Sử dụng ImageCache để tải hình ảnh
        if (movie.posterUrl.isNotEmpty()) {
            ImageCache.loadImageWithoutCache(
                holder.itemView.context,
                movie.posterUrl,
                holder.ivPoster,
                R.drawable.ic_launcher_background,
                R.drawable.ic_launcher_foreground
            )
        } else {
            holder.ivPoster.setImageResource(R.drawable.ic_launcher_background)
        }

        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }

        holder.btnBook.setOnClickListener {
            onBookClick(movie)
        }
    }

    override fun getItemCount() = movies.size
}



