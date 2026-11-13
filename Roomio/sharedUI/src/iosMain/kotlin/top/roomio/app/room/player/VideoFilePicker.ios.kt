@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package top.roomio.app.room.player

import androidx.compose.runtime.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject

@Composable
internal actual fun rememberPlatformVideoPicker(onPicked: (LocalVideoFile) -> Unit, onError: () -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onError)
    val delegate = remember { object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
            val access = url.startAccessingSecurityScopedResource()
            var released = false
            picked(LocalVideoFile(url.absoluteString.orEmpty(),url.lastPathComponent ?: "video") {
                if(!released) { released=true; if(access) url.stopAccessingSecurityScopedResource() }
            })
        }
        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = Unit
    } }
    return {
        var host = UIApplication.sharedApplication.keyWindow?.rootViewController
        while(host?.presentedViewController != null) host=host.presentedViewController
        if(host == null) failed() else {
            val picker=UIDocumentPickerViewController(forOpeningContentTypes=listOf(UTTypeMovie),asCopy=false)
            picker.delegate=delegate
            picker.allowsMultipleSelection=false
            host.presentViewController(picker,animated=true,completion=null)
        }
    }
}
