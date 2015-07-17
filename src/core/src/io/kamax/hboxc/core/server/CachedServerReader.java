/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2013 Maxime Dor
 * 
 * http://kamax.io/hbox/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.hboxc.core.server;

import net.engio.mbassy.listener.Handler;
import io.kamax.hbox.comm.in.MachineIn;
import io.kamax.hbox.comm.in.MediumIn;
import io.kamax.hbox.comm.in.NetworkAttachModeIn;
import io.kamax.hbox.comm.in.SessionIn;
import io.kamax.hbox.comm.in.SnapshotIn;
import io.kamax.hbox.comm.in.StorageControllerIn;
import io.kamax.hbox.comm.in.StorageControllerSubTypeIn;
import io.kamax.hbox.comm.in.StorageControllerTypeIn;
import io.kamax.hbox.comm.in.StoreIn;
import io.kamax.hbox.comm.in.StoreItemIn;
import io.kamax.hbox.comm.in.TaskIn;
import io.kamax.hbox.comm.in.UserIn;
import io.kamax.hbox.comm.out.ModuleOut;
import io.kamax.hbox.comm.out.SessionOut;
import io.kamax.hbox.comm.out.StoreItemOut;
import io.kamax.hbox.comm.out.StoreOut;
import io.kamax.hbox.comm.out.TaskOut;
import io.kamax.hbox.comm.out.event.hypervisor.HypervisorConnectionStateEventOut;
import io.kamax.hbox.comm.out.event.machine.MachineDataChangeEventOut;
import io.kamax.hbox.comm.out.event.machine.MachineRegistrationEventOut;
import io.kamax.hbox.comm.out.event.machine.MachineSnapshotDataChangedEventOut;
import io.kamax.hbox.comm.out.event.machine.MachineStateEventOut;
import io.kamax.hbox.comm.out.event.module.ModuleEventOut;
import io.kamax.hbox.comm.out.event.snapshot.SnapshotDeletedEventOut;
import io.kamax.hbox.comm.out.event.snapshot.SnapshotModifiedEventOut;
import io.kamax.hbox.comm.out.event.snapshot.SnapshotRestoredEventOut;
import io.kamax.hbox.comm.out.event.snapshot.SnapshotTakenEventOut;
import io.kamax.hbox.comm.out.event.task.TaskQueueEventOut;
import io.kamax.hbox.comm.out.event.task.TaskStateEventOut;
import io.kamax.hbox.comm.out.host.HostOut;
import io.kamax.hbox.comm.out.hypervisor.HypervisorLoaderOut;
import io.kamax.hbox.comm.out.hypervisor.MachineOut;
import io.kamax.hbox.comm.out.hypervisor.OsTypeOut;
import io.kamax.hbox.comm.out.hypervisor.ScreenshotOut;
import io.kamax.hbox.comm.out.hypervisor.SnapshotOut;
import io.kamax.hbox.comm.out.network.NetworkAttachModeOut;
import io.kamax.hbox.comm.out.network.NetworkAttachNameOut;
import io.kamax.hbox.comm.out.network.NetworkInterfaceOut;
import io.kamax.hbox.comm.out.network.NetworkInterfaceTypeOut;
import io.kamax.hbox.comm.out.security.PermissionOut;
import io.kamax.hbox.comm.out.security.UserOut;
import io.kamax.hbox.comm.out.storage.MediumOut;
import io.kamax.hbox.comm.out.storage.StorageControllerSubTypeOut;
import io.kamax.hbox.comm.out.storage.StorageControllerTypeOut;
import io.kamax.hbox.comm.out.storage.StorageDeviceAttachmentOut;
import io.kamax.hbox.event.HyperboxEvents;
import io.kamax.hbox.exception.HyperboxException;
import io.kamax.hbox.states.TaskQueueEvents;
import io.kamax.hboxc.event.EventManager;
import io.kamax.hboxc.event.machine.MachineAddedEvent;
import io.kamax.hboxc.event.machine.MachineDataChangedEvent;
import io.kamax.hboxc.event.machine.MachineRemovedEvent;
import io.kamax.hboxc.event.machine.MachineStateChangedEvent;
import io.kamax.hboxc.event.module.ModuleEvent;
import io.kamax.hboxc.event.snapshot.SnapshotDeletedEvent;
import io.kamax.hboxc.event.snapshot.SnapshotModifiedEvent;
import io.kamax.hboxc.event.snapshot.SnapshotRestoredEvent;
import io.kamax.hboxc.event.snapshot.SnapshotTakenEvent;
import io.kamax.hboxc.event.task.TaskAddedEvent;
import io.kamax.hboxc.event.task.TaskRemovedEvent;
import io.kamax.hboxc.event.task.TaskStateChangedEvent;
import io.kamax.hboxc.server._GuestReader;
import io.kamax.hboxc.server._Hypervisor;
import io.kamax.hboxc.server._Machine;
import io.kamax.hboxc.server._ServerReader;
import io.kamax.hboxc.server.task._Task;
import io.kamax.tool.AxStrings;
import io.kamax.tool.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CachedServerReader implements _ServerReader {

   private _ServerReader reader;

   private Map<String, MachineOut> mOutListCache;
   private Long mOutListCacheUpdate;
   private Map<String, MachineOut> mOutCache;
   private Set<String> invalidMachineUuidSet;

   private Map<String, TaskOut> tOutListCache;
   private Long tOutListCacheUpdate;
   private Map<String, TaskOut> tOutCache;
   private Set<String> invalidTaskIdSet;

   private Map<String, MediumOut> medOutCache;
   private Set<String> invalidMedOutSet;

   private Map<String, Map<String, SnapshotOut>> snapOutCache;

   public CachedServerReader(_ServerReader reader) {
      this.reader = reader;
      reset();
      EventManager.get().register(this);
   }

   private void reset() {
      mOutListCache = new ConcurrentHashMap<String, MachineOut>();
      mOutCache = new ConcurrentHashMap<String, MachineOut>();
      invalidMachineUuidSet = new LinkedHashSet<String>();

      tOutListCache = new ConcurrentHashMap<String, TaskOut>();
      tOutCache = new ConcurrentHashMap<String, TaskOut>();
      invalidTaskIdSet = new LinkedHashSet<String>();

      medOutCache = new HashMap<String, MediumOut>();
      invalidMedOutSet = new LinkedHashSet<String>();

      snapOutCache = new HashMap<String, Map<String, SnapshotOut>>();
   }

   private void insertSnapshot(String vmId, SnapshotOut snapOut) {
      if (!snapOutCache.containsKey(vmId)) {
         snapOutCache.put(vmId, new HashMap<String, SnapshotOut>());
      }
      snapOutCache.get(vmId).put(snapOut.getUuid(), snapOut);
   }

   private void updateSnapshot(String vmId, SnapshotOut snapOut) {
      if (snapOutCache.containsKey(vmId)) {
         snapOutCache.get(vmId).put(snapOut.getUuid(), snapOut);
      }
   }

   private void removeSnapshot(String vmId, String snapId) {
      if (snapOutCache.containsKey(vmId)) {
         snapOutCache.get(vmId).remove(snapId);
      }
   }

   private void refreshSnapshot(String vmUuid, String snapUuid) {
      try {
         SnapshotOut snapOut = reader.getSnapshot(vmUuid, snapUuid);
         insertSnapshot(vmUuid, snapOut);
      } catch (Throwable t) {
         Logger.error("Unable to refresh Snapshot #" + snapUuid + " of VM #" + vmUuid + ": " + t.getMessage());
      }
   }

   @Override
   public String getId() {
      return reader.getId();
   }

   @Override
   public String getName() {
      return reader.getName();
   }

   @Override
   public String getType() {
      return reader.getType();
   }

   @Override
   public String getVersion() {
      return reader.getVersion();
   }

   @Override
   public SnapshotOut getRootSnapshot(String vmId) {
      return reader.getRootSnapshot(vmId);
   }

   @Override
   public SnapshotOut getSnapshot(MachineIn mIn, SnapshotIn snapIn) {
      if (!snapOutCache.containsKey(mIn.getUuid()) || !snapOutCache.get(mIn.getUuid()).containsKey(snapIn.getUuid())) {
         refreshSnapshot(mIn.getUuid(), snapIn.getUuid());
      }

      return snapOutCache.get(mIn.getUuid()).get(snapIn.getUuid());
   }

   @Override
   public SnapshotOut getCurrentSnapshot(MachineIn mIn) {
      if (mOutCache.containsKey(mIn.getId())) {
         return getSnapshot(mIn.getId(), mOutCache.get(mIn.getId()).getCurrentSnapshot());
      } else {
         return reader.getCurrentSnapshot(mIn);
      }
   }

   @Handler
   private void putMachineSnapDataChangedEvent(MachineSnapshotDataChangedEventOut ev) {
      snapOutCache.remove(ev.getUuid());
   }

   @Handler
   private void putSnapshotTakenEvent(SnapshotTakenEventOut ev) {
      updateMachine(ev.getMachine());
      refreshSnapshot(ev.getMachine().getUuid(), ev.getSnapshotUuid());
      refreshSnapshot(ev.getMachine().getUuid(), ev.getSnapshot().getParentUuid());
      EventManager.post(new SnapshotTakenEvent(ev.getServer(), ev.getMachine(), ev.getSnapshot()));
   }

   @Handler
   private void putSnashotDeletedEvent(SnapshotDeletedEventOut ev) {
      SnapshotOut deletedSnap = snapOutCache.get(ev.getUuid()).get(ev.getSnapshotUuid());
      refreshSnapshot(ev.getMachine().getUuid(), deletedSnap.getParentUuid());
      for (String child : deletedSnap.getChildrenUuid()) {
         refreshSnapshot(ev.getUuid(), child);
      }
      updateMachine(ev.getMachine());
      removeSnapshot(ev.getMachine().getUuid(), ev.getSnapshotUuid());
      EventManager.post(new SnapshotDeletedEvent(ev.getServer(), ev.getMachine(), ev.getSnapshot()));
   }

   @Handler
   private void putSnapshotRestoredEvent(SnapshotRestoredEventOut ev) {
      updateMachine(ev.getMachine());
      updateSnapshot(ev.getMachine().getUuid(), ev.getSnapshot());
      EventManager.post(new SnapshotRestoredEvent(ev.getServer(), ev.getMachine(), ev.getSnapshot()));
   }

   @Handler
   private void putSnashopModifiedEvent(SnapshotModifiedEventOut ev) {
      updateSnapshot(ev.getMachine().getUuid(), ev.getSnapshot());
      EventManager.post(new SnapshotModifiedEvent(ev.getServer(), ev.getMachine(), ev.getSnapshot()));
   }

   @Handler
   private void putMachineDataChangedEvent(MachineDataChangeEventOut ev) {
      updateMachine(ev.getMachine());
      EventManager.post(new MachineDataChangedEvent(reader.getId(), getMachine(ev.getMachine().getUuid())));
   }

   @Handler
   private void putMachineRegistrationEvent(MachineRegistrationEventOut ev) {
      if (ev.isRegistered()) {
         insertMachine(ev.getMachine());
         EventManager.post(new MachineAddedEvent(ev.getServerId(), ev.getMachine()));
      } else {
         deleteMachine(ev.getUuid());
         EventManager.post(new MachineRemovedEvent(ev.getServerId(), ev.getMachine()));
      }
   }

   @Handler
   private void putMachineStateEvent(MachineStateEventOut ev) {
      updateMachine(ev.getMachine());
      EventManager.post(new MachineStateChangedEvent(reader.getId(), getMachine(ev.getMachine().getUuid())));
   }

   @Handler
   private void putHypervisorStateEvent(HypervisorConnectionStateEventOut ev) {
      if (AxStrings.equals(getId(), ev.getServerId())) {
         Logger.verbose("Clearing cache for server " + toString());
         reset();
      }
   }

   private void insertMachine(String vmId) {
      insertMachine(reader.getMachine(vmId));
   }

   private void insertMachine(MachineOut mOut) {
      snapOutCache.remove(mOut.getId());
      mOutCache.put(mOut.getId(), mOut);
      mOutListCache.put(mOut.getId(), mOut);
      invalidMachineUuidSet.remove(mOut.getId());
   }

   private void updateMachine(String vmId) {
      updateMachine(reader.getMachine(vmId));
   }

   private void updateMachine(MachineOut mOut) {
      mOutCache.put(mOut.getId(), mOut);
      mOutListCache.put(mOut.getId(), mOut);
   }

   private void deleteMachine(String vmId) {
      invalidMachineUuidSet.add(vmId);
      mOutCache.remove(vmId);
      mOutListCache.remove(vmId);
      snapOutCache.remove(vmId);
   }

   private void refreshMachine(String vmId) {
      try {
         if (!mOutCache.containsKey(vmId) && !mOutListCache.containsKey(vmId)) {
            insertMachine(vmId);
         } else {
            updateMachine(vmId);
         }
         // TODO catch the proper exception
      } catch (HyperboxException e) {
         // Virtualbox error meaning "Machine not found", so we remove from cache
         if ((e.getMessage() != null) && (e.getMessage().contains("0x80070005") || e.getMessage().contains("0x80BB0001"))) {
            deleteMachine(vmId);
         }
         throw e;
      }
   }

   private void insertMedium(MediumIn medIn) {
      MediumOut medOut = reader.getMedium(medIn);
      medOutCache.put(medOut.getUuid(), medOut);
      medOutCache.put(medOut.getLocation(), medOut);
      invalidMedOutSet.remove(medOut.getUuid());
   }

   private void updateMedium(MediumIn medIn) {
      MediumOut medOut = reader.getMedium(medIn);
      medOutCache.put(medOut.getUuid(), medOut);
      medOutCache.put(medOut.getLocation(), medOut);
   }

   private void deleteMedium(MediumIn medIn) {
      Logger.debug("Removing Medium ID " + medIn.getId() + " from cache");
      MediumOut medOut = getMedium(medIn);
      invalidMedOutSet.add(medOut.getUuid());
      medOutCache.remove(medOut.getUuid());
      medOutCache.remove(medOut.getLocation());
   }

   private void refreshMedium(MediumIn medIn) {
      Logger.debug("Refreshing medium ID " + medIn.getId());
      try {
         if (!medOutCache.containsKey(medIn.getId())) {
            Logger.debug("Unknown medium, fetching data & adding to cache");
            insertMedium(medIn);
         } else {
            Logger.debug("Known medium, updating cache");
            updateMedium(medIn);
         }
      } catch (HyperboxException e) {
         Logger.debug("Cannot fetch information from server, removing from cache if exists");
         deleteMedium(medIn);
      }
   }

   private void updateMachineList() {
      mOutListCache.clear();
      for (MachineOut mOut : reader.listMachines()) {
         mOutListCache.put(mOut.getUuid(), mOut);
      }

      mOutListCacheUpdate = System.currentTimeMillis();
   }

   @Override
   public List<MachineOut> listMachines() {
      if (mOutListCacheUpdate == null) {
         updateMachineList();
      }

      return new ArrayList<MachineOut>(mOutListCache.values());
   }

   @Override
   public MachineOut getMachine(MachineIn mIn) {
      return getMachine(mIn.getUuid());
   }

   @Handler
   public void putTaskStateChangedEvent(TaskStateEventOut ev) {
      updateTask(ev.getTask());
      EventManager.post(new TaskStateChangedEvent(ev.getServer(), ev.getTask()));
   }

   @Handler
   public void putTaskQueueEvent(TaskQueueEventOut ev) {
      if (ev.getQueueEvent().equals(TaskQueueEvents.TaskAdded)) {
         insertTask(ev.getTask());
         EventManager.post(new TaskAddedEvent(ev.getServer(), ev.getTask()));
      }
      if (ev.getQueueEvent().equals(TaskQueueEvents.TaskRemoved)) {
         removeTask(ev.getTask());
         EventManager.post(new TaskRemovedEvent(ev.getServer(), ev.getTask()));
      }
   }

   @Handler
   public void putModuleEventOutput(ModuleEventOut ev) {
      EventManager.post(new ModuleEvent(HyperboxEvents.ModuleLoaded, ev.getServer(), ev.getModule()));
   }

   private void refreshTask(TaskIn tIn) {
      try {
         TaskOut tOut = reader.getTask(tIn);
         insertTask(tOut);
         // TODO catch the proper exception
      } catch (HyperboxException e) {
         invalidTaskIdSet.add(tIn.getId());
         removeTask(tIn.getId());
         throw e;
      }
   }

   private void insertTask(TaskOut tOut) {
      tOutListCache.put(tOut.getId(), tOut);
      tOutCache.put(tOut.getId(), tOut);
   }

   private void updateTask(TaskOut tOut) {
      tOutListCache.put(tOut.getId(), tOut);
      tOutCache.put(tOut.getId(), tOut);
   }

   private void removeTask(String taskId) {
      tOutListCache.remove(taskId);
      tOutCache.remove(taskId);
   }

   private void removeTask(TaskOut tOut) {
      removeTask(tOut.getId());
   }

   private void updateTaskList() {
      tOutListCache.clear();
      for (TaskOut tOut : reader.listTasks()) {
         tOutListCache.put(tOut.getId(), tOut);
      }

      tOutListCacheUpdate = System.currentTimeMillis();
   }

   @Override
   public List<TaskOut> listTasks() {
      if (tOutListCacheUpdate == null) {
         updateTaskList();
      }

      return new ArrayList<TaskOut>(tOutListCache.values());
   }

   @Override
   public TaskOut getTask(TaskIn tIn) {
      if (invalidTaskIdSet.contains(tIn.getId())) {
         throw new HyperboxException("Task was not found");
      }
      if (!tOutCache.containsKey(tIn.getId())) {
         refreshTask(tIn);
      }
      return tOutCache.get(tIn.getId());
   }

   @Override
   public MediumOut getMedium(MediumIn medIn) {
      if (invalidMedOutSet.contains(medIn.getUuid())) {
         throw new HyperboxException(medIn.getUuid() + " does not relate to a medium");
      }
      if (!medOutCache.containsKey(medIn.getUuid()) || !medOutCache.containsKey(medIn.getLocation())) {
         refreshMedium(medIn);
      }

      if (medOutCache.containsKey(medIn.getUuid())) {
         return medOutCache.get(medIn.getUuid());
      } else if (medOutCache.containsKey(medIn.getLocation())) {
         return medOutCache.get(medIn.getLocation());
      } else {
         return null;
      }
   }

   @Override
   public SessionOut getSession(SessionIn sIn) {
      return reader.getSession(sIn);
   }

   @Override
   public StorageControllerSubTypeOut getStorageControllerSubType(StorageControllerSubTypeIn scstIn) {
      return reader.getStorageControllerSubType(scstIn);
   }

   @Override
   public StorageControllerTypeOut getStorageControllerType(StorageControllerTypeIn sctIn) {
      return reader.getStorageControllerType(sctIn);
   }

   @Override
   public StoreOut getStore(StoreIn sIn) {
      return reader.getStore(sIn);
   }

   @Override
   public StoreItemOut getStoreItem(StoreItemIn siIn) {
      return reader.getStoreItem(siIn);
   }

   @Override
   public UserOut getUser(UserIn uIn) {
      return reader.getUser(uIn);
   }

   @Override
   public List<StorageDeviceAttachmentOut> listAttachments(StorageControllerIn scIn) {
      return reader.listAttachments(scIn);
   }

   @Override
   public List<String> listKeyboardMode(MachineIn mIn) {
      return reader.listKeyboardMode(mIn);
   }

   @Override
   public List<MediumOut> listMediums() {
      return reader.listMediums();
   }

   @Override
   public List<String> listMouseMode(MachineIn mIn) {
      return reader.listMouseMode(mIn);
   }

   @Override
   public List<OsTypeOut> listOsType() {
      return reader.listOsType();
   }

   @Override
   public List<OsTypeOut> listOsType(MachineIn mIn) {
      return reader.listOsType(mIn);
   }

   @Override
   public List<SessionOut> listSessions() {
      return reader.listSessions();
   }

   @Override
   public List<StorageControllerSubTypeOut> listStorageControllerSubType(StorageControllerTypeIn sctIn) {
      return reader.listStorageControllerSubType(sctIn);
   }

   @Override
   public List<StorageControllerTypeOut> listStorageControllerType() {
      return reader.listStorageControllerType();
   }

   @Override
   public List<StoreItemOut> listStoreItems(StoreIn sIn) {
      return reader.listStoreItems(sIn);
   }

   @Override
   public List<StoreItemOut> listStoreItems(StoreIn sIn, StoreItemIn siIn) {
      return reader.listStoreItems(sIn, siIn);
   }

   @Override
   public List<StoreOut> listStores() {
      return reader.listStores();
   }

   @Override
   public List<UserOut> listUsers() {
      return reader.listUsers();
   }

   @Override
   public List<NetworkInterfaceOut> listNetworkInterfaces(MachineIn mIn) {
      return reader.listNetworkInterfaces(mIn);
   }

   @Override
   public List<NetworkAttachModeOut> listNetworkAttachModes() {
      return reader.listNetworkAttachModes();
   }

   @Override
   public List<NetworkAttachNameOut> listNetworkAttachNames(NetworkAttachModeIn namIn) {
      return reader.listNetworkAttachNames(namIn);
   }

   @Override
   public List<NetworkInterfaceTypeOut> listNetworkInterfaceTypes() {
      return reader.listNetworkInterfaceTypes();
   }

   @Override
   public _Hypervisor getHypervisor() {
      return reader.getHypervisor();
   }

   @Override
   public ScreenshotOut getScreenshot(MachineIn mIn) {
      return reader.getScreenshot(mIn);
   }

   @Override
   public boolean isHypervisorConnected() {
      return reader.isHypervisorConnected();
   }

   @Override
   public List<HypervisorLoaderOut> listHypervisors() {
      return reader.listHypervisors();
   }

   @Override
   public List<StorageDeviceAttachmentOut> listAttachments(String machineUuid) {
      return reader.listAttachments(machineUuid);
   }

   @Override
   public String getProtocolVersion() {
      return reader.getProtocolVersion();
   }

   @Override
   public _GuestReader getGuest(String machineUuid) {
      return reader.getGuest(machineUuid);
   }

   @Override
   public MachineOut getMachine(String vmId) {
      if (invalidMachineUuidSet.contains(vmId)) {
         throw new HyperboxException(vmId + " does not relate to a machine");
      }
      if (!mOutCache.containsKey(vmId)) {
         refreshMachine(vmId);
      }
      return mOutCache.get(vmId);
   }

   @Override
   public List<PermissionOut> listPermissions(UserIn usrIn) {
      return reader.listPermissions(usrIn);
   }

   @Override
   public HostOut getHost() {
      return reader.getHost();
   }

   @Override
   public SnapshotOut getSnapshot(String vmId, String snapId) {
      return getSnapshot(new MachineIn(vmId), new SnapshotIn(snapId));
   }

   @Override
   public SnapshotOut getCurrentSnapshot(String vmId) {
      return getCurrentSnapshot(new MachineIn(vmId));
   }

   @Override
   public List<ModuleOut> listModules() {
      return reader.listModules();
   }

   @Override
   public ModuleOut getModule(String modId) {
      return reader.getModule(modId);
   }

   @Override
   public Set<String> listLogLevel() {
      return reader.listLogLevel();
   }

   @Override
   public String getLogLevel() {
      return reader.getLogLevel();
   }

   @Override
   public _Machine getMachineReader(String id) {
      return reader.getMachineReader(id);
   }

   @Override
   public _Task getTask(String id) {
      return reader.getTask(id);
   }

}