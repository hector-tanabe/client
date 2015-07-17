/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2014 Maxime Dor
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

package io.kamax.hboxc.gui.action.storage;

import io.kamax.hbox.comm.Command;
import io.kamax.hbox.comm.HypervisorTasks;
import io.kamax.hbox.comm.Request;
import io.kamax.hbox.comm.in.MachineIn;
import io.kamax.hbox.comm.in.MediumIn;
import io.kamax.hbox.comm.in.ServerIn;
import io.kamax.hbox.comm.in.StorageDeviceAttachmentIn;
import io.kamax.hbox.comm.out.storage.MediumOut;
import io.kamax.hbox.comm.out.storage.StorageDeviceAttachmentOut;
import io.kamax.hboxc.HyperboxClient;
import io.kamax.hboxc.gui.Gui;
import io.kamax.hboxc.gui.builder.IconBuilder;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

@SuppressWarnings("serial")
public class HypervisorToolsMediumAttachAction extends AbstractAction {

   private String serverId;
   private StorageDeviceAttachmentOut sdaOut;

   public HypervisorToolsMediumAttachAction(String serverId, StorageDeviceAttachmentOut sdaOut) {
      this(serverId, sdaOut, "Attach Hypervisor Tools", IconBuilder.getTask(HypervisorTasks.MediumMount), true);
   }

   public HypervisorToolsMediumAttachAction(String serverId, StorageDeviceAttachmentOut sdaOut, String label, ImageIcon icon, boolean isEnabled) {
      super(label, icon);
      setEnabled(isEnabled);
      this.serverId = serverId;
      this.sdaOut = sdaOut;
   }

   @Override
   public void actionPerformed(ActionEvent ae) {
      MediumOut medOut = Gui.getReader().getServerReader(serverId).getHypervisor().getToolsMedium();
      if (medOut != null) {
         Request req = new Request(Command.VBOX, HypervisorTasks.MediumMount);
         req.set(new ServerIn(serverId));
         req.set(new MachineIn(sdaOut.getMachineUuid()));
         req.set(new StorageDeviceAttachmentIn(sdaOut.getControllerName(), sdaOut.getPortId(), sdaOut.getDeviceId(), sdaOut.getDeviceType()));
         req.set(new MediumIn(medOut.getLocation(), medOut.getDeviceType()));
         Gui.post(req);
      } else {
         HyperboxClient.getView().postError("Cannot attach - No Hypervisor Tools available");
      }
   }

}