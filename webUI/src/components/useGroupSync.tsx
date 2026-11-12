import { logHandledException } from '../data/sessionLog';
import { useEffect, useRef, useState, type MutableRefObject } from 'react';
import type { Snapshot } from '../data/contracts';
import type { RoomConnection } from '../data/realtime';
import { activeSync, type GroupSyncState, type SyncStateFrame } from '../data/groupSync';
import { GroupSyncDialog } from './GroupSyncDialog';
import { readyForSync, type PlayerSyncBridge, type PlayerSyncInfo } from './playerSync';
import { t } from '../i18n';
interface Options { snapshot:Snapshot|null;identityId:string;connected:boolean;connection:MutableRefObject<RoomConnection|null>;frame:SyncStateFrame|null;notify(message:string):void }
export function useGroupSync(o:Options) {
 const bridge=useRef<PlayerSyncBridge|null>(null);
 const [open,setOpen]=useState(false),[busy,setBusy]=useState(false),[tick,setTick]=useState(0);
 const [engaged,setEngaged]=useState(false);
 const saved=useRef<PlayerSyncInfo|null>(null),attempt=useRef(''),prepared=useRef(''),lastReady=useRef<boolean|null>(null),started=useRef(''),failed=useRef('');
 const stateRef=useRef<GroupSyncState|null>(null),applied=useRef(''),version=useRef(-1);
 const [state,setState]=useState<GroupSyncState|null>(null);
 const self=o.snapshot?.members.find(x=>x.identityId===o.identityId);const owner=!!self?.isOwner;
 const session=o.snapshot?.stream.sessionId||'';const ownerName=o.snapshot?.members.find(x=>x.isOwner)?.displayName||o.snapshot?.ownerName||t('Host');
 const included=state?.participants.some(x=>x.membershipId===self?.membershipId&&x.response==='ACCEPTED')||false;
 const invoke=async(action:'invite'|'respond'|'commit'|'cancel'|'ready'|'failed',extra:Record<string,unknown>={})=>{
   if(!o.connection.current)throw Error('NETWORK');return o.connection.current.syncCommand(action,{requestId:stateRef.current?.requestId||attempt.current,...extra});
 };
 const restore=async(play?:boolean)=>{const player=bridge.current;const before=saved.current;saved.current=null;setEngaged(false);prepared.current='';lastReady.current=null;if(player&&before){player.seek(before.positionMs);if(play??before.playing)await player.play();else await player.pause();}};
 const fail=(message:string)=>{setEngaged(false);setOpen(false);saved.current=null;o.notify(message);};
 useEffect(()=>{const timer=setInterval(()=>setTick(x=>x+1),100);return()=>clearInterval(timer);},[]);
 useEffect(()=>{
   stateRef.current=null;setState(null);setOpen(false);setEngaged(false);saved.current=null;attempt.current='';prepared.current='';applied.current='';version.current=-1;
 },[session,o.identityId]);
 useEffect(()=>{
   const frame=o.frame;if(!frame)return;const next=frame.state;
   if(next&&next.streamSessionId!==session)return;
   if(next&&stateRef.current&&next.requestId!==stateRef.current.requestId&&next.createdAtMs<stateRef.current.createdAtMs)return;
   if(next&&stateRef.current?.requestId===next.requestId&&next.revision<=version.current)return;
   version.current=next?.revision??-1;stateRef.current=next;setState(next);
   if(!next){if(saved.current)void restore().catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.1", handledError); });return;}
   if(applied.current!==next.requestId){applied.current=next.requestId;if(next.phase==='INVITING')setOpen(true);}
   const mine=next.participants.find(x=>x.membershipId===self?.membershipId);
   if(next.phase==='INVITING'&&mine?.response==='ACCEPTED'&& !saved.current && bridge.current){saved.current=bridge.current.info();void bridge.current.pause();setEngaged(true);}
   if(next.phase==='CANCELLED'){setOpen(false);void restore().catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.2", handledError); });o.notify(t('Sync cancelled'));}
   if(next.phase==='FAILED'){fail(t('Sync stopped. You can keep watching.'));}
   if(mine?.response==='EXCLUDED'&&saved.current){fail(t('You couldn’t join this sync. You can keep watching.'));}
   if(next.phase==='SCHEDULED')setOpen(false);
   if(next.phase==='COMPLETED'&&owner&&next.participants.some(x=>x.response==='EXCLUDED'))o.notify(t('Started with ready viewers. Others can catch up.'));
 },[o.frame,session,self?.membershipId]);
 // Real player readiness and local scheduling share the server-calibrated clock.
 useEffect(()=>{
   const s=stateRef.current,player=bridge.current;if(!s||!player||!included||!activeSync(s))return;
   if(s.phase!=='PREPARING'&&s.phase!=='SCHEDULED')return;
   if(prepared.current!==s.requestId){failed.current='';prepared.current=s.requestId;lastReady.current=null;setEngaged(true);if(player.prepare)void player.prepare(s.targetPositionMs).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.3", handledError); void invoke('failed',{requestId:s.requestId,reason:'PREPARATION_FAILED'}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.4", handledError); });});else{void player.pause();player.seek(s.targetPositionMs);}}
   const info=player.info();
   if(info.error||!Number.isNaN(info.durationMs)&&(!Number.isFinite(info.durationMs)||info.durationMs<=0)){if(failed.current!==s.requestId){failed.current=s.requestId;void invoke('failed',{reason:'UNSUPPORTED_PLAYER'}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.5", handledError); });}return;}
   const ready=readyForSync(info,s.targetPositionMs);
   if(s.phase==='PREPARING'&&ready!==lastReady.current){lastReady.current=ready;void invoke('ready',{ready}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.6", handledError); lastReady.current=null;});}
   if(s.phase==='SCHEDULED'&&s.startAtMs){
     if(!o.connected){started.current=s.requestId;fail(t('Sync stopped. You can keep watching.'));return;}
     const remaining=s.startAtMs-(o.connection.current?.syncClock.now()??Date.now());
     if(!ready&&remaining>0){if(failed.current===s.requestId)return;failed.current=s.requestId;void invoke('failed',{reason:'READINESS_LOST'}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.7", handledError); });return;}
     if(remaining<=0&&started.current!==s.requestId){started.current=s.requestId;if(remaining< -750){void invoke('failed',{reason:'LATE_START'}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.8", handledError); });fail(t('Sync stopped. You can keep watching.'));return;}void player.play().then(()=>{saved.current=null;setEngaged(false);}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.9", handledError); void invoke('failed',{reason:'PLAYBACK_DENIED'}).catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.10", handledError); });fail(t('Select Play to start the video on this device.'));});}
   }
 },[tick,included]);
 useEffect(()=>{
   if(!engaged)return;
   const wait=o.connected?Math.max(1000,(state?.deadlineMs??Date.now()+130000)-(o.connection.current?.syncClock.now()??Date.now())+5000):12000;
   const timeout=setTimeout(()=>{void restore().catch((handledError)=>{
     logHandledException("useGroupSync.useGroupSync.timeout.11", handledError); });setOpen(false);o.notify(t('Sync stopped. You can keep watching.'));},wait);
   return()=>clearTimeout(timeout);
 },[engaged,o.connected,state?.phase,state?.deadlineMs]);
 const run=async(operation:()=>Promise<void>)=>{if(busy)return;setBusy(true);try{await operation();}catch (handledError){ logHandledException("useGroupSync.useGroupSync.run.12", handledError); if(saved.current&&!activeSync(stateRef.current))await restore().catch((handledError)=>{ logHandledException("useGroupSync.useGroupSync.run.13", handledError); });o.notify(t('Sync unavailable. Try again.'));}finally{setBusy(false);}};
 const ask=()=>void run(async()=>{const p=bridge.current;if(!p)throw Error('PLAYER_UNAVAILABLE');saved.current=p.info();await p.pause();setEngaged(true);attempt.current=crypto.randomUUID();await invoke('invite',{requestId:attempt.current,positionMs:Math.round(p.info().positionMs)});});
 const respond=(response:'ACCEPTED'|'DECLINED'|'WITHDRAWN')=>void run(async()=>{
   if(response==='ACCEPTED'){const p=bridge.current;if(!p)throw Error('PLAYER_UNAVAILABLE');saved.current=p.info();await p.pause();setEngaged(true);}
   await invoke('respond',{response});if(response==='WITHDRAWN'){setOpen(false);await restore(true);}if(response==='DECLINED')setOpen(false);
 });
 const openSync=()=>{if(stateRef.current?.phase==='SCHEDULED')return;if(activeSync(stateRef.current)||owner){setOpen(true);return;}void run(async()=>{const p=bridge.current;if(!p||!o.connection.current)throw Error('PLAYER_UNAVAILABLE');const result=await o.connection.current.sync(p.info().positionMs);if(result?.status!=='APPLIED')throw Error('UNAVAILABLE');p.seek(result.targetPositionMs);if(result.playing)await p.play();else await p.pause();o.notify(t('Caught up to {name}',{name:ownerName}));});};
 const remaining=state?.startAtMs?state.startAtMs-(o.connection.current?.syncClock.now()??Date.now()):0;
 const overlay=state?.phase==='SCHEDULED'&&included&&remaining>=-500?<div className="group-sync-countdown" role="status" aria-live="polite"><strong>{remaining>0?Math.min(3,Math.ceil(remaining/1000)):t('Action')}</strong></div>:null;
 return {bridge,locked:engaged,busy,openSync,overlay,dialog:<GroupSyncDialog open={open} owner={owner} ownerName={ownerName} selfId={self?.membershipId||''} state={state} positionMs={bridge.current?.info().positionMs||0} members={o.snapshot?.members||[]} busy={busy} connected={o.connected} close={()=>setOpen(false)} ask={ask} respond={respond} commit={()=>void run(async()=>{await invoke('commit');})} cancel={()=>void run(async()=>{await invoke('cancel');})}/>};
}

export type GroupSyncUi = ReturnType<typeof useGroupSync>;
