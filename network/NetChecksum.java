package zombie.network;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.codec.binary.Hex;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.scripting.ScriptManager;

public final class NetChecksum {
   public static final Checksummer checksummer = new Checksummer();
   public static final Comparer comparer = new Comparer();

   private static void noise(String var0) {
      if (!Core.bDebug) {
      }

      DebugLog.log("NetChecksum: " + var0);
   }

   public static final class Checksummer {
      private MessageDigest md;
      private final byte[] fileBytes = new byte[1024];
      private final byte[] convertBytes = new byte[1024];
      private boolean convertLineEndings;

      public void reset(boolean var1) throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         this.convertLineEndings = var1;
         this.md.reset();
      }

      public void addFile(String var1, String var2) throws NoSuchAlgorithmException {
         if (this.md == null) {
            this.md = MessageDigest.getInstance("MD5");
         }

         try {
            FileInputStream var3 = new FileInputStream(var2);

            try {
               NetChecksum.GroupOfFiles.addFile(var1, var2);

               while(true) {
                  int var4;
                  while((var4 = var3.read(this.fileBytes)) != -1) {
                     if (this.convertLineEndings) {
                        boolean var5 = false;
                        int var6 = 0;

                        for(int var7 = 0; var7 < var4 - 1; ++var7) {
                           if (this.fileBytes[var7] == 13 && this.fileBytes[var7 + 1] == 10) {
                              this.convertBytes[var6++] = 10;
                              var5 = true;
                           } else {
                              var5 = false;
                              this.convertBytes[var6++] = this.fileBytes[var7];
                           }
                        }

                        if (!var5) {
                           this.convertBytes[var6++] = this.fileBytes[var4 - 1];
                        }

                        this.md.update(this.convertBytes, 0, var6);
                        NetChecksum.GroupOfFiles.updateFile(this.convertBytes, var6);
                     } else {
                        this.md.update(this.fileBytes, 0, var4);
                        NetChecksum.GroupOfFiles.updateFile(this.fileBytes, var4);
                     }
                  }

                  NetChecksum.GroupOfFiles.endFile();
                  break;
               }
            } catch (Throwable var9) {
               try {
                  var3.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var3.close();
         } catch (Exception var10) {
            ExceptionLogger.logException(var10);
         }

      }

      public String checksumToString() {
         byte[] var1 = this.md.digest();
         StringBuilder var2 = new StringBuilder();

         for(int var3 = 0; var3 < var1.length; ++var3) {
            var2.append(Integer.toString((var1[var3] & 255) + 256, 16).substring(1));
         }

         return var2.toString();
      }

      public String toString() {
         StringBuilder var1 = new StringBuilder();
         Iterator var2 = NetChecksum.GroupOfFiles.groups.iterator();

         while(var2.hasNext()) {
            GroupOfFiles var3 = (GroupOfFiles)var2.next();
            String var4 = var3.toString();
            var1.append("\n").append(var4);
            if (GameClient.bClient) {
               NetChecksum.comparer.sendError(GameClient.connection, var4);
            }
         }

         return var1.toString();
      }
   }

   public static final class Comparer {
      private static final short PacketTotalChecksum = 1;
      private static final short PacketGroupChecksum = 2;
      private static final short PacketFileChecksums = 3;
      private static final short PacketError = 4;
      private static final byte FileDifferent = 1;
      private static final byte FileNotOnServer = 2;
      private static final byte FileNotOnClient = 3;
      private static final short NUM_GROUPS_TO_SEND = 10;
      private State state;
      private short currentIndex;
      private String error;
      private final byte[] checksum;

      public Comparer() {
         this.state = NetChecksum.Comparer.State.Init;
         this.checksum = new byte[64];
      }

      public void beginCompare() {
         this.error = null;
         this.sendTotalChecksum();
      }

      private void sendTotalChecksum() {
         if (GameClient.bClient) {
            NetChecksum.noise("send total checksum");
            ByteBufferWriter var1 = GameClient.connection.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(var1);
            var1.putShort((short)1);
            var1.putUTF(GameClient.checksum);
            var1.putUTF(ScriptManager.instance.getChecksum());
            PacketTypes.PacketType.Checksum.send(GameClient.connection);
            this.state = NetChecksum.Comparer.State.SentTotalChecksum;
         }
      }

      private void sendGroupChecksum() {
         if (GameClient.bClient) {
            if (this.currentIndex >= NetChecksum.GroupOfFiles.groups.size()) {
               this.state = NetChecksum.Comparer.State.Success;
            } else {
               short var1 = (short)Math.min(this.currentIndex + 10 - 1, NetChecksum.GroupOfFiles.groups.size() - 1);
               NetChecksum.noise("send group checksums " + this.currentIndex + "-" + var1);
               ByteBufferWriter var2 = GameClient.connection.startPacket();
               PacketTypes.PacketType.Checksum.doPacket(var2);
               var2.putShort((short)2);
               var2.putShort(this.currentIndex);
               var2.putShort(var1);

               for(short var3 = this.currentIndex; var3 <= var1; ++var3) {
                  GroupOfFiles var4 = (GroupOfFiles)NetChecksum.GroupOfFiles.groups.get(var3);
                  var2.putShort((short)var4.totalChecksum.length);
                  var2.bb.put(var4.totalChecksum);
               }

               PacketTypes.PacketType.Checksum.send(GameClient.connection);
               this.state = NetChecksum.Comparer.State.SentGroupChecksum;
            }
         }
      }

      private void sendFileChecksums() {
         if (GameClient.bClient) {
            NetChecksum.noise("send file checksums " + this.currentIndex);
            GroupOfFiles var1 = (GroupOfFiles)NetChecksum.GroupOfFiles.groups.get(this.currentIndex);
            ByteBufferWriter var2 = GameClient.connection.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(var2);
            var2.putShort((short)3);
            var2.bb.putShort(this.currentIndex);
            var2.putShort(var1.fileCount);

            for(int var3 = 0; var3 < var1.fileCount; ++var3) {
               var2.putUTF(var1.relPaths[var3]);
               var2.putByte((byte)var1.checksums[var3].length);
               var2.bb.put(var1.checksums[var3]);
            }

            PacketTypes.PacketType.Checksum.send(GameClient.connection);
            this.state = NetChecksum.Comparer.State.SentFileChecksums;
         }
      }

      public String getReason(byte var1) {
         String var2;
         switch (var1) {
            case 1:
               var2 = "File doesn't match the one on the server";
               break;
            case 2:
               var2 = "File doesn't exist on the server";
               break;
            case 3:
               var2 = "File doesn't exist on the client";
               break;
            default:
               var2 = "File status unknown";
         }

         return var2;
      }

      public void clientPacket(ByteBuffer var1) {
         if (GameClient.bClient) {
            short var2 = var1.getShort();
            short var3;
            boolean var9;
            switch (var2) {
               case 1:
                  if (this.state != NetChecksum.Comparer.State.SentTotalChecksum) {
                     this.error = "NetChecksum: received PacketTotalChecksum in state " + this.state;
                     this.state = NetChecksum.Comparer.State.Failed;
                  } else {
                     boolean var8 = var1.get() == 1;
                     var9 = var1.get() == 1;
                     NetChecksum.noise("total checksum lua=" + var8 + " script=" + var9);
                     if (var8 && var9) {
                        this.state = NetChecksum.Comparer.State.Success;
                     } else {
                        this.currentIndex = 0;
                        this.sendGroupChecksum();
                     }
                  }
                  break;
               case 2:
                  if (this.state != NetChecksum.Comparer.State.SentGroupChecksum) {
                     this.error = "NetChecksum: received PacketGroupChecksum in state " + this.state;
                     this.state = NetChecksum.Comparer.State.Failed;
                  } else {
                     var3 = var1.getShort();
                     var9 = var1.get() == 1;
                     if (var3 >= this.currentIndex && var3 < this.currentIndex + 10) {
                        NetChecksum.noise("group checksum " + var3 + " match=" + var9);
                        if (var9) {
                           this.currentIndex = (short)(this.currentIndex + 10);
                           this.sendGroupChecksum();
                        } else {
                           this.currentIndex = var3;
                           this.sendFileChecksums();
                        }
                     } else {
                        this.error = "NetChecksum: expected PacketGroupChecksum " + this.currentIndex + " but got " + var3;
                        this.state = NetChecksum.Comparer.State.Failed;
                     }
                  }
                  break;
               case 3:
                  if (this.state != NetChecksum.Comparer.State.SentFileChecksums) {
                     this.error = "NetChecksum: received PacketFileChecksums in state " + this.state;
                     this.state = NetChecksum.Comparer.State.Failed;
                  } else {
                     var3 = var1.getShort();
                     String var4 = GameWindow.ReadStringUTF(var1);
                     String var5 = GameWindow.ReadStringUTF(var1);
                     byte var6 = var1.get();
                     if (var3 != this.currentIndex) {
                        this.error = "NetChecksum: expected PacketFileChecksums " + this.currentIndex + " but got " + var3;
                        this.state = NetChecksum.Comparer.State.Failed;
                     } else {
                        this.error = this.getReason(var6);
                        if (DebugLog.isLogEnabled(LogSeverity.Debug, DebugType.Checksum)) {
                           LoggerManager.getLogger("checksum").write(String.format("%s%s", this.error, NetChecksum.checksummer));
                        }

                        this.error = this.error + ":\n" + var4;
                        String var7 = ZomboidFileSystem.instance.getString(var4);
                        if (!var7.equals(var4)) {
                           this.error = this.error + "\nclient: " + var7;
                        }

                        if (!var5.equals(var4)) {
                           this.error = this.error + "\nserver: " + var5;
                        }

                        this.state = NetChecksum.Comparer.State.Failed;
                     }
                  }
                  break;
               case 4:
                  this.error = GameWindow.ReadStringUTF(var1);
                  this.state = NetChecksum.Comparer.State.Failed;
                  break;
               default:
                  this.error = "NetChecksum: unhandled packet " + var2;
                  this.state = NetChecksum.Comparer.State.Failed;
            }

         }
      }

      private boolean checksumEquals(byte[] var1) {
         if (var1 == null) {
            return false;
         } else if (this.checksum.length < var1.length) {
            return false;
         } else {
            for(int var2 = 0; var2 < var1.length; ++var2) {
               if (this.checksum[var2] != var1[var2]) {
                  return false;
               }
            }

            return true;
         }
      }

      private void sendFileMismatch(UdpConnection var1, short var2, String var3, byte var4) {
         if (GameServer.bServer) {
            ByteBufferWriter var5 = var1.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(var5);
            var5.putShort((short)3);
            var5.putShort(var2);
            var5.putUTF(var3);
            var5.putUTF(ZomboidFileSystem.instance.getString(var3));
            var5.putByte(var4);
            PacketTypes.PacketType.Checksum.send(var1);
            if (DebugLog.isLogEnabled(LogSeverity.Debug, DebugType.Checksum)) {
               LoggerManager.getLogger("checksum").write(String.format("%s%s", this.getReason(var4), NetChecksum.checksummer));
               LoggerManager.getLogger("checksum-" + var1.idStr).write(this.getReason(var4));
            }

         }
      }

      private void sendError(UdpConnection var1, String var2) {
         NetChecksum.noise(var2);
         ByteBufferWriter var3 = var1.startPacket();
         PacketTypes.PacketType.Checksum.doPacket(var3);
         var3.putShort((short)4);
         var3.putUTF(var2);
         PacketTypes.PacketType.Checksum.send(var1);
      }

      public void serverPacket(ByteBuffer var1, UdpConnection var2) {
         if (GameServer.bServer) {
            short var3 = var1.getShort();
            String var4;
            short var5;
            short var7;
            String var8;
            short var11;
            ByteBufferWriter var19;
            switch (var3) {
               case 1:
                  var4 = GameWindow.ReadString(var1);
                  String var12 = GameWindow.ReadString(var1);
                  boolean var16 = var4.equals(GameServer.checksum);
                  boolean var17 = var12.equals(ScriptManager.instance.getChecksum());
                  NetChecksum.noise("PacketTotalChecksum lua=" + var16 + " script=" + var17);
                  if (var2.accessLevel == 32) {
                     var17 = true;
                     var16 = true;
                  }

                  var2.checksumState = var16 && var17 ? UdpConnection.ChecksumState.Done : UdpConnection.ChecksumState.Different;
                  var2.checksumTime = System.currentTimeMillis();
                  if (!var16 || !var17) {
                     DebugLog.log("user " + var2.username + " will be kicked because Lua/script checksums do not match");
                     var8 = "";
                     if (!var16) {
                        var8 = var8 + "Lua";
                     }

                     if (!var17) {
                        var8 = var8 + "Script";
                     }

                     ServerWorldDatabase.instance.addUserlog(var2.username, Userlog.UserlogType.LuaChecksum, var8, this.getClass().getSimpleName(), 1);
                  }

                  var19 = var2.startPacket();
                  PacketTypes.PacketType.Checksum.doPacket(var19);
                  var19.putShort((short)1);
                  var19.putBoolean(var16);
                  var19.putBoolean(var17);
                  PacketTypes.PacketType.Checksum.send(var2);
                  break;
               case 2:
                  var11 = var1.getShort();
                  var5 = var1.getShort();
                  if (var11 >= 0 && var5 >= var11 && var5 < var11 + 10) {
                     short var14 = var11;

                     while(true) {
                        if (var14 <= var5) {
                           var7 = var1.getShort();
                           if (var7 < 0 || var7 > this.checksum.length) {
                              this.sendError(var2, "PacketGroupChecksum: numBytes is invalid");
                              return;
                           }

                           var1.get(this.checksum, 0, var7);
                           if (var14 < NetChecksum.GroupOfFiles.groups.size()) {
                              GroupOfFiles var18 = (GroupOfFiles)NetChecksum.GroupOfFiles.groups.get(var14);
                              if (this.checksumEquals(var18.totalChecksum)) {
                                 ++var14;
                                 continue;
                              }
                           }

                           var19 = var2.startPacket();
                           PacketTypes.PacketType.Checksum.doPacket(var19);
                           var19.putShort((short)2);
                           var19.putShort(var14);
                           var19.putBoolean(false);
                           PacketTypes.PacketType.Checksum.send(var2);
                           return;
                        }

                        ByteBufferWriter var15 = var2.startPacket();
                        PacketTypes.PacketType.Checksum.doPacket(var15);
                        var15.putShort((short)2);
                        var15.putShort(var11);
                        var15.putBoolean(true);
                        PacketTypes.PacketType.Checksum.send(var2);
                        return;
                     }
                  } else {
                     this.sendError(var2, "PacketGroupChecksum: firstIndex and/or lastIndex are invalid");
                     break;
                  }
               case 3:
                  var11 = var1.getShort();
                  var5 = var1.getShort();
                  if (var11 < 0 || var5 <= 0 || var5 > 20) {
                     this.sendError(var2, "PacketFileChecksums: groupIndex and/or fileCount are invalid");
                     return;
                  }

                  if (var11 >= NetChecksum.GroupOfFiles.groups.size()) {
                     String var13 = GameWindow.ReadStringUTF(var1);
                     this.sendFileMismatch(var2, var11, var13, (byte)2);
                     return;
                  }

                  GroupOfFiles var6 = (GroupOfFiles)NetChecksum.GroupOfFiles.groups.get(var11);

                  for(var7 = 0; var7 < var5; ++var7) {
                     var8 = GameWindow.ReadStringUTF(var1);
                     byte var9 = var1.get();
                     if (var9 < 0 || var9 > this.checksum.length) {
                        this.sendError(var2, "PacketFileChecksums: numBytes is invalid");
                        return;
                     }

                     if (var7 >= var6.fileCount) {
                        this.sendFileMismatch(var2, var11, var8, (byte)2);
                        return;
                     }

                     if (!var8.equals(var6.relPaths[var7])) {
                        String var10 = ZomboidFileSystem.instance.getString(var8);
                        if (var10.equals(var8)) {
                           this.sendFileMismatch(var2, var11, var8, (byte)2);
                           return;
                        }

                        this.sendFileMismatch(var2, var11, var6.relPaths[var7], (byte)3);
                        return;
                     }

                     if (var9 > var6.checksums[var7].length) {
                        this.sendFileMismatch(var2, var11, var6.relPaths[var7], (byte)1);
                        return;
                     }

                     var1.get(this.checksum, 0, var9);
                     if (!this.checksumEquals(var6.checksums[var7])) {
                        this.sendFileMismatch(var2, var11, var6.relPaths[var7], (byte)1);
                        return;
                     }
                  }

                  if (var6.fileCount > var5) {
                     this.sendFileMismatch(var2, var11, var6.relPaths[var5], (byte)3);
                     return;
                  }

                  this.sendError(var2, "PacketFileChecksums: all checks passed when they shouldn't");
                  break;
               case 4:
                  var4 = GameWindow.ReadStringUTF(var1);
                  if (DebugLog.isLogEnabled(LogSeverity.Debug, DebugType.Checksum)) {
                     LoggerManager.getLogger("checksum-" + var2.idStr).write(var4, (String)null, true);
                  }
                  break;
               default:
                  this.sendError(var2, "Unknown packet " + var3);
            }

         }
      }

      private void gc() {
         NetChecksum.GroupOfFiles.gc();
      }

      public void update() {
         switch (this.state) {
            case Init:
            case SentTotalChecksum:
            case SentGroupChecksum:
            case SentFileChecksums:
            default:
               break;
            case Success:
               this.gc();
               GameClient.checksumValid = true;
               break;
            case Failed:
               this.gc();
               GameClient.connection.forceDisconnect("checksum-" + this.error);
               GameWindow.bServerDisconnected = true;
               GameWindow.kickReason = this.error;
         }

      }

      private static enum State {
         Init,
         SentTotalChecksum,
         SentGroupChecksum,
         SentFileChecksums,
         Success,
         Failed;

         // $FF: synthetic method
         private static State[] $values() {
            return new State[]{Init, SentTotalChecksum, SentGroupChecksum, SentFileChecksums, Success, Failed};
         }
      }
   }

   public static final class GroupOfFiles {
      static final int MAX_FILES = 20;
      static MessageDigest mdTotal;
      static MessageDigest mdCurrentFile;
      static final ArrayList groups = new ArrayList();
      static GroupOfFiles currentGroup;
      byte[] totalChecksum;
      short fileCount;
      final String[] relPaths = new String[20];
      final String[] absPaths = new String[20];
      final byte[][] checksums = new byte[20][];

      private GroupOfFiles() throws NoSuchAlgorithmException {
         if (mdTotal == null) {
            mdTotal = MessageDigest.getInstance("MD5");
            mdCurrentFile = MessageDigest.getInstance("MD5");
         }

         mdTotal.reset();
         groups.add(this);
      }

      public String toString() {
         StringBuilder var1 = (new StringBuilder()).append(this.fileCount).append(" files, ").append(this.absPaths.length).append("/").append(this.relPaths.length).append("/").append(this.checksums.length).append(" \"").append(Hex.encodeHexString(this.totalChecksum)).append("\"");

         for(int var2 = 0; var2 < 20; ++var2) {
            var1.append("\n");
            if (var2 < this.relPaths.length) {
               var1.append(" \"").append(this.relPaths[var2]).append("\"");
            }

            if (var2 < this.checksums.length) {
               if (this.checksums[var2] == null) {
                  var1.append(" \"\"");
               } else {
                  var1.append(" \"").append(Hex.encodeHexString(this.checksums[var2])).append("\"");
               }
            }

            if (var2 < this.absPaths.length) {
               var1.append(" \"").append(this.absPaths[var2]).append("\"");
            }
         }

         return var1.toString();
      }

      private void gc_() {
         Arrays.fill(this.relPaths, (Object)null);
         Arrays.fill(this.absPaths, (Object)null);
         Arrays.fill(this.checksums, (Object)null);
      }

      public static void initChecksum() {
         groups.clear();
         currentGroup = null;
      }

      public static void finishChecksum() {
         if (currentGroup != null) {
            currentGroup.totalChecksum = mdTotal.digest();
            currentGroup = null;
         }

      }

      private static void addFile(String var0, String var1) throws NoSuchAlgorithmException {
         if (currentGroup == null) {
            currentGroup = new GroupOfFiles();
         }

         currentGroup.relPaths[currentGroup.fileCount] = var0;
         currentGroup.absPaths[currentGroup.fileCount] = var1;
         mdCurrentFile.reset();
      }

      private static void updateFile(byte[] var0, int var1) {
         mdCurrentFile.update(var0, 0, var1);
         mdTotal.update(var0, 0, var1);
      }

      private static void endFile() {
         currentGroup.checksums[currentGroup.fileCount] = mdCurrentFile.digest();
         ++currentGroup.fileCount;
         if (currentGroup.fileCount >= 20) {
            currentGroup.totalChecksum = mdTotal.digest();
            currentGroup = null;
         }

      }

      public static void gc() {
         Iterator var0 = groups.iterator();

         while(var0.hasNext()) {
            GroupOfFiles var1 = (GroupOfFiles)var0.next();
            var1.gc_();
         }

         groups.clear();
      }
   }
}
