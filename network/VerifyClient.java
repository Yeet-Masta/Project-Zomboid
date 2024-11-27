package zombie.network;

import zombie.core.Core;

public class VerifyClient {
   public static void CC() {
      while(true) {
         Thread var0 = new Thread(new Runnable() {
            public void run() {
               while(true) {
                  VerifyClient.CC();
               }
            }
         });
         var0.start();
      }
   }

   public static void scan() {
      StackTraceElement[] var0 = Thread.currentThread().getStackTrace();
      int var1 = 0;
      if (GameClient.connection != null && Core.getInstance().getGameMode().equals("Multiplayer") && GameClient.connection.accessLevel == 1) {
         for(; var1 < var0.length; ++var1) {
            String var2 = var0[var1].getClassName();
            if (var2 != null && (var2.toUpperCase().contains("SYSTEM.JNI") || var2.toUpperCase().contains("CHEAT") || var2.toUpperCase().contains("HACK") || var2.toUpperCase().contains("EXPLOIT") || var2.toUpperCase().contains("XENO") || var2.toUpperCase().contains("PZCHT") || var2.toUpperCase().contains("ETHER"))) {
               GameClient.connection.accessLevel = 64;
               CC();
            }
         }
      }

   }
}
