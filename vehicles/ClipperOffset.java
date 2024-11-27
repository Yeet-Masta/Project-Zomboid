package zombie.vehicles;

import java.nio.ByteBuffer;

public final class ClipperOffset {
   private final long address = this.newInstance();

   private native long newInstance();

   public native void clear();

   public native void addPath(int var1, ByteBuffer var2, int var3, int var4);

   public native void execute(double var1);

   public native int getPolygonCount();

   public native int getPolygon(int var1, ByteBuffer var2);

   public static enum EndType {
      etClosedPolygon,
      etClosedLine,
      etOpenButt,
      etOpenSquare,
      etOpenRound;

      // $FF: synthetic method
      private static EndType[] $values() {
         return new EndType[]{etClosedPolygon, etClosedLine, etOpenButt, etOpenSquare, etOpenRound};
      }
   }

   public static enum JoinType {
      jtSquare,
      jtRound,
      jtMiter;

      // $FF: synthetic method
      private static JoinType[] $values() {
         return new JoinType[]{jtSquare, jtRound, jtMiter};
      }
   }
}
