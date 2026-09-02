package com.ridvan.target

import android.app.Application
import com.ridvan.target.data.local.TargetDatabase

class TargetApplication : Application() {
    val database: TargetDatabase by lazy { TargetDatabase.getInstance(this) }
}
