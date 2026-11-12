package top.roomio.app.room

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.*
import top.roomio.app.profile.ProfileAvatar
import top.roomio.domain.party.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupSyncDialog(state: RoomUiState, onAction: (RoomAction) -> Unit) {
    if (!state.syncDialogOpen) return
    val attempt=state.groupSync?.takeIf { it.active }
    val draft=attempt==null
    val preparing=attempt?.phase==GroupSyncPhase.PREPARING
    val response=attempt?.participants?.find { it.membershipId==state.syncSelfId }?.response
    val title=stringResource(if(draft)Res.string.group_sync_together else if(state.isOwner)Res.string.group_sync_who else Res.string.group_sync_request)
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(24.dp).heightIn(max=620.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { Text(title,Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);IconButton(onClick={onAction(RoomAction.SyncDismiss)}){Icon(Icons.Default.Close,stringResource(Res.string.close))} }
            if(!draft&&!state.isOwner)Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Sync,null);Text(stringResource(Res.string.group_sync_inviter,state.model.ownerName),style=MaterialTheme.typography.titleMedium)}
            Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.extraLarge,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.group_sync_meet))
                    val seconds=(attempt?.targetPositionMs?:state.positionMs)/1000
                    Text("${seconds/60}:${(seconds%60).toString().padStart(2,'0')}",style=MaterialTheme.typography.displayLarge)
                }
            }
            if(preparing)Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){LoadingIndicator();Text(stringResource(Res.string.group_sync_preparing))}
            else if(attempt!=null)attempt.participants.filter{it.membershipId!=attempt.ownerMembershipId}.forEach { person ->
                val member=state.syncMembers.find{it.membershipId==person.membershipId}
                val accepted=person.response==GroupSyncResponse.ACCEPTED;val declined=person.response==GroupSyncResponse.DECLINED
                val color=if(accepted)MaterialTheme.colorScheme.secondaryContainer else if(declined)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
                val ink=if(accepted)MaterialTheme.colorScheme.onSecondaryContainer else if(declined)MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                Surface(color=color,contentColor=ink,shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        val avatar=runCatching{ProfileAvatar.valueOf(member?.avatar.orEmpty().uppercase())}.getOrDefault(ProfileAvatar.COMET)
                        Image(painterResource(avatar.resource),null,Modifier.size(40.dp).clip(CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(if(person.membershipId==state.syncSelfId)stringResource(Res.string.chat_you) else member?.displayName.orEmpty(),style=MaterialTheme.typography.titleSmall)
                            Text(stringResource(when(person.response){GroupSyncResponse.ACCEPTED->Res.string.group_sync_accepted;GroupSyncResponse.DECLINED->Res.string.group_sync_declined;GroupSyncResponse.WITHDRAWN->Res.string.group_sync_withdrawn;else->if(person.membershipId==state.syncSelfId)Res.string.group_sync_pending_you else Res.string.group_sync_pending}),style=MaterialTheme.typography.bodySmall)
                        }
                        if(accepted)Icon(Icons.Default.Check,null)else if(declined)Icon(Icons.Default.Close,null)
                    }
                }
            }
            if(!preparing)FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.End),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(draft)Button(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncAsk)}){Text(stringResource(Res.string.group_sync_ask))}
                else if(state.isOwner){TextButton(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncCancel)}){Text(stringResource(Res.string.group_sync_cancel))};Button(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncCommit)}){Text(stringResource(Res.string.group_sync_start,attempt!!.participants.count{it.response==GroupSyncResponse.ACCEPTED}))}}
                else if(response==GroupSyncResponse.ACCEPTED)OutlinedButton(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncRespond(GroupSyncResponse.WITHDRAWN))}){Text(stringResource(Res.string.group_sync_withdraw))}
                else {TextButton(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncRespond(GroupSyncResponse.DECLINED))}){Text(stringResource(Res.string.group_sync_no))};Button(enabled=!state.syncBusy && state.chat.connected,onClick={onAction(RoomAction.SyncRespond(GroupSyncResponse.ACCEPTED))}){Text(stringResource(Res.string.group_sync_accept))}}
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if(maxWidth<600.dp)ModalBottomSheet(onDismissRequest={onAction(RoomAction.SyncDismiss)},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)){content()}
        else Dialog(onDismissRequest={onAction(RoomAction.SyncDismiss)}){Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceContainerHigh){content()}}
    }
}
@Composable
internal fun GroupSyncCountdown(value: String?) {
    if(value==null)return
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha=.55f)),contentAlignment=Alignment.Center){
        Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.primaryContainer){Text(if(value=="Action")stringResource(Res.string.group_sync_action)else value,Modifier.padding(32.dp),style=MaterialTheme.typography.displayLarge,textAlign=TextAlign.Center)}
    }
}
