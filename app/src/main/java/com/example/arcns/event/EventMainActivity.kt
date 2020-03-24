package com.example.arcns.event

class EventMainActivity(
    var type: EventMainActivityType,
    var data: Any? = null
)

enum class EventMainActivityType {
    onDataInterfaceReLogin,
    onDataInterfaceUpdateCurrentUser
}