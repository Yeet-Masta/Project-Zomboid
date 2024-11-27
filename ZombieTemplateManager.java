package zombie;

import java.util.ArrayList;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.textures.Texture;

public class ZombieTemplateManager {
   public Texture addOverlayToTexture(ArrayList var1, Texture var2) {
      return null;
   }

   public class ZombieTemplate {
      public Texture tex;
   }

   public class BodyOverlay {
      public BodyPartType location;
      public OverlayType type;
   }

   public static enum OverlayType {
      BloodLight,
      BloodMedium,
      BloodHeavy;

      // $FF: synthetic method
      private static OverlayType[] $values() {
         return new OverlayType[]{BloodLight, BloodMedium, BloodHeavy};
      }
   }
}
