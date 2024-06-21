import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HomeScreenBroadcastReceiver(private val onUpdate: (steps: Int, caloriesBurned: Int, distance: Float) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("HomeScreenBroadcast", "Broadcast received")
        val steps = intent?.getIntExtra("steps", 0) ?: 0
        val caloriesBurned = intent?.getIntExtra("caloriesBurned", 0) ?: 0
        val distance = intent?.getFloatExtra("distance", 0f) ?: 0f
        onUpdate(steps, caloriesBurned, distance)
    }
}
