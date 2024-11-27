package zombie.ai.astar;

public class AStarPathFinder {
   public static enum PathFindProgress {
      notrunning,
      failed,
      found,
      notyetfound;

      // $FF: synthetic method
      private static PathFindProgress[] $values() {
         return new PathFindProgress[]{notrunning, failed, found, notyetfound};
      }
   }
}
