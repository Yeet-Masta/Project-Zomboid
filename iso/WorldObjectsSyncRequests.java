package zombie.iso;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PacketTypes;

public final class WorldObjectsSyncRequests {
   public final ArrayList requests = new ArrayList();
   public long timeout = 1000L;

   public void putRequest(IsoChunk var1) {
      SyncData var2 = new SyncData();
      var2.x = var1.wx;
      var2.y = var1.wy;
      var2.hashCodeWorldObjects = var1.getHashCodeObjects();
      var2.reqTime = 0L;
      var2.reqCount = 0;
      synchronized(this.requests) {
         this.requests.add(var2);
      }
   }

   public void sendRequests(UdpConnection var1) {
      if (this.requests.size() != 0) {
         ByteBufferWriter var2 = var1.startPacket();
         PacketTypes.PacketType.SyncWorldObjectsReq.doPacket(var2);
         ByteBuffer var3 = var2.bb;
         int var4 = var3.position();
         var2.putShort((short)0);
         int var5 = 0;
         synchronized(this.requests) {
            for(int var7 = 0; var7 < this.requests.size(); ++var7) {
               SyncData var8 = (SyncData)this.requests.get(var7);
               if (var8.reqCount > 2) {
                  this.requests.remove(var7);
                  --var7;
               } else {
                  if (var8.reqTime == 0L) {
                     var8.reqTime = System.currentTimeMillis();
                     ++var5;
                     var3.putInt(var8.x);
                     var3.putInt(var8.y);
                     var3.putLong(var8.hashCodeWorldObjects);
                     ++var8.reqCount;
                  }

                  if (System.currentTimeMillis() - var8.reqTime >= this.timeout) {
                     var8.reqTime = System.currentTimeMillis();
                     ++var5;
                     var3.putInt(var8.x);
                     var3.putInt(var8.y);
                     var3.putLong(var8.hashCodeWorldObjects);
                     ++var8.reqCount;
                  }

                  if (var5 >= 50) {
                     break;
                  }
               }
            }
         }

         if (var5 == 0) {
            GameClient.connection.cancelPacket();
         } else {
            int var6 = var3.position();
            var3.position(var4);
            var3.putShort((short)var5);
            var3.position(var6);
            PacketTypes.PacketType.SyncWorldObjectsReq.send(GameClient.connection);
         }
      }
   }

   public void receiveIsoSync(int var1, int var2) {
      synchronized(this.requests) {
         for(int var4 = 0; var4 < this.requests.size(); ++var4) {
            SyncData var5 = (SyncData)this.requests.get(var4);
            if (var5.x == var1 && var5.y == var2) {
               this.requests.remove(var4);
            }
         }

      }
   }

   private class SyncData {
      int x;
      int y;
      long hashCodeWorldObjects;
      long reqTime;
      int reqCount;
   }
}
