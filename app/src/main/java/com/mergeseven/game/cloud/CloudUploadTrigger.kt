package com.mergeseven.game.cloud

/**
 * Narrow upload surface so gameplay ViewModels do not need the full sync stack in tests.
 */
fun interface CloudUploadTrigger {
    fun requestUpload(reason: UploadReason)
}

class NoOpCloudUploadTrigger : CloudUploadTrigger {
    override fun requestUpload(reason: UploadReason) = Unit
}
