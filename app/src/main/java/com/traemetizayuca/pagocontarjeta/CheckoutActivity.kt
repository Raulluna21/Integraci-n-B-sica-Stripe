package com.traemetizayuca.pagocontarjeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardInputWidget
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.ArrayList

class CheckoutActivity : AppCompatActivity() {
    //private val backendUrl = "http://1.1.1.10:4242/" Servidor local
    private val backendUrl = "https://link-del-servidor-en.herokuapp.com/" //Servidor en Heroku
    private val httpClient = OkHttpClient()
    private lateinit var paymentIntentClientSecret : String
    private lateinit var stripe : Stripe
    private var precioReal : String = ""
    private var precioMul : Int = 0


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)


        val vistaPrecio = findViewById<TextView>(R.id.vistaPrecio)
        val precioReal2 = intent.getStringExtra("edPrice")

        if (precioReal2 != null) {
            precioReal = precioReal2
            precioMul = precioReal.toInt() *100
            vistaPrecio.text = "El precio real a pagar es $precioReal MXN de $precioMul"
        }

        stripe = Stripe(applicationContext, "pk_test_51HxH2nGOApI9wW6huovpLtcPw8yXVmTEWirKgj1O5reFMVAeJ6ToUIQDjyexS7j4YxAeDy1tIIv6Ina4isGXFaAG005IxQyoeY")
        startCheckout()
    }

    private fun startCheckout() {
        val weakActivity = WeakReference<Activity>(this)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val amount : Double = precioMul.toDouble()
        val payMap = HashMap<String, Any>()
        val itemMap = HashMap<String, Any>()
        val itemList = ArrayList<HashMap<String, Any>>()
        payMap["currency"] = "MXN"
        itemMap["id"] = "HOLS"
        itemMap["name"] = "Algún nombre"
        itemMap["email"] = "Algún correo@gmail.com"
        itemMap["amount"] = amount
        itemList.add(itemMap)
        payMap["items"] = itemList
        val requestJson : String = Gson().toJson(payMap)
        val body = requestJson.toRequestBody(mediaType)
        val request = Request.Builder().url(backendUrl + "create-payment-intent").post(body).build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                weakActivity.get()?.let { activity -> displayAlert(activity, "Error al conectar con el servidor", "Error: $e")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    weakActivity.get()?.let { activity -> displayAlert(activity, "Error al conectar con el servidor", "Error: $response")
                    }
                } else {
                    val responseData = response.body?.string()
                    val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                    paymentIntentClientSecret = responseJson.getString("clientSecret")
                }
            }
        })
        val payButton: Button = findViewById(R.id.payButton)
        payButton.setOnClickListener {
            val cardInputWidget = findViewById<CardInputWidget>(R.id.cardInputWidget)
            cardInputWidget.paymentMethodCreateParams?.let { params ->
                val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(params, paymentIntentClientSecret)
                stripe.confirmPayment(this, confirmParams)
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val weakActivity = WeakReference<Activity>(this)

        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            @SuppressLint("ShowToast")
            override fun onSuccess(result: PaymentIntentResult) {
                val paymentIntent = result.intent
                val status = paymentIntent.status
                if (status == StripeIntent.Status.Succeeded) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    weakActivity.get()?.let { activity ->
                        displayAlert(activity, "Payment succeeded", gson.toJson(paymentIntent))
                    }
                } else if (status == StripeIntent.Status.RequiresPaymentMethod) {
                    weakActivity.get()?.let { activity ->
                        displayAlert(activity, "Payment failed required method", paymentIntent.lastPaymentError?.message.orEmpty())
                    }
                }
            }
            override fun onError(e: Exception) {
                weakActivity.get()?.let { activity ->
                    displayAlert(activity, "Payment failed", e.toString())
                }
            }
        })
    }

    private fun displayAlert(activity: Activity, title: String, message: String, restartDemo: Boolean = false) {
        runOnUiThread {
            val builder = AlertDialog.Builder(activity).setTitle(title).setMessage(message)
            builder.setPositiveButton("Ok", null)
            builder.create().show()
        }
    }
}

