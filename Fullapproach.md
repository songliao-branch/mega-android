
###Overview of full approach
The MEGAchat SDK has an internal send message queue that does not provide API for modifying, thus the only way is to use a pre-emptive approach.
When offline, instead of calling sendMessage immediately, treat the message as a draft and add to a offline message queue. When connected, send it. This allows deletion without touching SDK’s internal sending queue.

###In ChatViewModel.kt

fun sendMessage(msg){  
	if (_state.value.isConnected) {
		sendTextMessageUseCase(chatId, msg)
	} else {
		// create a draft offline message with random Long msgId X so UI can display,
		UpdateTempTextMessageUseCase(chatId, msg)// this updates UI only
		OfflineTextMessageQueue.enqueue(SendOfflineTextWhenOnlineWorker(chatId, msg, X))
	}
}
fun onDeletedMessages(messages: Set<TypedMessage>) {
	if (_state.value.isConnected) {
		// proceed to normal deletion
		deleteMessagesUseCase(messages)
	}  else {
		val mgsIds = messages.map { it.msgId }
		OfflineTextMessageQueue.cancel(msgIds)
		DeleteLocalMessageUseCase(msgIds)// delete from local db immediately to update the UI
	}

## new classes	
class UpdateTempTextMessageUseCase(chatId, msg) {
	fun invoke() {
		long id = System.currentTimeMillis()
		typedMessageDao.insert(id, chatId, msg) // this will update the UI
	}
}

class SendOfflineTextWhenOnlineWorker(tempId, chatId, msg) {
 	fun doWork() {
		ChatMessage sdkMsg = megaChatApi.sendMessage(chatId, msg)
		//  megaChat SDK returns a dedicated identifier msgId, use this to update existing record in db
		// this will not cause UI to change
	 	typedMessageDao.update(tempId, sdkMsg.msgId)
	}
}

class DeleteLocalMessageUseCase(chatId, msgId) {
	fun invoke() {
		typedMessaegDao.delete(chatId, msgId)
	}
}

class OfflineSendTextWorker (constraint Connected, chatId, msg: String) {
	fun doWork() {
	long tempId = random()
	val sdkMsg = chatRepository.sendMessage(chatId, message) // megachat api
	chatRepository.updateMsgId(tempId, sdkMsg.msgId)// update local db
}

}
class OfflineTextMessageQueue {

	fun enqueue(chatId, msg, X:msgId) {
	 val workRequest = OneTimeWorkRequestBuilder<OfflineSendTextWorker>()
            .setInputData(chatId, msg)
            .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
            .addTag(msgId)
            .build()

	//create one time job with constraint requires Internet
		WorkManager.getInstance(context)
           	 .beginUniqueWork(
              	  "$offlinetext$msgId”,
              	  ExistingWorkPolicy.REPLACE,
              	  workRequest)
            .enqueue()
	}

	fun cancel(list<MsgId>) {
		WorkManager.getInstance(context)
           	 .cancelAllWorkByTag(messageId)
		//remove all jobs with msg ids
	}
}
