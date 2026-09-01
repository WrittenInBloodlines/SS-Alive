package com.ss.alive
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ss.alive.alive.*
class MainActivity:AppCompatActivity(){
private lateinit var rootView:LinearLayout
override fun onCreate(s:Bundle?){super.onCreate(s);showHome()}
override fun onResume(){super.onResume();if(::rootView.isInitialized)showHome()}
private fun showHome(){rootView=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(48,64,48,48)}
rootView.addView(TextView(this).apply{text="S•S ALIVE";textSize=32f})
rootView.addView(TextView(this).apply{text="Create characters that can live on your screen.";textSize=17f})
rootView.addView(Button(this).apply{text="CREATE ALIVE";setOnClickListener{startActivity(Intent(this@MainActivity,AliveEditorActivity::class.java))}})
rootView.addView(TextView(this).apply{text="TEMPLATES";textSize=22f;setPadding(0,28,0,8)})
card("CAT","🐈","Cat");card("DOG","🐕","Dog");card("CHICK","🐥","Chick")
rootView.addView(Button(this).apply{text="ALLOW DISPLAY OVER OTHER APPS";setOnClickListener{startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}})
rootView.addView(Button(this).apply{text="START EQUIPPED ALIVES";setOnClickListener{startEquipped()}})
rootView.addView(Button(this).apply{text="STOP ALIVE";setOnClickListener{stopService(Intent(this@MainActivity,AliveService::class.java))}})
val ps=AliveRepository.all(this).filter{!it.isTemplate};if(ps.isNotEmpty()){rootView.addView(TextView(this).apply{text="YOUR ALIVES";textSize=21f});ps.forEach{p->rootView.addView(Button(this).apply{text=if(AliveRepository.isEquipped(this@MainActivity,p.id))"UNEQUIP "+p.name.uppercase() else "EQUIP "+p.name.uppercase();setOnClickListener{if(AliveRepository.isEquipped(this@MainActivity,p.id))AliveRepository.unequip(this@MainActivity,p.id)else AliveRepository.equip(this@MainActivity,p);showHome()}})}
setContentView(rootView)}
private fun card(k:String,e:String,n:String){val p=AliveRepository.template(this,k);rootView.addView(TextView(this).apply{text="$e  $n";textSize=19f});rootView.addView(Button(this).apply{text=if(AliveRepository.isEquipped(this@MainActivity,p.id))"UNEQUIP" else "EQUIP";setOnClickListener{if(AliveRepository.isEquipped(this@MainActivity,p.id))AliveRepository.unequip(this@MainActivity,p.id)else AliveRepository.equip(this@MainActivity,p);showHome()}})}
private fun startEquipped(){if(AliveRepository.equipped(this).isEmpty()){Toast.makeText(this,"Equip at least one Alive first",Toast.LENGTH_SHORT).show();return};if(!Settings.canDrawOverlays(this)){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));return};ContextCompat.startForegroundService(this,Intent(this,AliveService::class.java))}
}