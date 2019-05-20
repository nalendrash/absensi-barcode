package nanz.absen

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils

import android.content.Intent
import android.view.View
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast

class LoginActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btLogin.setOnClickListener {
            pbLogin.visibility = View.VISIBLE
            btLogin.visibility = View.GONE

            val email = etEmailLogin.text
            val password  = etPasswordLogin.text

            if (TextUtils.isEmpty(email)){
                toast("Enter Email!")
            }
            if (TextUtils.isEmpty(password)){
                toast("Enter Password!")
            }

            var context = this

            auth.signInWithEmailAndPassword(email.toString(), password.toString())
                    .addOnCompleteListener(this, object : OnCompleteListener<AuthResult>{
                        override fun onComplete(task: Task<AuthResult>) {
                            pbLogin.visibility = View.GONE
                            btLogin.visibility = View.VISIBLE
                            if (!task.isSuccessful){
                                toast("Login Error")
                            }else{
//                                startActivity(Intent(context, MainActivity::class.java))
                                startActivity(intentFor<MainActivity>("user_name" to email.toString()))
                                finish()
                            }
                        }

                    })
        }
    }

}
