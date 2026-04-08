package com.quotamaster

import android.app.Application
import com.quotamaster.di.AppContainer
import com.quotamaster.util.NotificationHelper

class QuotaMasterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        container = AppContainer(this)
    }
}