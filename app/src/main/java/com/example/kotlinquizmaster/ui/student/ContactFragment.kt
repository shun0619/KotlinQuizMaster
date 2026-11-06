package com.example.kotlinquizmaster.ui.student

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.FragmentContactBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/** Google Maps of the campus, plus Call and Email buttons that use implicit intents. */
class ContactFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            map.addMarker(MarkerOptions().position(CAMPUS).title(getString(R.string.campus_name)))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(CAMPUS, 15f))
            map.uiSettings.isZoomControlsEnabled = true
        }

        // Dial a number (no CALL_PHONE permission needed).
        binding.callButton.setOnClickListener {
            launch(Intent(Intent.ACTION_DIAL, "tel:${getString(R.string.ais_phone)}".toUri()))
        }
        // Send an email to the instructor.
        binding.emailButton.setOnClickListener {
            val email = QuizRepository(requireContext()).firstInstructorEmail() ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                .putExtra(Intent.EXTRA_SUBJECT, "Question about Kotlin Quiz Master")
            launch(intent)
        }
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No app on this device can do that.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val CAMPUS = LatLng(-36.8800, 174.7180)
    }
}
