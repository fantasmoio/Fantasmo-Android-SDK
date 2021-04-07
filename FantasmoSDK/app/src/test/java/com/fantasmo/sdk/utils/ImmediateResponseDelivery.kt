package com.fantasmo.sdk.utils

import com.android.volley.ExecutorDelivery
import java.util.concurrent.Executor

class ImmediateResponseDelivery(handler: Executor) : ExecutorDelivery(handler) {
    constructor() : this((Executor { command -> command.run() }))
}