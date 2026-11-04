package com.simpletools.app

import android.app.Application

class SimpleToolsApplication : Application() {

    /** Manual dependency injection: small, explicit, no DI framework to ship. */
    val container: AppContainer by lazy { AppContainer(this) }
}
