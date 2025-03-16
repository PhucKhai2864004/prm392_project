package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Booking
import java.text.SimpleDateFormat
import java.util.Locale

class BookingAdapter(
    private val bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMovieTitle: TextView = view.findViewById(R.id.tvMovieTitle)
        val tvCinemaAndDate: TextView = view.findViewById(R.id.tvCinemaAndDate)
        val tvSeats: TextView = view.findViewById(R.id.tvSeats)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        holder.tvMovieTitle.text = booking.movieTitle
        holder.tvCinemaAndDate.text = "${booking.cinemaName} | ${dateFormat.format(booking.showDate)} | ${booking.showTime}"
        holder.tvSeats.text = "Seats: ${booking.seats.joinToString(", ")}"
        holder.tvStatus.text = booking.status

        holder.itemView.setOnClickListener {
            onBookingClick(booking)
        }
    }

    override fun getItemCount() = bookings.size
}

