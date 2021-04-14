package com.jeremy.jappbuild

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jeremy.router.annotations.Destination

@Destination(url = "router://page-kotlin",description = "登录页面")
class KtMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kt_main)
    }
}