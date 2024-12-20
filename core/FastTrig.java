package zombie.core;

public class FastTrig {
   public static double cos(double var0) {
      return sin(var0 + 1.5707963267948966);
   }

   public static double sin(double var0) {
      var0 = reduceSinAngle(var0);
      return Math.abs(var0) <= 0.7853981633974483 ? Math.sin(var0) : Math.cos(1.5707963267948966 - var0);
   }

   private static double reduceSinAngle(double var0) {
      var0 %= 6.283185307179586;
      if (Math.abs(var0) > Math.PI) {
         var0 -= 6.283185307179586;
      }

      if (Math.abs(var0) > 1.5707963267948966) {
         var0 = Math.PI - var0;
      }

      return var0;
   }
}
