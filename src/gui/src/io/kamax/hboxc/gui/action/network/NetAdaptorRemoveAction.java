/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2015 Maxime Dor
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

package io.kamax.hboxc.gui.action.network;

import io.kamax.hbox.comm.HypervisorTasks;
import io.kamax.hboxc.gui.Gui;
import io.kamax.hboxc.gui.builder.IconBuilder;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

@SuppressWarnings("serial")
public class NetAdaptorRemoveAction extends AbstractAction {

   private String srvId;
   private String modeId;
   private String adaptorId;

   public NetAdaptorRemoveAction(String srvId, String modeId, String adaptorId) {
      super("Remove", IconBuilder.getTask(HypervisorTasks.NetAdaptorRemove));
      this.srvId = srvId;
      this.modeId = modeId;
      this.adaptorId = adaptorId;
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      Gui.getServer(srvId).getHypervisor().removeAdaptor(modeId, adaptorId);
   }

}