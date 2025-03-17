package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Showtime
import java.text.NumberFormat
import java.util.Locale

class ShowtimeAdapter(
    private val showtimes: List<Showtime>,
    private val onShowtimeClick: (Showtime) -> Unit
) : RecyclerView.Adapter<ShowtimeAdapter.ShowtimeViewHolder>() {

    class ShowtimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvAvailableSeats: TextView = view.findViewById(R.id.tvAvailableSeats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowtimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_showtime, parent, false)
        return ShowtimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowtimeViewHolder, position: Int) {
        val showtime = showtimes[position]

        holder.tvTime.text = showtime.time

        // Định dạng giá vé theo VND
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val priceText = "${formatter.format(showtime.price)} VND"

        holder.tvAvailableSeats.text = "${showtime.availableSeats.size} ghế trống - ${priceText}"

        holder.itemView.setOnClickListener {
            onShowtimeClick(showtime)
        }
    }

    override fun getItemCount() = showtimes.size
}

