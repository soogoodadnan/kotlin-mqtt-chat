package com.starj.mqttchat.ui.chat

import com.google.gson.Gson
import com.starj.mqttchat.common.BaseMvpPresenter
import com.starj.mqttchat.common.BaseMvpView
import com.starj.mqttchat.datas.Author
import com.starj.mqttchat.datas.Message
import com.starj.mqttchat.mqtt.MqttManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.eclipse.paho.client.mqttv3.MqttMessage


class ChatPresenter<MvpView : BaseMvpView> : BaseMvpPresenter<MvpView> {

    private lateinit var view: ChatMvpView
    private lateinit var mqttManager: MqttManager

    private val gson = Gson()

    override fun attachView(view: MvpView) {
        this.view = view as ChatMvpView
    }

    fun connect(author: Author, topic: String) {
        mqttManager = MqttManager(author, topic, actionOnSubscribed())
        mqttManager.connect()
    }

    override fun destroy() = mqttManager.disconnect()

    private fun actionOnSubscribed(): (String, MqttMessage) -> Unit {
        return { _, mqttMessage ->
            Single.create<Message> {
                val message: Message = gson.fromJson(mqttMessage.toString(), Message::class.java)

                it.onSuccess(message)
            }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(view::onReceiveMessage, Throwable::printStackTrace)
        }
    }

    fun sendMessage(input: CharSequence?) {
        val message = Message(author = mqttManager.author, text = input.toString())

        mqttManager.publish(gson.toJson(message))
    }
}