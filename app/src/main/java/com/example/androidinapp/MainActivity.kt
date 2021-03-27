package com.example.androidinapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.androidinapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardPurchaseItem.setOnClickListener {
            startActivity(Intent(this, PurchaseItemActivity::class.java))
        }

        binding.cardSubscribe.setOnClickListener {
            startActivity(Intent(this, SubscribeActivity::class.java))
        }

    }
}