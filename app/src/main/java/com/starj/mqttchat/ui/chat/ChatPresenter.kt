package com.starj.mqttchat.ui.chat

import com.google.gson.Gson
import com.starj.mqttchat.common.BaseMvpPresenter
import com.starj.mqttchat.common.BaseMvpView
import com.starj.mqttchat.common.RxPresenter
import com.starj.mqttchat.datas.Author
import com.starj.mqttchat.datas.Message
import com.starj.mqttchat.extensions.onMain
import com.starj.mqttchat.mqtt.MqttManager
import io.reactivex.Single
import org.eclipse.paho.client.mqttv3.MqttMessage


class ChatPresenter<MvpView : BaseMvpView> : RxPresenter(), BaseMvpPresenter<MvpView> {

    private lateinit var view: ChatMvpView
    private lateinit var mqttManager: MqttManager

    private val gson = Gson()

    override fun attachView(view: MvpView) {
        this.view = view as ChatMvpView
    }

    override fun destroy() {
        dispose()
        mqttManager.disconnect()
    }

    fun connect(author: Author, topic: String) {
        add(Single.create<Unit> {
            mqttManager = MqttManager(author, topic, actionOnSubscribed())

            when {
                mqttManager.connect() -> it.onSuccess(Unit)
                else -> it.onError(IllegalAccessException("Fail to connect"))
            }
        }.onMain().subscribe({ view.onSuccessConnect() }, { view.onErrorConnect() }))
    }

    private fun actionOnSubscribed(): (String, MqttMessage) -> Unit {
        return { _, mqttMessage ->
            add(Single.create<Message> {
                val message: Message = gson.fromJson(mqttMessage.toString(), Message::class.java)

                it.onSuccess(message)
            }.onMain().subscribe(view::onReceiveMessage, Throwable::printStackTrace))
        }
    }

    fun sendMessage(input: CharSequence?) {
        add(Single.create<String> {
            val message = Message(author = mqttManager.author, text = input.toString())

            it.onSuccess(gson.toJson(message))
        }.onMain().subscribe({ mqttManager.publish(it) }, {}))
    }
}