package zombie.radio.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Rand;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.world.WorldDictionary;

public class RecordedMedia {
   public static boolean DISABLE_LINE_LEARNING = false;
   private static final int SPAWN_COMMON = 0;
   private static final int SPAWN_RARE = 1;
   private static final int SPAWN_EXCEPTIONAL = 2;
   public static final int VERSION1 = 1;
   public static final int VERSION2 = 2;
   public static final int VERSION = 2;
   public static final String SAVE_FILE = "recorded_media.bin";
   private final ArrayList indexes = new ArrayList();
   private static final ArrayList indexesFromServer = new ArrayList();
   private final Map mediaDataMap = new HashMap();
   private final Map categorizedMap = new HashMap();
   private final ArrayList categories = new ArrayList();
   private final ArrayList legacyListenedLines = new ArrayList();
   private final HashSet homeVhsSpawned = new HashSet();
   private final Map retailVhsSpawnTable = new HashMap();
   private final Map retailCdSpawnTable = new HashMap();
   private boolean REQUIRES_SAVING = true;

   public void init() {
      try {
         this.load();
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      LuaEventManager.triggerEvent("OnInitRecordedMedia", this);
      this.retailCdSpawnTable.put(0, new ArrayList());
      this.retailCdSpawnTable.put(1, new ArrayList());
      this.retailCdSpawnTable.put(2, new ArrayList());
      this.retailVhsSpawnTable.put(0, new ArrayList());
      this.retailVhsSpawnTable.put(1, new ArrayList());
      this.retailVhsSpawnTable.put(2, new ArrayList());
      ArrayList var1 = (ArrayList)this.categorizedMap.get("CDs");
      Iterator var2;
      MediaData var3;
      if (var1 != null) {
         var2 = var1.iterator();

         while(var2.hasNext()) {
            var3 = (MediaData)var2.next();
            if (var3.getSpawning() == 1) {
               ((ArrayList)this.retailCdSpawnTable.get(1)).add(var3);
            } else if (var3.getSpawning() == 2) {
               ((ArrayList)this.retailCdSpawnTable.get(2)).add(var3);
            } else {
               ((ArrayList)this.retailCdSpawnTable.get(0)).add(var3);
            }
         }
      } else {
         DebugLog.General.error("categorizedMap with CDs is empty");
      }

      var1 = (ArrayList)this.categorizedMap.get("Retail-VHS");
      if (var1 != null) {
         var2 = var1.iterator();

         while(var2.hasNext()) {
            var3 = (MediaData)var2.next();
            if (var3.getSpawning() == 1) {
               ((ArrayList)this.retailVhsSpawnTable.get(1)).add(var3);
            } else if (var3.getSpawning() == 2) {
               ((ArrayList)this.retailVhsSpawnTable.get(2)).add(var3);
            } else {
               ((ArrayList)this.retailVhsSpawnTable.get(0)).add(var3);
            }
         }
      } else {
         DebugLog.General.error("categorizedMap with Retail-VHS is empty");
      }

      try {
         this.save();
      } catch (Exception var4) {
         var4.printStackTrace();
      }

   }

   public static byte getMediaTypeForCategory(String var0) {
      if (var0 == null) {
         return -1;
      } else {
         return (byte)(var0.equalsIgnoreCase("cds") ? 0 : 1);
      }
   }

   public ArrayList getCategories() {
      return this.categories;
   }

   public ArrayList getAllMediaForType(byte var1) {
      ArrayList var2 = new ArrayList();
      Iterator var3 = this.mediaDataMap.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         if (((MediaData)var4.getValue()).getMediaType() == var1) {
            var2.add((MediaData)var4.getValue());
         }
      }

      var2.sort(new MediaNameSorter());
      return var2;
   }

   public ArrayList getAllMediaForCategory(String var1) {
      ArrayList var2 = new ArrayList();
      Iterator var3 = this.mediaDataMap.entrySet().iterator();

      while(var3.hasNext()) {
         Map.Entry var4 = (Map.Entry)var3.next();
         if (var1.equalsIgnoreCase(((MediaData)var4.getValue()).getCategory())) {
            var2.add((MediaData)var4.getValue());
         }
      }

      var2.sort(new MediaNameSorter());
      return var2;
   }

   public MediaData register(String var1, String var2, String var3, int var4) {
      if (this.mediaDataMap.containsKey(var2)) {
         DebugLog.log("RecordeMedia -> MediaData id already exists : " + var2);
         return null;
      } else {
         if (var4 < 0) {
            var4 = 0;
         }

         MediaData var5 = new MediaData(var2, var3, var4);
         this.mediaDataMap.put(var2, var5);
         var5.setCategory(var1);
         if (!this.categorizedMap.containsKey(var1)) {
            this.categorizedMap.put(var1, new ArrayList());
            this.categories.add(var1);
         }

         ((ArrayList)this.categorizedMap.get(var1)).add(var5);
         short var6;
         if (this.indexes.contains(var2)) {
            var6 = (short)this.indexes.indexOf(var2);
         } else {
            var6 = (short)this.indexes.size();
            this.indexes.add(var2);
         }

         var5.setIndex(var6);
         this.REQUIRES_SAVING = true;
         return var5;
      }
   }

   public MediaData getMediaDataFromIndex(short var1) {
      return var1 >= 0 && var1 < this.indexes.size() ? this.getMediaData((String)this.indexes.get(var1)) : null;
   }

   public short getIndexForMediaData(MediaData var1) {
      return (short)this.indexes.indexOf(var1.getId());
   }

   public MediaData getMediaData(String var1) {
      return (MediaData)this.mediaDataMap.get(var1);
   }

   public MediaData getRandomFromCategory(String var1) {
      if (this.categorizedMap.containsKey(var1)) {
         MediaData var2 = null;
         int var3;
         if (var1.equalsIgnoreCase("cds")) {
            var3 = Rand.Next(0, 1000);
            if (var3 < 100) {
               if (((ArrayList)this.retailCdSpawnTable.get(2)).size() > 0) {
                  var2 = (MediaData)((ArrayList)this.retailCdSpawnTable.get(2)).get(Rand.Next(0, ((ArrayList)this.retailCdSpawnTable.get(2)).size()));
               }
            } else if (var3 < 400) {
               if (((ArrayList)this.retailCdSpawnTable.get(1)).size() > 0) {
                  var2 = (MediaData)((ArrayList)this.retailCdSpawnTable.get(1)).get(Rand.Next(0, ((ArrayList)this.retailCdSpawnTable.get(1)).size()));
               }
            } else {
               var2 = (MediaData)((ArrayList)this.retailCdSpawnTable.get(0)).get(Rand.Next(0, ((ArrayList)this.retailCdSpawnTable.get(0)).size()));
            }

            if (var2 != null) {
               return var2;
            }

            return (MediaData)((ArrayList)this.retailCdSpawnTable.get(0)).get(Rand.Next(0, ((ArrayList)this.retailCdSpawnTable.get(0)).size()));
         }

         if (var1.equalsIgnoreCase("retail-vhs")) {
            var3 = Rand.Next(0, 1000);
            if (var3 < 100) {
               if (((ArrayList)this.retailVhsSpawnTable.get(2)).size() > 0) {
                  var2 = (MediaData)((ArrayList)this.retailVhsSpawnTable.get(2)).get(Rand.Next(0, ((ArrayList)this.retailVhsSpawnTable.get(2)).size()));
               }
            } else if (var3 < 400) {
               if (((ArrayList)this.retailVhsSpawnTable.get(1)).size() > 0) {
                  var2 = (MediaData)((ArrayList)this.retailVhsSpawnTable.get(1)).get(Rand.Next(0, ((ArrayList)this.retailVhsSpawnTable.get(1)).size()));
               }
            } else {
               var2 = (MediaData)((ArrayList)this.retailVhsSpawnTable.get(0)).get(Rand.Next(0, ((ArrayList)this.retailVhsSpawnTable.get(0)).size()));
            }

            if (var2 != null) {
               return var2;
            }

            return (MediaData)((ArrayList)this.retailVhsSpawnTable.get(0)).get(Rand.Next(0, ((ArrayList)this.retailVhsSpawnTable.get(0)).size()));
         }

         if (var1.equalsIgnoreCase("home-vhs")) {
            var3 = Rand.Next(0, 1000);
            if (var3 < 200) {
               ArrayList var4 = (ArrayList)this.categorizedMap.get("Home-VHS");
               var2 = (MediaData)var4.get(Rand.Next(0, var4.size()));
               if (!this.homeVhsSpawned.contains(var2.getIndex())) {
                  this.homeVhsSpawned.add(var2.getIndex());
                  this.REQUIRES_SAVING = true;
                  return var2;
               }
            }
         }
      }

      return null;
   }

   public void load() throws IOException {
      this.indexes.clear();
      if (GameClient.bClient) {
         this.indexes.addAll(indexesFromServer);
         indexesFromServer.clear();
      }

      if (!Core.getInstance().isNoSave()) {
         String var1 = ZomboidFileSystem.instance.getFileNameInCurrentSave("recorded_media.bin");
         File var2 = new File(var1);
         if (!var2.exists()) {
            if (!WorldDictionary.isIsNewGame()) {
               DebugLog.log("RecordedMedia data file is missing from world folder.");
            }

         } else {
            try {
               FileInputStream var3 = new FileInputStream(var2);

               try {
                  DebugLog.log("Loading Recorded Media:" + var1);
                  ByteBuffer var4 = ByteBuffer.allocate((int)var2.length());
                  var4.clear();
                  int var5 = var3.read(var4.array());
                  var4.limit(var5);
                  int var6 = var4.getInt();
                  int var7 = var4.getInt();

                  int var8;
                  String var9;
                  for(var8 = 0; var8 < var7; ++var8) {
                     var9 = GameWindow.ReadString(var4);
                     if (!GameClient.bClient) {
                        this.indexes.add(var9);
                     }
                  }

                  if (var6 == 1) {
                     var7 = var4.getInt();

                     for(var8 = 0; var8 < var7; ++var8) {
                        var9 = GameWindow.ReadString(var4);
                        this.legacyListenedLines.add(var9);
                     }
                  }

                  var7 = var4.getInt();

                  for(var8 = 0; var8 < var7; ++var8) {
                     this.homeVhsSpawned.add(var4.getShort());
                  }
               } catch (Throwable var11) {
                  try {
                     var3.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var3.close();
            } catch (Exception var12) {
               var12.printStackTrace();
            }

         }
      }
   }

   public void save() throws IOException {
      if (!Core.getInstance().isNoSave() && this.REQUIRES_SAVING) {
         try {
            int var1 = 0;
            var1 += this.indexes.size() * 40;
            var1 += this.homeVhsSpawned.size() * 2;
            var1 += 512;
            byte[] var2 = new byte[var1];
            ByteBuffer var3 = ByteBuffer.wrap(var2);
            var3.putInt(2);
            var3.putInt(this.indexes.size());

            for(int var4 = 0; var4 < this.indexes.size(); ++var4) {
               GameWindow.WriteString(var3, (String)this.indexes.get(var4));
            }

            var3.putInt(this.homeVhsSpawned.size());
            Short[] var9 = (Short[])this.homeVhsSpawned.toArray(new Short[0]);

            for(int var5 = 0; var5 < var9.length; ++var5) {
               var3.putShort(var9[var5]);
            }

            var3.flip();
            String var10 = ZomboidFileSystem.instance.getFileNameInCurrentSave("recorded_media.bin");
            File var6 = new File(var10);
            DebugLog.log("Saving Recorded Media:" + var10);
            FileOutputStream var7 = new FileOutputStream(var6);
            var7.getChannel().truncate(0L);
            var7.write(var3.array(), 0, var3.limit());
            var7.flush();
            var7.close();
         } catch (Exception var8) {
            var8.printStackTrace();
         }

         this.REQUIRES_SAVING = false;
      }
   }

   public static String toAscii(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());
      var0 = Normalizer.normalize(var0, Form.NFD);
      char[] var2 = var0.toCharArray();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         char var5 = var2[var4];
         if (var5 <= 127) {
            var1.append(var5);
         }
      }

      return var1.toString();
   }

   public boolean hasListenedToLine(IsoPlayer var1, String var2) {
      return var1.isKnownMediaLine(var2);
   }

   public boolean hasListenedToAll(IsoPlayer var1, MediaData var2) {
      if (var1 == null) {
         var1 = IsoPlayer.players[0];
      }

      if (var1 != null && var2 != null) {
         for(int var3 = 0; var3 < var2.getLineCount(); ++var3) {
            MediaData.MediaLineData var4 = var2.getLine(var3);
            if (!var1.isKnownMediaLine(var4.getTextGuid())) {
               return false;
            }
         }

         return var2.getLineCount() > 0;
      } else {
         return false;
      }
   }

   public void sendRequestData(ByteBuffer var1) {
      var1.putInt(this.indexes.size());

      for(int var2 = 0; var2 < this.indexes.size(); ++var2) {
         GameWindow.WriteStringUTF(var1, (String)this.indexes.get(var2));
      }

   }

   public static void receiveRequestData(ByteBuffer var0) {
      indexesFromServer.clear();
      int var1 = var0.getInt();

      for(int var2 = 0; var2 < var1; ++var2) {
         indexesFromServer.add(GameWindow.ReadStringUTF(var0));
      }

   }

   public void handleLegacyListenedLines(IsoPlayer var1) {
      if (!this.legacyListenedLines.isEmpty()) {
         if (var1 != null) {
            Iterator var2 = this.legacyListenedLines.iterator();

            while(var2.hasNext()) {
               String var3 = (String)var2.next();
               var1.addKnownMediaLine(var3);
            }
         }

         this.legacyListenedLines.clear();
      }
   }

   public static class MediaNameSorter implements Comparator {
      public int compare(MediaData var1, MediaData var2) {
         return var1.getTranslatedItemDisplayName().compareToIgnoreCase(var2.getTranslatedItemDisplayName());
      }
   }
}
