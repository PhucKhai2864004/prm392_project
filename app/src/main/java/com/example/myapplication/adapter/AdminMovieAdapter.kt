package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Movie

class AdminMovieAdapter(
    private var movies: List<Movie>,
    private val onEditClick: (Movie) -> Unit,
    private val onDeleteClick: (Movie) -> Unit
) : RecyclerView.Adapter<AdminMovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivMoviePoster)
        val tvTitle: TextView = view.findViewById(R.id.tvMovieTitle)
        val tvGenre: TextView = view.findViewById(R.id.tvMovieGenre)
        val tvDuration: TextView = view.findViewById(R.id.tvMovieDuration)
        val tvStatus: TextView = view.findViewById(R.id.tvMovieStatus)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.tvTitle.text = movie.title
        holder.tvGenre.text = movie.genre
        holder.tvDuration.text = movie.duration
        holder.tvStatus.text = if (movie.nowShowing) "Đang chiếu" else "Sắp chiếu"

        // In a real app, you would use Glide to load the movie poster
        // Glide.with(holder.itemView.context).load(movie.posterUrl).into(holder.ivPoster)

        holder.btnEdit.setOnClickListener {
            onEditClick(movie)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(movie)
        }
    }

    override fun getItemCount() = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}