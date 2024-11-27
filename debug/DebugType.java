package zombie.debug;

public enum DebugType {
   Packet,
   NetworkFileDebug,
   Network,
   General,
   Lua,
   Mod,
   Sound,
   Zombie,
   Combat,
   Objects,
   Fireplace,
   Radio,
   MapLoading,
   Clothing,
   Animation,
   Asset,
   Script,
   Shader,
   Input,
   Recipe,
   ActionSystem,
   IsoRegion,
   UnitTests,
   FileIO,
   Multiplayer,
   Ownership,
   Death,
   Damage,
   Statistic,
   Vehicle,
   Voice,
   Checksum;

   public static boolean Do(DebugType var0) {
      return DebugLog.isEnabled(var0);
   }

   // $FF: synthetic method
   private static DebugType[] $values() {
      return new DebugType[]{Packet, NetworkFileDebug, Network, General, Lua, Mod, Sound, Zombie, Combat, Objects, Fireplace, Radio, MapLoading, Clothing, Animation, Asset, Script, Shader, Input, Recipe, ActionSystem, IsoRegion, UnitTests, FileIO, Multiplayer, Ownership, Death, Damage, Statistic, Vehicle, Voice, Checksum};
   }
}
