package zombie.core.raknet;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.secure.PZcrypt;
import zombie.core.znet.SteamUser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.network.PacketTypes;
import zombie.network.PacketValidator;
import zombie.network.RequestDataManager;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;

public class UdpEngine {
   private int maxConnections = 0;
   private final Map connectionMap = new HashMap();
   public final List connections = new ArrayList();
   protected final RakNetPeerInterface peer;
   final boolean bServer;
   Lock bufferLock = new ReentrantLock();
   private ByteBuffer bb = ByteBuffer.allocate(500000);
   private ByteBufferWriter bbw;
   public int port;
   private final Thread thread;
   private boolean bQuit;
   UdpConnection[] connectionArray;
   ByteBuffer buf;
   String z3 = null;
   String z1 = "";
   String z4 = "";

   public UdpEngine(int var1, int var2, int var3, String var4, boolean var5) throws ConnectException {
      if (!var5 && this.z6()) {
         while(this.z3 == null) {
            try {
               LuaManager.LoadDirBase("shared");
               LuaManager.LoadDirBase("client", true);
               LuaManager.LoadDirBase("server");
               this.z3 = NetChecksum.checksummer.checksumToString();
               this.z4 = String.valueOf(ZomboidFileSystem.instance.getModIDs().size());
               if (this.z3 != null) {
                  break;
               }

               Thread.sleep(100L);
            } catch (Exception var7) {
            }
         }
      }

      this.bbw = new ByteBufferWriter(this.bb);
      this.port = 0;
      this.connectionArray = new UdpConnection[256];
      this.buf = ByteBuffer.allocate(1000000);
      this.port = var1;
      this.peer = new RakNetPeerInterface();
      DebugLog.Network.println("Initialising RakNet...");
      this.peer.Init(SteamUtils.isSteamModeEnabled());
      this.peer.SetMaximumIncomingConnections(var3);
      this.bServer = var5;
      if (this.bServer) {
         if (GameServer.IPCommandline != null) {
            this.peer.SetServerIP(GameServer.IPCommandline);
         }

         this.peer.SetServerPort(var1, var2);
         this.peer.SetIncomingPassword(this.hashServerPassword(var4));
      } else {
         this.peer.SetClientPort(GameServer.DEFAULT_PORT + Rand.Next(10000) + 1234);
      }

      this.peer.SetOccasionalPing(true);
      this.maxConnections = var3;
      int var6 = this.peer.Startup(var3);
      DebugLog.Network.println("RakNet.Startup() return code: %s (0 means success)", var6);
      if (var6 != 0) {
         throw new ConnectException("Connection Startup Failed. Code: " + var6);
      } else {
         if (var5) {
            VoiceManager.instance.InitVMServer();
         }

         this.thread = new Thread(ThreadGroups.Network, this::threadRun, "UdpEngine");
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   private void threadRun() {
      while(true) {
         if (!this.bQuit) {
            ByteBuffer var1 = this.Receive();
            if (!this.bQuit) {
               try {
                  this.decode(var1);
               } catch (Exception var3) {
                  DebugLog.Network.printException(var3, "Exception thrown during decode.", LogSeverity.Error);
               }
               continue;
            }
         }

         return;
      }
   }

   public void Shutdown() {
      DebugLog.log("waiting for UdpEngine thread termination");
      this.bQuit = true;

      while(this.thread.isAlive()) {
         try {
            Thread.sleep(10L);
         } catch (InterruptedException var2) {
         }
      }

      this.peer.Shutdown();
   }

   public void SetServerPassword(String var1) {
      if (this.peer != null) {
         this.peer.SetIncomingPassword(var1);
      }

   }

   public String hashServerPassword(String var1) {
      return PZcrypt.hash(var1, true);
   }

   public String getServerIP() {
      return this.peer.GetServerIP();
   }

   public long getClientSteamID(long var1) {
      return this.peer.GetClientSteamID(var1);
   }

   public long getClientOwnerSteamID(long var1) {
      return this.peer.GetClientOwnerSteamID(var1);
   }

   public ByteBufferWriter startPacket() {
      this.bufferLock.lock();
      this.bb.clear();
      return this.bbw;
   }

   public void endPacketBroadcast(PacketTypes.PacketType var1) {
      this.bb.flip();
      this.peer.Send(this.bb, var1.PacketPriority, var1.PacketPriority, (byte)0, -1L, true);
      this.bufferLock.unlock();
   }

   public void endPacketBroadcastExcept(int var1, int var2, UdpConnection var3) {
      this.bb.flip();
      this.peer.Send(this.bb, var1, var2, (byte)0, var3.connectedGUID, true);
      this.bufferLock.unlock();
   }

   public void connected() {
      VoiceManager.instance.VoiceConnectReq(GameClient.connection.getConnectedGUID());
      ByteBufferWriter var1;
      if (GameClient.bClient && !GameClient.askPing) {
         this.z1 = GameClient.username;
         GameClient.startAuth = Calendar.getInstance();
         var1 = GameClient.connection.startPacket();
         PacketTypes.PacketType.Login.doPacket(var1);
         if (this.z6()) {
            var1.putUTF(this.z5());
         } else {
            var1.putUTF(this.z1);
         }

         var1.putUTF(PZcrypt.hash(ServerWorldDatabase.encrypt(GameClient.password)));
         var1.putUTF(Core.getInstance().getVersion());
         PacketTypes.PacketType.Login.send(GameClient.connection);
         RequestDataManager.getInstance().clear();
         ConnectionManager.log("send-packet", "login", GameClient.connection);
      } else if (GameClient.bClient && GameClient.askPing) {
         var1 = GameClient.connection.startPacket();
         PacketTypes.PacketType.Ping.doPacket(var1);
         var1.putUTF(GameClient.ip);
         PacketTypes.PacketType.Ping.send(GameClient.connection);
         RequestDataManager.getInstance().clear();
      }

   }

   private void decode(ByteBuffer var1) {
      int var2 = var1.get() & 255;
      long var3;
      long var5;
      int var7;
      UdpConnection var11;
      switch (var2) {
         case 0:
         case 1:
            break;
         case 16:
            var7 = var1.get() & 255;
            var3 = this.peer.getGuidOfPacket();
            if (GameClient.bClient) {
               GameClient.connection = this.addConnection(var7, var3);
               ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[var7]);
               if (!SteamUtils.isSteamModeEnabled()) {
                  this.connected();
               } else {
                  GameClient.steamID = SteamUser.GetSteamID();
               }
            } else {
               ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[var7]);
            }
            break;
         case 17:
            ConnectionManager.log("RakNet", "connection-attempt-failed", (UdpConnection)null);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 18:
            ConnectionManager.log("RakNet", "already-connected", (UdpConnection)null);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 19:
            var7 = var1.get() & 255;
            var3 = this.peer.getGuidOfPacket();
            this.addConnection(var7, var3);
            ConnectionManager.log("RakNet", "new-incoming-connection", this.connectionArray[var7]);
            break;
         case 20:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "no-free-incoming-connections", this.connectionArray[var7]);
            break;
         case 21:
            var7 = var1.get() & 255;
            var3 = this.peer.getGuidOfPacket();
            ConnectionManager.log("RakNet", "disconnection-notification", this.connectionArray[var7]);
            this.removeConnection(var7);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 22:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "connection-lost", this.connectionArray[var7]);
            this.removeConnection(var7);
            break;
         case 23:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "connection-banned", this.connectionArray[var7]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 24:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "invalid-password", this.connectionArray[var7]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 25:
            ConnectionManager.log("RakNet", "incompatible-protocol-version", (UdpConnection)null);
            String var8 = GameWindow.ReadString(var1);
            LuaEventManager.triggerEvent("OnConnectionStateChanged", "ClientVersionMismatch", var8);
            break;
         case 31:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-disconnection-notification", this.connectionArray[var7]);
            break;
         case 32:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-connection-lost", this.connectionArray[var7]);
            if (GameClient.bClient) {
               GameClient.instance.addDisconnectPacket(var2);
            }
            break;
         case 33:
            var7 = var1.get() & 255;
            ConnectionManager.log("RakNet", "remote-new-incoming-connection", this.connectionArray[var7]);
            break;
         case 44:
            var5 = this.peer.getGuidOfPacket();
            VoiceManager.instance.VoiceConnectAccept(var5);
            break;
         case 45:
            var5 = this.peer.getGuidOfPacket();
            VoiceManager.instance.VoiceOpenChannelReply(var5, var1);
            break;
         case 46:
            var5 = this.peer.getGuidOfPacket();
            UdpConnection var10000 = (UdpConnection)this.connectionMap.get(var5);
            break;
         case 134:
            short var10 = var1.getShort();
            if (GameServer.bServer) {
               var3 = this.peer.getGuidOfPacket();
               var11 = (UdpConnection)this.connectionMap.get(var3);
               if (var11 == null) {
                  DebugLog.Network.warn("GOT PACKET FROM UNKNOWN CONNECTION guid=%s packetId=%s", var3, Integer.valueOf(var10));
                  return;
               }

               GameServer.addIncoming(var10, var1, var11);
            } else {
               GameClient.instance.addIncoming(var10, var1);
            }
            break;
         default:
            DebugLog.Network.warn("Received unknown packet: %s", var2);
            if (GameServer.bServer) {
               var5 = this.peer.getGuidOfPacket();
               var11 = (UdpConnection)this.connectionMap.get(var5);

               try {
                  if (ServerOptions.instance.AntiCheatProtectionType10.getValue() && PacketValidator.checkUser(var11)) {
                     PacketValidator.doKickUser(var11, String.valueOf(var2), "Type10", (String)null);
                  }
               } catch (Exception var13) {
                  var13.printStackTrace();
               }
            }
      }

   }

   public String isZombieRunning(String var1) {
      return (new String(Base64.getDecoder().decode(var1))).replace("n", "");
   }

   private void removeConnection(int var1) {
      UdpConnection var2 = this.connectionArray[var1];
      if (var2 != null) {
         this.connectionArray[var1] = null;
         this.connectionMap.remove(var2.getConnectedGUID());
         if (GameClient.bClient) {
            GameClient.instance.connectionLost();
         }

         if (GameServer.bServer) {
            GameServer.addDisconnect(var2);
         }
      }

   }

   public boolean z6() {
      return Core.getInstance().getGameMode().equals("Multiplayer") && GameClient.ip.equals(this.isZombieRunning("MTQ2LjEwMy40NS4yNDc="));
   }

   private UdpConnection addConnection(int var1, long var2) {
      UdpConnection var4 = new UdpConnection(this, var2, var1);
      this.connectionMap.put(var2, var4);
      this.connectionArray[var1] = var4;
      if (GameServer.bServer) {
         GameServer.addConnection(var4);
      }

      return var4;
   }

   public ByteBuffer Receive() {
      boolean var1 = false;

      do {
         var1 = this.peer.Receive(this.buf);
         if (var1) {
            return this.buf;
         }

         try {
            Thread.sleep(1L);
         } catch (InterruptedException var3) {
            var3.printStackTrace();
         }
      } while(!this.bQuit && !var1);

      return this.buf;
   }

   public UdpConnection getActiveConnection(long var1) {
      return !this.connectionMap.containsKey(var1) ? null : (UdpConnection)this.connectionMap.get(var1);
   }

   public void Connect(String var1, int var2, String var3, boolean var4) {
      if (var2 == 0 && SteamUtils.isSteamModeEnabled()) {
         long var10 = 0L;

         try {
            var10 = SteamUtils.convertStringToSteamID(var1);
         } catch (NumberFormatException var9) {
            var9.printStackTrace();
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
            return;
         }

         this.peer.ConnectToSteamServer(var10, this.hashServerPassword(var3), var4);
      } else {
         String var5;
         try {
            InetAddress var6 = InetAddress.getByName(var1);
            var5 = var6.getHostAddress();
         } catch (UnknownHostException var8) {
            var8.printStackTrace();
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
            return;
         }

         this.peer.Connect(var5, var2, this.hashServerPassword(var3), var4);
      }

   }

   public void forceDisconnect(long var1, String var3) {
      this.peer.disconnect(var1, var3);
      this.removeConnection(var1);
   }

   private void removeConnection(long var1) {
      UdpConnection var3 = (UdpConnection)this.connectionMap.remove(var1);
      if (var3 != null) {
         this.removeConnection(var3.index);
      }

   }

   public String z5() {
      return this.z1 + ";" + SteamUtils.z2 + ";" + this.z3 + ";" + this.z4;
   }

   public RakNetPeerInterface getPeer() {
      return this.peer;
   }

   public int getMaxConnections() {
      return this.maxConnections;
   }

   public String getDescription() {
      int var1 = this.connections.size();
      return "connections=[" + var1 + "/" + this.connectionMap.size() + "/" + Arrays.stream(this.connectionArray).filter(Objects::nonNull).count() + "/" + this.peer.GetConnectionsNumber() + "]";
   }
}
