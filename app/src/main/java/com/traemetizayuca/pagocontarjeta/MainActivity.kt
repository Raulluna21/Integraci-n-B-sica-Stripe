package com.traemetizayuca.pagocontarjeta

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnpay = findViewById<Button>(R.id.btnpay)
        val edPrice = findViewById<EditText>(R.id.eDprice)

        btnpay.setOnClickListener {
            if (edPrice.length() != 0){
                val precio : String = edPrice.text.toString()
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("edPrice", precio)
                startActivity(intent)
            }else{
                Toast.makeText(this, "Ingresa un número", Toast.LENGTH_SHORT).show()
            }
        }
    } //final

}