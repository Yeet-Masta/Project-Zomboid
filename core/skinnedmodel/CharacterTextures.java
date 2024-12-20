package zombie.core.skinnedmodel;

import java.util.ArrayList;
import zombie.iso.IsoDirections;

public final class CharacterTextures {
   final ArrayList m_animSets = new ArrayList();

   CTAnimSet getAnimSet(String var1) {
      for(int var2 = 0; var2 < this.m_animSets.size(); ++var2) {
         CTAnimSet var3 = (CTAnimSet)this.m_animSets.get(var2);
         if (var3.m_name.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   DeadBodyAtlas.BodyTexture getTexture(String var1, String var2, IsoDirections var3, int var4) {
      CTAnimSet var5 = this.getAnimSet(var1);
      if (var5 == null) {
         return null;
      } else {
         CTState var6 = var5.getState(var2);
         if (var6 == null) {
            return null;
         } else {
            CTEntry var7 = var6.getEntry(var3, var4);
            return var7 == null ? null : var7.m_texture;
         }
      }
   }

   void addTexture(String var1, String var2, IsoDirections var3, int var4, DeadBodyAtlas.BodyTexture var5) {
      CTAnimSet var6 = this.getAnimSet(var1);
      if (var6 == null) {
         var6 = new CTAnimSet();
         var6.m_name = var1;
         this.m_animSets.add(var6);
      }

      var6.addEntry(var2, var3, var4, var5);
   }

   void clear() {
      this.m_animSets.clear();
   }

   private static final class CTAnimSet {
      String m_name;
      final ArrayList m_states = new ArrayList();

      CTState getState(String var1) {
         for(int var2 = 0; var2 < this.m_states.size(); ++var2) {
            CTState var3 = (CTState)this.m_states.get(var2);
            if (var3.m_name.equals(var1)) {
               return var3;
            }
         }

         return null;
      }

      void addEntry(String var1, IsoDirections var2, int var3, DeadBodyAtlas.BodyTexture var4) {
         CTState var5 = this.getState(var1);
         if (var5 == null) {
            var5 = new CTState();
            var5.m_name = var1;
            this.m_states.add(var5);
         }

         var5.addEntry(var2, var3, var4);
      }
   }

   private static final class CTState {
      String m_name;
      final CTEntryList[] m_entries = new CTEntryList[IsoDirections.values().length];

      CTState() {
         for(int var1 = 0; var1 < this.m_entries.length; ++var1) {
            this.m_entries[var1] = new CTEntryList();
         }

      }

      CTEntry getEntry(IsoDirections var1, int var2) {
         CTEntryList var3 = this.m_entries[var1.index()];

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            CTEntry var5 = (CTEntry)var3.get(var4);
            if (var5.m_frame == var2) {
               return var5;
            }
         }

         return null;
      }

      void addEntry(IsoDirections var1, int var2, DeadBodyAtlas.BodyTexture var3) {
         CTEntryList var4 = this.m_entries[var1.index()];
         CTEntry var5 = new CTEntry();
         var5.m_frame = var2;
         var5.m_texture = var3;
         var4.add(var5);
      }
   }

   private static final class CTEntry {
      int m_frame;
      DeadBodyAtlas.BodyTexture m_texture;
   }

   private static final class CTEntryList extends ArrayList {
   }
}
